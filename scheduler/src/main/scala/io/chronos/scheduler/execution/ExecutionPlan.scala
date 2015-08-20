package io.chronos.scheduler.execution

import java.time.{Clock, Duration => JDuration, ZonedDateTime}
import java.util.UUID

import akka.actor._
import io.chronos.Trigger._
import io.chronos.id._
import io.chronos.scheduler.Registry.{JobDisabled, JobNotEnabled}
import io.chronos.{JobSpec, Trigger}

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 16/08/15.
 */
object ExecutionPlan {

  type ExecutionProps = (PlanId, JobSpec) => Props

  def props(trigger: Trigger)(executionProps: ExecutionProps)(implicit clock: Clock) =
    Props(classOf[ExecutionPlan], trigger, executionProps, clock)

}

class ExecutionPlan(trigger: Trigger, executionProps: ExecutionPlan.ExecutionProps)(implicit clock: Clock)
  extends Actor with ActorLogging {

  private val planId: PlanId = UUID.randomUUID()
  private var triggerTask: Option[Cancellable] = None
  private val scheduledTime = ZonedDateTime.now(clock)
  private var lastExecutionTime: Option[ZonedDateTime] = None

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    context.system.eventStream.subscribe(self, classOf[JobDisabled])
  }

  @throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    context.system.eventStream.unsubscribe(self)
  }

  override def receive: Receive = {
    case jobSpec: JobSpec =>
      context.become(scheduleTrigger(jobSpec.id, jobSpec))

    case JobNotEnabled(_) =>
      log.info("Job can't be scheduled")
      self ! PoisonPill
  }

  private def active(jobId: JobId, jobSpec: JobSpec): Receive = {
    case JobDisabled(id) if id == jobId =>
      log.info("Job has been disabled. jobId={}", id)
      triggerTask.foreach { _.cancel() }
      triggerTask = None
      self ! PoisonPill
      context.become(inactive)

    case Execution.Result(outcome) =>
      lastExecutionTime = Some(ZonedDateTime.now(clock))
      if (trigger.isRecurring) {
        outcome match {
          case _: Execution.Success =>
            context.become(scheduleTrigger(jobId, jobSpec))

          case _ =>
            // Plan is no longer needed
            self ! PoisonPill
        }
      }
  }

  private def inactive: Receive = {
    case _ => self ! PoisonPill
  }

  private def scheduleTrigger(jobId: JobId, jobSpec: JobSpec): Receive = triggerDelay match {
    case Some(delay) =>
      import context.dispatcher
      // Create a new execution
      val execution = context.actorOf(executionProps(planId, jobSpec))
      triggerTask = Some(context.system.scheduler.scheduleOnce(delay, execution, Execution.WakeUp))
      active(jobId, jobSpec)
    case _ =>
      self ! PoisonPill
      inactive
  }

  private def triggerDelay: Option[FiniteDuration] = {
    val now = ZonedDateTime.now(clock)
    nextExecutionTime match {
      case Some(time) if time.isBefore(now) || time.isEqual(now) =>
        Some(0 millis)
      case Some(time) =>
        val delay = JDuration.between(now, time)
        Some(delay.toMillis millis)
      case None => None
    }
  }

  private def nextExecutionTime: Option[ZonedDateTime] = trigger.nextExecutionTime(lastExecutionTime match {
    case Some(time) => LastExecutionTime(time)
    case None       => ScheduledTime(scheduledTime)
  })

}