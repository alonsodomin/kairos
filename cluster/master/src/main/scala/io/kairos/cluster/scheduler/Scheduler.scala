package io.kairos.cluster.scheduler

import java.util.UUID

import akka.actor._
import akka.cluster.client.ClusterClientReceptionist
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.pattern._
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.Timeout
import io.kairos.{ExecutionPlan, JobSpec}
import io.kairos.cluster.core.KairosJournal
import io.kairos.cluster.protocol.WorkerProtocol
import io.kairos.id._
import io.kairos.protocol.{RegistryProtocol, SchedulerProtocol}
import io.kairos.time.TimeSource

import scala.concurrent.Future
import scala.concurrent.duration._
import scalaz.ListT
import scalaz.std.scalaFuture._

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

  override implicit def actorSystem: ActorSystem = context.system

  override def receive: Receive = {
    case cmd: ScheduleJob =>
      val handler = context.actorOf(jobFetcherProps(cmd.jobId, sender(), cmd), "handler")
      registry.tell(GetJob(cmd.jobId), handler)

    case cmd @ CreateExecutionDriver(_, config, _) =>
      log.debug("Found enabled job {}. Initializing a new execution plan for it.", config.jobId)
      val props = factoryProps(config.jobId, cmd, shardRegion)
      context.actorOf(props, s"execution-plan-factory-${config.jobId}")

    case get: GetExecutionPlan =>
      import context.dispatcher

      val requestor = sender()
      ListT(executionPlanIds).find(_ == get.planId).isEmpty.flatMap { empty =>
        if (empty) Future.successful(None)
        else {
          implicit val timeout = Timeout(2 seconds)
          (shardRegion ? get).mapTo[ExecutionPlan]
        }
      } pipeTo requestor

    case GetExecutionPlans =>
      import context.dispatcher
      executionPlanIds pipeTo sender()

    case msg: WorkerMessage =>
      taskQueue.tell(msg, sender())
  }

  private def executionPlanIds: Future[List[PlanId]] = {
    readJournal.eventsByTag(SchedulerTagEventAdapter.tags.ExecutionPlan, 0).
      filter(env =>
        env.event match {
          case evt: ExecutionDriver.Created => true
          case _                            => false
        }
      ).map(env =>
      env.event.asInstanceOf[ExecutionDriver.Created].planId
    ).runFold(List.empty[PlanId]) {
      case (list, planId) => planId :: list
    }
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