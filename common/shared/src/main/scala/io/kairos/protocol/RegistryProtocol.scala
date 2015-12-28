package io.kairos.protocol

import io.kairos.JobSpec
import io.kairos.id._

/**
 * Created by aalonsodominguez on 21/08/15.
 */
object RegistryProtocol {

  sealed trait RegistryCommand
  sealed trait RegistryEvent

  case class GetJob(jobId: JobId) extends RegistryCommand
  case object GetJobs extends RegistryCommand
  case class JobNotEnabled(jobId: JobId)

  case class RegisterJob(job: JobSpec) extends RegistryCommand
  case class JobAccepted(jobId: JobId, job: JobSpec) extends RegistryEvent
  case class JobRejected(artifactId: ArtifactId, cause: JobRejectedCause) extends RegistryEvent

  case class DisableJob(jobId: JobId) extends RegistryCommand
  case class JobDisabled(jobId: JobId) extends RegistryEvent

}