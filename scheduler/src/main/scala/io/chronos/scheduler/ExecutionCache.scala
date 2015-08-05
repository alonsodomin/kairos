package io.chronos.scheduler

import java.time.{Clock, ZonedDateTime}

import io.chronos.id._
import io.chronos.{Execution, Schedule}

/**
 * Created by aalonsodominguez on 01/08/15.
 */
trait ExecutionCache {

  def getSchedule(scheduleId: ScheduleId): Option[Schedule]

  def getScheduledJobs: Seq[(ScheduleId, Schedule)]

  def aliveSchedules(f: (ScheduleId, Schedule, ZonedDateTime) => Boolean): Traversable[(ScheduleId, Schedule)]

  def getExecution(executionId: ExecutionId): Option[Execution]

  def getExecutions(filter: Execution => Boolean): Traversable[Execution]

  def currentExecutionOf(scheduleId: ScheduleId): Option[ExecutionId]

  def schedule(jobSchedule: Schedule)(implicit clock: Clock): Execution

  def reschedule(scheduleId: ScheduleId)(implicit clock: Clock): Execution

  def nextExecutionTime(scheduleId: ScheduleId)(implicit clock: Clock): Option[ZonedDateTime]

  def sweepOverdueExecutions(batchLimit: Int)(f: ExecutionId => Unit)(implicit clock: Clock): Unit

  def updateExecution[T](executionId: ExecutionId, stage: Execution.Stage)(f: Execution => T): T

}
