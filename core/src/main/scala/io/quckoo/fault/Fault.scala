/*
 * Copyright 2016 Antonio Alonso Dominguez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quckoo.fault

import io.quckoo.id.{ArtifactId, JobId}

import scalaz.NonEmptyList

/**
  * Created by alonsodomin on 28/12/2015.
  */
sealed trait Fault extends Serializable

// == Generic errors ================

final case class ExceptionThrown(className: String, message: String) extends Fault {

  override def toString: String = s"$className: $message"

}
object ExceptionThrown {
  def from(t: Throwable): ExceptionThrown = ExceptionThrown(t.getClass.getName, t.getMessage)
}

// == Artifact resolution errors ============

case class MissingDependencies(dependencies: NonEmptyList[DependencyFault]) extends Fault

sealed trait DependencyFault extends Fault {
  val artifactId: ArtifactId
}
case class UnresolvedDependency(artifactId: ArtifactId) extends DependencyFault

object DownloadFailed {
  sealed trait Reason
  case object NotFound extends Reason
  final case class Other(message: String) extends Reason
}
case class DownloadFailed(artifactId: ArtifactId, reason: DownloadFailed.Reason) extends DependencyFault

// == Validation errors ====================

sealed trait ValidationFault extends Fault

case class NotNull(msg: String) extends ValidationFault
case class Required(msg: String) extends ValidationFault