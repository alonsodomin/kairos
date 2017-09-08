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

package io.quckoo.cluster.config

import io.quckoo.config._

import eu.timepit.refined.auto._

import com.typesafe.config.ConfigFactory
import org.scalatest.{FlatSpec, Inside, Matchers}

import scala.concurrent.duration._
import scala.util.Success

/**
  * Created by alonsodomin on 04/11/2016.
  */
object ClusterSettingsSpec {

  object taskQueue {
    final val DefaultMaxWorkTimeout = 10 minutes
  }

  object http {
    final val DefaultInterface: IPv4  = "0.0.0.0"
    final val DefaultPort: PortNumber = 8095
  }

}

class ClusterSettingsSpec extends FlatSpec with Matchers with Inside {
  import ClusterSettingsSpec._

  "ClusterConfig" should "be able to load default configuration" in {
    val config               = ConfigFactory.load()
    val clusterConfigAttempt = ClusterSettings(config)

    inside(clusterConfigAttempt) {
      case Success(clusterConf) =>
        clusterConf.taskQueue.maxWorkTimeout shouldBe taskQueue.DefaultMaxWorkTimeout
        clusterConf.http.bindInterface shouldBe http.DefaultInterface
        clusterConf.http.bindPort shouldBe http.DefaultPort
    }
  }

}
