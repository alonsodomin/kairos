package io.kairos.protocol

import io.kairos.id.ArtifactId

/**
  * Created by alonsodomin on 28/12/2015.
  */
sealed trait ErrorResponse extends Serializable

// == Generic errors ================

case class ExceptionThrown(className: String, message: String) extends ErrorResponse {

  override def toString: String = s"$className: $message"

}

object ExceptionThrown {
  def apply(t: Throwable): ExceptionThrown = ExceptionThrown(t.getClass.getName, t.getMessage)
}

// == Artifact resolution errors ============

sealed trait ResolutionFailed extends ErrorResponse

case class UnresolvedDependency(artifactId: ArtifactId) extends ResolutionFailed {
  override def toString = artifactId.toString
}

case class DownloadFailed(artifactName: String) extends ResolutionFailed