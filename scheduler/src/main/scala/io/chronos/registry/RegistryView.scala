package io.chronos.registry

import akka.cluster.Cluster
import akka.persistence.PersistentView
import io.chronos.JobSpec
import io.chronos.id.JobId
import io.chronos.protocol.RegistryProtocol

/**
 * Created by aalonsodominguez on 29/08/15.
 */
class RegistryView extends PersistentView {
  import RegistryProtocol._

  private var enabledJobs = Map.empty[JobId, JobSpec]

  override val persistenceId: String = Cluster(context.system).selfRoles.find(_.startsWith("backend-")) match {
    case Some(role) => role + "-registry"
    case None       => "registry"
  }

  override val viewId: String = self.path.name

  def receive: Receive = {
    case JobAccepted(jobId, jobSpec) =>
      enabledJobs += (jobId -> jobSpec)

    case JobDisabled(jobId) =>
      enabledJobs -= jobId

    case GetJobs =>
      sender() ! enabledJobs.values.toIndexedSeq
  }

}
