/*
 * Copyright 2015 A. Alonso Dominguez
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

package io.quckoo.reflect

import java.net.URL

import cats.{Eq, Show}
import cats.implicits._

import io.quckoo.ArtifactId

/**
  * Created by aalonsodominguez on 17/07/15.
  */
object Artifact {

  implicit val artifactEq: Eq[Artifact] = Eq.fromUniversalEquals

  implicit val artifactShow: Show[Artifact] = Show.show { artifact =>
    val classpath = artifact.classpath.mkString(":")
    show"${artifact.artifactId} :: $classpath"
  }

}

final case class Artifact(artifactId: ArtifactId, classpath: List[URL]) {

  lazy val classLoader: ClassLoader = new ArtifactClassLoader(classpath.toArray)

}
