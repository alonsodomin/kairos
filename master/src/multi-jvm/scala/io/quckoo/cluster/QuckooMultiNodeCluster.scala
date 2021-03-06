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

package io.quckoo.cluster

import akka.persistence.Persistence
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import akka.stream.ActorMaterializer
import akka.testkit.ImplicitSender

import com.typesafe.config.ConfigFactory

import io.quckoo.ArtifactId
import io.quckoo.cluster.config.ClusterSettings
import io.quckoo.cluster.core.QuckooGuardian
import io.quckoo.cluster.journal.QuckooTestJournal
import io.quckoo.multijvm.MultiNodeClusterSpec
import io.quckoo.protocol.client._
import io.quckoo.testkit.ImplicitClock

import scala.concurrent.duration._
import scala.concurrent.{Await, Promise}
import scala.util.Success

/**
 * Created by domingueza on 26/08/15.
 */
object QuckooNodesConfig extends MultiNodeConfig {
  val scheduler = role(QuckooRoles.Scheduler)
  val registry  = role(QuckooRoles.Registry)

  commonConfig(debugConfig(on = false))

  nodeConfig(scheduler)(
    ConfigFactory.parseString("akka.cluster.roles=[scheduler]"),
    MultiNodeClusterSpec.clusterConfig
  )
  nodeConfig(registry)(
    ConfigFactory.parseString("akka.cluster.roles=[registry]"),
    MultiNodeClusterSpec.clusterConfig
  )
}

class QuckooMultiNodeClusterSpecMultiJvmNode1 extends QuckooMultiNodeCluster
class QuckooMultiNodeClusterSpecMultiJvmNode2 extends QuckooMultiNodeCluster

object QuckooMultiNodeCluster {

  val GuardianName = "quckoo"
  val TestArtifactId = ArtifactId("io.quckoo", "quckoo-example-jobs_2.11", "0.1.0")

}

abstract class QuckooMultiNodeCluster extends MultiNodeSpec(QuckooNodesConfig) with ImplicitSender
  with MultiNodeClusterSpec with ImplicitClock {

  import QuckooNodesConfig._
  import QuckooMultiNodeCluster._

  implicit val materializer = ActorMaterializer()
  val journal = new QuckooTestJournal

  Persistence(system)

  "A Quckoo cluster" must {
    val Success(settings) = ClusterSettings(system.settings.config)

    "send connect commands from one node to the other one" in {
      awaitClusterUp(registry, scheduler)

      runOn(registry) {
        val bootPromise = Promise[Unit]
        system.actorOf(QuckooGuardian.props(settings, journal, bootPromise), GuardianName)
        Await.ready(bootPromise.future, 5 seconds)

        enterBarrier("deployed")

        val schedulerGuardian = system.actorSelection(node(scheduler) / "user" / GuardianName)
        schedulerGuardian ! Connect

        expectMsg(Connected)

        enterBarrier("connected")

        schedulerGuardian ! Disconnect
        expectMsg(Disconnected)

        enterBarrier("disconnected")
      }

      runOn(scheduler) {
        val bootPromise = Promise[Unit]
        system.actorOf(QuckooGuardian.props(settings, journal, bootPromise), GuardianName)
        Await.ready(bootPromise.future, 5 seconds)

        enterBarrier("deployed")

        val registryGuardian = system.actorSelection(node(registry) / "user" / GuardianName)
        registryGuardian ! Connect

        expectMsg(Connected)

        enterBarrier("connected")

        registryGuardian ! Disconnect
        expectMsg(Disconnected)

        enterBarrier("disconnected")
      }

      enterBarrier("finished")
    }

  }

}
