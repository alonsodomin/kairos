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

package io.quckoo.cluster.core

import akka.actor.{Actor, ActorLogging, Props}

import io.quckoo.auth.UserId

import scala.concurrent.duration.FiniteDuration

/**
  * Created by alonsodomin on 14/10/2015.
  */
object UserAuthenticator {

  case class Authenticate(userId: UserId, password: Array[Char])
  case class AuthenticationSuccess()
  case object AuthenticationFailed

  def props(sessionTimeout: FiniteDuration): Props =
    Props(classOf[UserAuthenticator], sessionTimeout)

}

class UserAuthenticator(sessionTimeout: FiniteDuration) extends Actor with ActorLogging {
  import UserAuthenticator._

  def receive = {
    case Authenticate(userId, password) =>
  }

}