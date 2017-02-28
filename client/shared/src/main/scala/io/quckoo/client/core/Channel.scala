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

package io.quckoo.client.core

import io.quckoo.api.Topic

/**
  * Created by domingueza on 20/09/2016.
  */
trait Channel[P <: Protocol] {
  type Event

  val topic: Topic[Event]
  val unmarshall: Unmarshall[P#EventType, Event]
}

object Channel {
  trait Aux[P <: Protocol, E] extends Channel[P] { type Event = E }
}
