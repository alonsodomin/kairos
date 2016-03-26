package io.quckoo.api

import io.quckoo.ExecutionPlan
import io.quckoo.auth.AuthInfo
import io.quckoo.id.PlanId
import io.quckoo.protocol.scheduler._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alonsodomin on 13/03/2016.
  */
trait Scheduler {

  def executionPlan(planId: PlanId)(implicit ec: ExecutionContext, auth: AuthInfo): Future[Option[ExecutionPlan]]

  def allExecutionPlanIds(implicit ec: ExecutionContext, auth: AuthInfo): Future[Set[PlanId]]

  def schedule(schedule: ScheduleJob)(implicit ec: ExecutionContext, auth: AuthInfo): Future[Either[JobNotFound, ExecutionPlanStarted]]

}
