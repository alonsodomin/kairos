package io.kairos.cluster.scheduler

import java.util.UUID

import akka.actor._
import akka.cluster.client.ClusterClientReceptionist
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}

import io.kairos.JobSpec
import io.kairos.cluster.core.KairosJournal
import io.kairos.cluster.protocol.WorkerProtocol
import io.kairos.id._
import io.kairos.protocol.{RegistryProtocol, SchedulerProtocol}
import io.kairos.time.TimeSource

/**
 * Created by aalonsodominguez on 16/08/15.
 */
object Scheduler {
  import SchedulerProtocol._

  private[scheduler] case class CreateExecutionDriver(spec: JobSpec, config: ScheduleJob, requestor: ActorRef)

  def props(registry: ActorRef, queueProps: Props)(implicit timeSource: TimeSource) =
    Props(classOf[Scheduler], registry, queueProps, timeSource)

}

class Scheduler(registry: ActorRef, queueProps: Props)(implicit timeSource: TimeSource)
    extends Actor with ActorLogging with KairosJournal {

  import RegistryProtocol._
  import Scheduler._
  import SchedulerProtocol._
  import WorkerProtocol._

  ClusterClientReceptionist(context.system).registerService(self)

  final implicit val materializer = ActorMaterializer(ActorMaterializerSettings(context.system))

  private[this] val taskQueue = context.actorOf(queueProps, "queue")
  private[this] val shardRegion = ClusterSharding(context.system).start(
    ExecutionDriver.ShardName,
    entityProps     = ExecutionDriver.props,
    settings        = ClusterShardingSettings(context.system),
    extractEntityId = ExecutionDriver.idExtractor,
    extractShardId  = ExecutionDriver.shardResolver
  )
  private[this] val executionPlanView = context.actorOf(
    Props(classOf[ExecutionPlanView], shardRegion), "plans"
  )

  override implicit def actorSystem: ActorSystem = context.system

  override def preStart(): Unit = {
    readJournal.allPersistenceIds().
      filter(_.startsWith("ExecutionPlan-")).
      flatMapConcat { persistenceId =>
        readJournal.eventsByPersistenceId(persistenceId, 0, System.currentTimeMillis())
      } runForeach { env =>
        executionPlanView ! env.event
      }
  }

  override def postStop(): Unit =
    context.stop(executionPlanView)

  override def receive: Receive = {
    case cmd: ScheduleJob =>
      val handler = context.actorOf(jobFetcherProps(cmd.jobId, sender(), cmd), "handler")
      registry.tell(GetJob(cmd.jobId), handler)

    case cmd @ CreateExecutionDriver(_, config, _) =>
      log.debug("Found enabled job {}. Initializing a new execution plan for it.", config.jobId)
      val props = factoryProps(config.jobId, cmd, shardRegion)
      context.actorOf(props, s"execution-plan-factory-${config.jobId}")

    case get: GetExecutionPlan =>
      executionPlanView.tell(get, sender())

    case GetExecutionPlans =>
      executionPlanView.tell(GetExecutionPlans, sender())

    case msg: WorkerMessage =>
      taskQueue.tell(msg, sender())
  }

  private[this] def jobFetcherProps(jobId: JobId, requestor: ActorRef, config: ScheduleJob): Props =
    Props(classOf[JobFetcher], jobId, requestor, config)

  private[this] def factoryProps(jobId: JobId, createCmd: CreateExecutionDriver,
                                 shardRegion: ActorRef): Props =
    Props(classOf[ExecutionDriverFactory], jobId, createCmd, shardRegion)

}

private class JobFetcher(jobId: JobId, requestor: ActorRef, config: SchedulerProtocol.ScheduleJob)
    extends Actor with ActorLogging {

  import Scheduler._
  import SchedulerProtocol._

  def receive: Receive = {
    case Some(spec: JobSpec) =>
      if (!spec.disabled) {
        // create execution plan
        context.parent ! CreateExecutionDriver(spec, config, requestor)
      } else {
        log.info("Found job {} in the registry but is not enabled.", jobId)
        requestor ! JobNotEnabled(jobId)
      }
      context.stop(self)

    case None =>
      log.info("No enabled job with id {} could be retrieved.", jobId)
      requestor ! JobNotFound(jobId)
      context.stop(self)
  }

}

private class ExecutionDriverFactory(jobId: JobId, cmd: Scheduler.CreateExecutionDriver,
                                     shardRegion: ActorRef)
    extends Actor with ActorLogging {

  import SchedulerProtocol._

  private[this] val mediator = DistributedPubSub(context.system).mediator

  override def preStart(): Unit =
    mediator ! DistributedPubSubMediator.Subscribe(SchedulerTopic, self)

  def receive: Receive = initializing

  def initializing: Receive = {
    case DistributedPubSubMediator.SubscribeAck(_) =>
      import cmd._

      val planId = UUID.randomUUID()
      log.debug("Starting execution plan for job {}.", config.jobId)
      val executionProps = Execution.props(
        planId, executionTimeout = cmd.config.timeout
      )
      shardRegion ! ExecutionDriver.New(config.jobId, spec, planId, config.trigger, executionProps)

    case response @ ExecutionPlanStarted(`jobId`, _) =>
      log.info("Execution plan for job {} has been started.", jobId)
      cmd.requestor ! response
      mediator ! DistributedPubSubMediator.Unsubscribe(SchedulerTopic, self)
      context.become(shuttingDown)
  }

  def shuttingDown: Receive = {
    case DistributedPubSubMediator.UnsubscribeAck(_) =>
      context.stop(self)
  }

}

private class ExecutionPlanView(shardRegion: ActorRef) extends Actor {
  import SchedulerProtocol._

  private[this] var finishedExecutionPlans = Set.empty[PlanId]
  private[this] var activeExecutionPlans = Set.empty[PlanId]

  def receive: Receive = {
    case get: GetExecutionPlan =>
      if (activeExecutionPlans.contains(get.planId)) {
        shardRegion.tell(get, sender())
      } else {
        sender() ! None
      }

    case GetExecutionPlans =>
      sender() ! (activeExecutionPlans ++ finishedExecutionPlans)

    case evt: ExecutionDriver.Created =>
      activeExecutionPlans += evt.planId

    case evt: ExecutionPlanFinished =>
      activeExecutionPlans -= evt.planId
      finishedExecutionPlans += evt.planId
  }

}