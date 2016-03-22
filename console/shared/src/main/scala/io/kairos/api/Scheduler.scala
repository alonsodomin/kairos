package io.kairos.api

import io.kairos.ExecutionPlan
import io.kairos.id._
import io.kairos.protocol.SchedulerProtocol

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alonsodomin on 13/03/2016.
  */
trait Scheduler {
  import SchedulerProtocol._

  def executionPlan(planId: PlanId)(implicit ec: ExecutionContext): Future[Option[ExecutionPlan]]

  def allExecutionPlanIds(implicit ec: ExecutionContext): Future[Set[PlanId]]

  def schedule(schedule: ScheduleJob)(implicit ec: ExecutionContext): Future[Either[JobNotFound, ExecutionPlanStarted]]

}
