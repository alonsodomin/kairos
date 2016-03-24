package io.quckoo.console.client.core

import io.quckoo.api.{Registry, Scheduler}
import io.quckoo.console.client.security.ClientAuth
import io.quckoo.console.info.ClusterInfo
import io.quckoo.console.protocol.LoginRequest
import io.quckoo.console.ConsoleAuth
import io.quckoo.id.{JobId, PlanId}
import io.quckoo.protocol.RegistryProtocol
import io.quckoo.protocol.SchedulerProtocol
import io.quckoo.serialization
import io.quckoo.{ExecutionPlan, JobSpec, Validated}

import org.scalajs.dom

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by alonsodomin on 13/10/2015.
 */
object ConsoleClient extends ConsoleAuth with Registry with Scheduler with ClientAuth {
  import dom.ext.Ajax
  import upickle.default._
  import serialization.json.scalajs._

  import RegistryProtocol._
  import SchedulerProtocol._

  private[this] val BaseURI = "/api"
  private[this] val LoginURI = BaseURI + "/login"
  private[this] val LogoutURI = BaseURI + "/logout"

  private[this] val ClusterDetailsURI = BaseURI + "/cluster/info"

  private[this] val RegistryBaseURI = BaseURI + "/registry"
  private[this] val JobsURI = RegistryBaseURI + "/jobs"

  private[this] val SchedulerBaseURI = BaseURI + "/scheduler"
  private[this] val ExecutionPlansURI = SchedulerBaseURI + "/plans"

  private[this] val JsonRequestHeaders = Map(
    "Content-Type" -> "application/json"
  )

  override def login(username: String, password: String)(implicit ec: ExecutionContext): Future[Unit] = {
    Ajax.post(LoginURI, write(LoginRequest(username, password)), headers = JsonRequestHeaders).
      map { xhr => () }
  }

  override def logout()(implicit ec: ExecutionContext): Future[Unit] = {
    Ajax.post(LogoutURI, headers = authHeaders) map { xhr => () }
  }

  override def clusterDetails(implicit ec: ExecutionContext): Future[ClusterInfo] = {
    Ajax.get(ClusterDetailsURI, headers = authHeaders) map { xhr =>
      read[ClusterInfo](xhr.responseText)
    }
  }

  override def enableJob(jobId: JobId)(implicit ec: ExecutionContext): Future[JobEnabled] = {
    Ajax.post(JobsURI + "/" + jobId + "/enable", headers = authHeaders).map { xhr =>
      read[JobEnabled](xhr.responseText)
    }
  }

  override def disableJob(jobId: JobId)(implicit ec: ExecutionContext): Future[JobDisabled] = {
    Ajax.post(JobsURI + "/" + jobId + "/disable", headers = authHeaders).map { xhr =>
      read[JobDisabled](xhr.responseText)
    }
  }

  override def fetchJob(jobId: JobId)(implicit ec: ExecutionContext): Future[Option[JobSpec]] = {
    Ajax.get(JobsURI + "/" + jobId, headers = authHeaders ++ JsonRequestHeaders).map { xhr =>
      Some(read[JobSpec](xhr.responseText))
    } recover {
      case _ => None
    }
  }

  override def registerJob(jobSpec: JobSpec)(implicit ec: ExecutionContext): Future[Validated[JobId]] = {
    Ajax.put(JobsURI, write(jobSpec), headers = authHeaders ++ JsonRequestHeaders).map { xhr =>
      read[Validated[JobId]](xhr.responseText)
    }
  }

  override def fetchJobs(implicit ec: ExecutionContext): Future[Map[JobId, JobSpec]] = {
    Ajax.get(JobsURI, headers = authHeaders ++ JsonRequestHeaders).map { xhr =>
      read[Map[JobId, JobSpec]](xhr.responseText)
    }
  }

  override def executionPlan(planId: PlanId)(implicit ec: ExecutionContext): Future[Option[ExecutionPlan]] = {
    Ajax.get(ExecutionPlansURI + "/" + planId, headers = authHeaders).map { xhr =>
      Some(read[ExecutionPlan](xhr.responseText))
    } recover {
      case _ => None
    }
  }

  override def allExecutionPlanIds(implicit ec: ExecutionContext): Future[Set[PlanId]] = {
    Ajax.get(ExecutionPlansURI, headers = authHeaders).map { xhr =>
      read[Set[PlanId]](xhr.responseText)
    }
  }

  override def schedule(scheduleJob: ScheduleJob)
                       (implicit ec: ExecutionContext): Future[Either[JobNotFound, ExecutionPlanStarted]] = {
    Ajax.post(ExecutionPlansURI, write(scheduleJob), headers = authHeaders ++ JsonRequestHeaders).map { xhr =>
      Right(read[ExecutionPlanStarted](xhr.responseText))
    } recover {
      case _ => Left(JobNotFound(scheduleJob.jobId))
    }
  }
}