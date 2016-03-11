package io.kairos.cluster.scheduler.execution

import java.util.UUID

import akka.actor._
import akka.cluster.pubsub.{DistributedPubSubMediator, DistributedPubSub}
import io.kairos.Trigger._
import io.kairos.fault.{ExceptionThrown, Faults}
import io.kairos.id._
import io.kairos.protocol.{RegistryProtocol, SchedulerProtocol}
import io.kairos.time.{DateTime, TimeSource}
import io.kairos.{JobSpec, Trigger}

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 16/08/15.
 */
object ExecutionPlan {

  def props(planId: PlanId, trigger: Trigger)(executionProps: ExecutionFSMProps)(implicit timeSource: TimeSource) =
    Props(classOf[ExecutionPlan], planId, trigger, executionProps, timeSource)

}

class ExecutionPlan(val planId: PlanId, trigger: Trigger, executionProps: ExecutionFSMProps)(implicit timeSource: TimeSource)
  extends Actor with ActorLogging {

  import RegistryProtocol._
  import SchedulerProtocol._

  private val mediator = DistributedPubSub(context.system).mediator

  private var triggerTask: Option[Cancellable] = None
  private val scheduledTime = timeSource.currentDateTime
  private var lastExecutionTime: Option[DateTime] = None

  private var originalRequestor: Option[ActorRef] = None

  override def preStart(): Unit =
    mediator ! DistributedPubSubMediator.Subscribe(RegistryTopic, self)

  override def postStop(): Unit =
    mediator ! DistributedPubSubMediator.Unsubscribe(RegistryTopic, self)

  override def receive: Receive = {
    case (jobId: JobId, jobSpec: JobSpec) =>
      originalRequestor = Some(sender())
      context.become(schedule(jobId, jobSpec))

    case JobNotEnabled(jobId) =>
      log.info("Given job can't be scheduled as it is not enabled. jobId={}", jobId)
      context.become(shutdown)
  }

  private def active(jobId: JobId, jobSpec: JobSpec): Receive = {
    case JobDisabled(id) if id == jobId =>
      log.info("Job has been disabled. jobId={}", id)
      context.become(shutdown)

    case ExecutionFSM.Result(outcome) =>
      def nextStage: Receive = if (trigger.isRecurring) {
        outcome match {
          case _: Execution.Success =>
            schedule(jobId, jobSpec)

          case Execution.Failure(cause) =>
            if (shouldRetry(cause))
              schedule(jobId, jobSpec)
            else shutdown

          case _ =>
            // Plan is no longer needed
            shutdown
        }
      } else shutdown

      lastExecutionTime = Some(timeSource.currentDateTime)
      context.become(nextStage)
  }

  private def shouldRetry(cause: Faults): Boolean =
    !cause.list.exists(_.isInstanceOf[ExceptionThrown])

  private def shutdown: Receive = {
    if (triggerTask.isDefined) {
      log.debug("Cancelling trigger for execution plan. planId={}", planId)
      triggerTask.foreach( _.cancel() )
      triggerTask = None
    }

    log.info("Stopping execution plan. planId={}", planId)
    self ! PoisonPill

    { case _ => log.error("Execution plan '{}' unavailable, shutting down.", planId) }
  }

  private def schedule(jobId: JobId, jobSpec: JobSpec): Receive = {
    def nextExecutionTime: Option[DateTime] = trigger.nextExecutionTime(lastExecutionTime match {
      case Some(time) => LastExecutionTime(time)
      case None       => ScheduledTime(scheduledTime)
    })

    def triggerDelay: Option[FiniteDuration] = {
      val now = timeSource.currentDateTime
      nextExecutionTime match {
        case Some(time) if time.isBefore(now) || time.isEqual(now) =>
          Some(0 millis)
        case Some(time) =>
          val delay = now.diff(time)
          Some(delay.toMillis millis)
        case None => None
      }
    }

    triggerDelay match {
      case Some(delay) =>
        import context.dispatcher

        // Create a new execution
        val taskId = UUID.randomUUID()
        log.info("Scheduling a new execution. jobId={}, taskId={}", jobId, taskId)
        val execution = context.actorOf(executionProps(taskId, jobSpec), "exec-" + taskId)
        triggerTask = Some(context.system.scheduler.scheduleOnce(delay, execution, ExecutionFSM.WakeUp))
        originalRequestor.foreach { _ ! JobScheduled(jobId, planId) }

        active(jobId, jobSpec)

      case _ => shutdown
    }
  }

}
