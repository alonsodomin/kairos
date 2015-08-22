package io.chronos.resolver

import java.net.URL

import io.chronos.id.ModuleId
import io.chronos.protocol._
import org.apache.ivy.Ivy
import org.apache.ivy.core.module.descriptor._
import org.apache.ivy.core.module.id.{ModuleRevisionId => IvyModuleId}
import org.apache.ivy.core.report.ArtifactDownloadReport
import org.apache.ivy.core.resolve.ResolveOptions
import org.slf4s.Logging

/**
 * Created by aalonsodominguez on 17/07/15.
 */
trait DependencyResolver {

  def resolve(jobModuleId: ModuleId, download: Boolean = false): Either[ResolutionFailed, JobPackage]

}

class IvyDependencyResolver(config: IvyConfiguration) extends DependencyResolver with Logging {

  private val DefaultConfName = "default"
  private val CompileConfName = "compile"
  private val RuntimeConfName = "runtime"

  private lazy val ivy = Ivy.newInstance(config)

  def resolve(jobModuleId: ModuleId, download: Boolean): Either[ResolutionFailed, JobPackage] = {
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
      Left(ResolutionFailed(resolveReport.getUnresolvedDependencies.map(_.getModuleId.toString)))
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