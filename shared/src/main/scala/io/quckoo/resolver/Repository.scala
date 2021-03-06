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

package io.quckoo.resolver

import java.io.{File, IOException}
import java.net.URL

import org.xml.sax.SAXParseException

import scala.xml.XML

/**
  * Created by aalonsodominguez on 18/07/15.
  */
sealed trait Repository {
  type RepositoryType <: Repository

  def name: String

}

sealed trait GenericRepository extends Repository {
  type RepositoryType <: GenericRepository

  def patterns: Patterns

  protected def copy(patterns: Patterns): RepositoryType

  def mavenStyle(): RepositoryType = copy(patterns.mavenStyle())
  def artifacts(artifactPatterns: String*): RepositoryType =
    copy(patterns.withArtifacts(artifactPatterns: _*))
  def ivys(ivyPatterns: String*): RepositoryType =
    copy(patterns.withIvys(ivyPatterns: _*))
}

final case class MavenRepository(name: String, url: URL) extends Repository {
  type RepositoryType = MavenRepository
}

final case class URLRepository(name: String, patterns: Patterns) extends GenericRepository {
  type RepositoryType = URLRepository

  override protected def copy(patterns: Patterns): RepositoryType =
    URLRepository(name, patterns)
}

final case class FileRepository(name: String, patterns: Patterns) extends GenericRepository {
  type RepositoryType = FileRepository

  override protected def copy(patterns: Patterns): RepositoryType =
    FileRepository(name, patterns)
}

object Repository {
  private[resolver] val mavenStyleBasePattern =
    "[organisation]/[module](_[scalaVersion])(_[sbtVersion])/[revision]/[artifact]-[revision](-[classifier]).[ext]"
  private[resolver] val sbtStylePattern =
    "[organisation]/[module]/[revision]/[type]s/[artifact](-[classifier]).[ext]"

  def mavenStylePatterns = Patterns(Nil, mavenStyleBasePattern :: Nil)

  def mavenRemote(name: String, baseURL: URL): Repository = {
    implicit val patterns: Patterns = mavenStylePatterns
    url(name, baseURL)
  }

  lazy val mavenCentral =
    MavenRepository("Maven Central", new URL("http://repo1.maven.org/maven2"))
  lazy val mavenLocal: Repository = {
    implicit val patterns: Patterns = mavenStylePatterns
    file("Maven Local", mavenLocalFolder)
  }

  def sbtLocal(name: String): Repository = {
    val pList    = ("${" + "ivy.home}/" + name + "/" + sbtStylePattern) :: Nil
    val patterns = Patterns(pList, pList, mavenCompatible = false)
    FileRepository(name, patterns)
  }

  object file {
    def apply(name: String, baseFolder: File)(implicit patterns: Patterns): FileRepository =
      repositoryFactory(new File(baseFolder.toURI.normalize).getAbsolutePath)(
        FileRepository(name, _)
      )
  }

  object url {
    def apply(name: String, baseURL: URL)(implicit patterns: Patterns): URLRepository =
      repositoryFactory(baseURL.toURI.normalize.toURL.toString)(URLRepository(name, _))
  }

  private def repositoryFactory[T <: GenericRepository](
      base: String
  )(constructor: Patterns => T)(implicit patterns: Patterns): T =
    constructor(Patterns.resolvePatterns(base, patterns))

  private[this] def mavenLocalFolder: File = {
    def loadHomeFromSettings(f: () => File): Option[File] =
      try {
        val file = f()
        if (!file.exists) None
        else
          (XML.loadFile(file) \ "localRepository").text match {
            case ""        => None
            case e: String => Some(new File(e))
          }
      } catch {
        // Occurs inside File constructor when property or environment variable does not exist
        case _: NullPointerException => None
        // Occurs when File does not exist
        case _: IOException => None
        case e: SAXParseException =>
          System.err.println(s"WARNING: Problem parsing ${f().getAbsolutePath}, ${e.getMessage}");
          None
      }

    loadHomeFromSettings(() => new File(System.getProperty("user.home"), ".m2/settings.xml")) orElse
      loadHomeFromSettings(() => new File(new File(System.getenv("M2_HOME")), "conf/settings.xml")) getOrElse
      new File(System.getProperty("user.home"), ".m2/repository")
  }

}
