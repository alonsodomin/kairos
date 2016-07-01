/*
 * Copyright 2016 Antonio Alonso Dominguez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quckoo.cluster.scheduler

import java.util.UUID

import akka.actor._
import akka.cluster.client.ClusterClientReceptionist
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.pattern._
import akka.persistence.query.scaladsl.{AllPersistenceIdsQuery, CurrentPersistenceIdsQuery, EventsByPersistenceIdQuery}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, OverflowStrategy}
import akka.stream.scaladsl.Source

import io.quckoo.{ExecutionPlan, JobSpec}
import io.quckoo.cluster.protocol._
import io.quckoo.cluster.topics
import io.quckoo.id._
import io.quckoo.protocol.registry._
import io.quckoo.protocol.scheduler._
import io.quckoo.time.TimeSource

import scala.concurrent._
import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 16/08/15.
 */
object Scheduler {

  type Journal = AllPersistenceIdsQuery with CurrentPersistenceIdsQuery with EventsByPersistenceIdQuery

  private[scheduler] case class CreateExecutionDriver(spec: JobSpec, config: ScheduleJob, requestor: ActorRef)

  def props(registry: ActorRef, journal: Journal, queueProps: Props)(implicit timeSource: TimeSource) =
    Props(classOf[Scheduler], registry, journal, queueProps, timeSource)

}

class Scheduler(registry: ActorRef, journal: Scheduler.Journal, queueProps: Props)(implicit timeSource: TimeSource)
    extends Actor with ActorLogging {

  import Scheduler._

  ClusterClientReceptionist(context.system).registerService(self)

  final implicit val materializer = ActorMaterializer(
    ActorMaterializerSettings(context.system), "registry"
  )

  private[this] val monitor = context.actorOf(TaskQueueMonitor.props, "monitor")
  private[this] val taskQueue = context.actorOf(queueProps, "queue")
  private[this] val shardRegion = ClusterSharding(context.system).start(
    ExecutionDriver.ShardName,
    entityProps     = ExecutionDriver.props,
    settings        = ClusterShardingSettings(context.system).withRememberEntities(true),
    extractEntityId = ExecutionDriver.idExtractor,
    extractShardId  = ExecutionDriver.shardResolver
  )
  private[this] val executionPlanIndex = context.actorOf(
    ExecutionPlanIndex.props(shardRegion), "executionPlanIndex"
  )
  private[this] val executionIndex = context.actorOf(
    ExecutionIndex.props(journal), "executionIndex"
  )

  override def preStart(): Unit = {
    journal.currentPersistenceIds().
      filter(_.startsWith(ExecutionDriver.ShardName)).
      flatMapConcat(persistenceId => journal.eventsByPersistenceId(persistenceId, 0, Long.MaxValue)).
      runForeach { envelope => executionPlanIndex ! envelope.event }
  }

  override def receive: Receive = {
    case cmd: ScheduleJob =>
      val handler = context.actorOf(jobFetcherProps(cmd.jobId, sender(), cmd))
      registry.tell(GetJob(cmd.jobId), handler)

    case cmd @ CreateExecutionDriver(_, config, _) =>
      val planId = UUID.randomUUID()
      val props = factoryProps(config.jobId, planId, cmd, shardRegion)
      log.debug("Found enabled job {}. Initializing a new execution plan for it.", config.jobId)
      context.actorOf(props, s"execution-driver-factory-$planId")

    case cancel: CancelPlan =>
      shardRegion forward cancel

    case get: GetExecutionPlan =>
      executionPlanIndex forward get

    case GetExecutionPlans =>
      import context.dispatcher
      queryExecutionPlans pipeTo sender()

    case GetTasks =>
      executionIndex forward GetTasks

    case msg: WorkerMessage =>
      taskQueue forward msg
  }

  def queryExecutionPlans: Future[Map[PlanId, ExecutionPlan]] = {
    Source.actorRef[ExecutionPlan](100, OverflowStrategy.fail).
      mapMaterializedValue { upstream =>
        executionPlanIndex.tell(GetExecutionPlans, upstream)
      }.runFold(Map.empty[PlanId, ExecutionPlan]) {
        (map, plan) => map + (plan.planId -> plan)
      }
  }

  private[this] def jobFetcherProps(jobId: JobId, requestor: ActorRef, config: ScheduleJob): Props =
    Props(classOf[JobFetcher], jobId, requestor, config)

  private[this] def factoryProps(jobId: JobId, planId: PlanId, createCmd: CreateExecutionDriver,
      shardRegion: ActorRef): Props =
    Props(classOf[ExecutionDriverFactory], jobId, planId, createCmd, shardRegion)

}

private class JobFetcher(jobId: JobId, requestor: ActorRef, config: ScheduleJob)
    extends Actor with ActorLogging {

  import Scheduler._

  context.setReceiveTimeout(3 seconds)

  def receive: Receive = {
    case (`jobId`, spec: JobSpec) =>
      if (!spec.disabled) {
        // create execution plan
        context.parent ! CreateExecutionDriver(spec, config, requestor)
      } else {
        log.info("Found job {} in the registry but is not enabled.", jobId)
        requestor ! JobNotEnabled(jobId)
      }
      context stop self

    case JobNotFound(`jobId`) =>
      log.info("No enabled job with id {} could be retrieved.", jobId)
      requestor ! JobNotFound(jobId)
      context stop self

    case ReceiveTimeout =>
      log.error("Timed out while fetching job {} from the registry.", jobId)
      requestor ! JobNotFound(jobId)
      context stop self
  }

  override def unhandled(message: Any): Unit = {
    log.warning("Unexpected message {} received when fetching job {}.", message, jobId)
  }

}

private class ExecutionDriverFactory(
    jobId: JobId,
    planId: PlanId,
    cmd: Scheduler.CreateExecutionDriver,
    shardRegion: ActorRef)
  extends Actor with ActorLogging {

  private[this] val mediator = DistributedPubSub(context.system).mediator

  override def preStart(): Unit =
    mediator ! DistributedPubSubMediator.Subscribe(topics.Scheduler, self)

  def receive: Receive = initializing

  def initializing: Receive = {
    case DistributedPubSubMediator.SubscribeAck(_) =>
      import cmd._

      log.debug("Starting execution plan for job {}.", jobId)
      val executionProps = Execution.props(
        planId, executionTimeout = cmd.config.timeout
      )
      shardRegion ! ExecutionDriver.New(jobId, spec, planId, config.trigger, executionProps)

    case response @ ExecutionPlanStarted(`jobId`, _) =>
      log.info("Execution plan for job {} has been started.", jobId)
      cmd.requestor ! response
      mediator ! DistributedPubSubMediator.Unsubscribe(topics.Scheduler, self)
      context.become(shuttingDown)
  }

  def shuttingDown: Receive = {
    case DistributedPubSubMediator.UnsubscribeAck(_) =>
      context.stop(self)
  }

}
