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

package io.quckoo.worker

import akka.actor.{AddressFromURIString, RootActorPath}
import pureconfig._

import scala.util.{Failure, Success}

/**
  * Created by alonsodomin on 04/11/2016.
  */
package object config {

  final val AkkaArteryCanonicalHost = "akka.remote.artery.canonical.hostname"
  final val AkkaArteryCanonicalPort = "akka.remote.artery.canonical.port"
  final val AkkaArteryBindHost = "akka.remote.artery.bind.hostname"
  final val AkkaArteryBindPort = "akka.remote.artery.bind.port"

  final val DefaultUdpInterface = "127.0.0.1"
  final val DefaultUdpPort      = 50010

  final val ExecutorDispatcher = "quckoo.worker.dispatcher"
  final val ResolverDispatcher = "quckoo.resolver.dispatcher"

  final val HostAndPort = """(.+?):(\d+)""".r

  implicit val contactPointConfig: ConfigConvert[ContactPoint] = ConfigConvert.fromNonEmptyString {
    case AddressFromURIString(addr) => Success(new ContactPoint(RootActorPath(addr) / "system" / "receptionist"))
    case str                        => Failure(new IllegalArgumentException(s"Invalid contact point: $str"))
  }

}
