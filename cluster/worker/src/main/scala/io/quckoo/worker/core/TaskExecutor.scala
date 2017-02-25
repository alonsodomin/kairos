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

package io.quckoo.worker.core

import akka.actor.{Actor, ActorLogging}
import io.quckoo.fault.Fault

/**
  * Created by alonsodomin on 16/02/2017.
  */
object TaskExecutor {

  case object Run
  sealed trait Response extends Product with Serializable
  final case class Failed(error: Fault) extends Response
  final case class Completed(result: Any) extends Response

}

trait TaskExecutor extends Actor with ActorLogging