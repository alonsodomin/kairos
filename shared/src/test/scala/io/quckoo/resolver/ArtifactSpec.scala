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

package io.quckoo.resolver

import java.net.URL

import io.quckoo.ArtifactId

import org.scalatest.{FlatSpec, Inside, Matchers}

import scala.collection.mutable
import scala.util.Success

/**
 * Created by aalonsodominguez on 26/07/15.
 */
class ArtifactSpec extends FlatSpec with Matchers with Inside {

  class StubArtifactClassLoader(urls: Array[URL]) extends ArtifactClassLoader(Array.empty) {
    private val classMap = mutable.Map.empty[String, Class[_]]

    def this() = this(Array.empty)

    def addClass(name: String, clazz: Class[_]): Unit = {
      classMap += (name -> clazz)
    }

    override def loadClass(name: String): Class[_] = classMap(name)

    override def getURLs: Array[URL] = urls

  }

  private val TestArtifactId = ArtifactId("io.kairos.test", "package-test", "SNAPSHOT")

  "A JobModulePackage" should "return the URLs of its classpath from the class loader" in {
    val expectedUrls = Array(new URL("http://www.example.com"))

    val stubClassLoader = new StubArtifactClassLoader(expectedUrls)
    val artifact = new Artifact(TestArtifactId, stubClassLoader)

    val returnedUrls = artifact.classpath

    returnedUrls should be (expectedUrls)
  }

  it should "load a job class from the class loader" in {
    val stubClassLoader   = new StubArtifactClassLoader
    val expectedClass     = classOf[DummyJavaJob]
    val expectedClassName = expectedClass.getName
    stubClassLoader.addClass(expectedClassName, expectedClass)

    val artifact = new Artifact(TestArtifactId, stubClassLoader)

    val returnedClass = artifact.jobClass(expectedClassName)

    inside (returnedClass) {
      case Success(clazz) => clazz should be (expectedClass)
    }
  }

  it should "inject parameters into the job instance" in {
    val stubClassLoader   = new StubArtifactClassLoader
    val expectedClass     = classOf[DummyJavaJob]
    val expectedClassName = expectedClass.getName
    stubClassLoader.addClass(expectedClassName, expectedClass)

    val parameterValue = 5
    val expectedJobInstance = new DummyJavaJob
    expectedJobInstance.value = parameterValue

    val artifact = new Artifact(TestArtifactId, stubClassLoader)

    val returnedInstance = artifact.newJob(expectedClassName, Map("value" -> parameterValue))

    inside (returnedInstance) {
      case Success(instance) => instance.asInstanceOf[DummyJavaJob].value should be (parameterValue)
    }
  }

}