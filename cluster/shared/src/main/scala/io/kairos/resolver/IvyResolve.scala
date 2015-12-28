package io.kairos.resolver

import java.net.URL

import akka.actor.ActorSystem
import io.kairos.id.ModuleId
import io.kairos.protocol._
import org.apache.ivy.Ivy
import org.apache.ivy.core.module.descriptor._
import org.apache.ivy.core.module.id.{ModuleRevisionId => IvyModuleId}
import org.apache.ivy.core.report.ArtifactDownloadReport
import org.apache.ivy.core.resolve.ResolveOptions

/**
 * Created by aalonsodominguez on 17/07/15.
 */
object IvyResolve {

  def apply(system: ActorSystem): IvyResolve = {
    val settings = IvyConfiguration(system.settings.config)
    new IvyResolve(settings)
  }

}

class IvyResolve(config: IvyConfiguration) extends ResolveFun {

  private val DefaultConfName = "default"
  private val CompileConfName = "compile"
  private val RuntimeConfName = "runtime"

  private lazy val ivy = Ivy.newInstance(config)

  def apply(jobModuleId: ModuleId, download: Boolean): Either[ResolutionFailed, JobPackage] = {
    def newCallerInstance(confs: Array[String]): ModuleDescriptor = {
      val moduleRevisionId: IvyModuleId = IvyModuleId.newInstance(
        jobModuleId.group, jobModuleId.artifact, jobModuleId.version
      )

      val descriptor = new DefaultModuleDescriptor(IvyModuleId.newInstance(
        moduleRevisionId.getOrganisation, moduleRevisionId.getName + "-job", "working"), "execution", null, true
      )
      confs.foreach(c => descriptor.addConfiguration(new Configuration(c)))
      descriptor.setLastModified(System.currentTimeMillis)

      val dependencyDescriptor = new DefaultDependencyDescriptor(descriptor, moduleRevisionId, true, true, true)
      confs.foreach(c => dependencyDescriptor.addDependencyConfiguration(c, c))
      descriptor.addDependency(dependencyDescriptor)

      descriptor
    }

    val configurations = Array(DefaultConfName, CompileConfName, RuntimeConfName)

    val moduleDescriptor = newCallerInstance(configurations)

    val resolveOptions = new ResolveOptions().
      setTransitive(true).
      setValidate(true).
      setDownload(download).
      setOutputReport(false).
      setConfs(Array(RuntimeConfName))

    val resolveReport = ivy.resolve(moduleDescriptor, resolveOptions)

    if (resolveReport.hasError) {
      Left(UnresolvedDependencies(resolveReport.getUnresolvedDependencies.map(_.getModuleId.toString)))
    } else {
      Right(JobPackage(jobModuleId, artifactLocations(resolveReport.getAllArtifactsReports)))
    }
  }

  private def artifactLocations(artifactReports: Seq[ArtifactDownloadReport]): Seq[URL] = {
    for (report <- artifactReports) yield {
      Option(report.getUnpackedLocalFile).
        orElse(Option(report.getLocalFile)) match {
        case Some(file) => Right(file)
        case None       => Left(report.getArtifact.getUrl)
      }
    } fold (identity, _.toURI.toURL)
  }

}
