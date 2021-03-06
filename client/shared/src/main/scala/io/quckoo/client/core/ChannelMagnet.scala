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

package io.quckoo.client.core

import io.quckoo.api.TopicTag
import io.quckoo.serialization.Decoder

/**
  * Created by alonsodomin on 20/09/2016.
  */
trait ChannelMagnet[E] {
  implicit def topicTag: TopicTag[E]
  implicit def decoder: Decoder[String, E]

  def resolve[P <: Protocol](driver: Driver[P]): Channel.Aux[P, E] = driver.channelFor[E]
}

object ChannelMagnet {
  implicit def apply[E](implicit topicTag0: TopicTag[E],
                        decoder0: Decoder[String, E]): ChannelMagnet[E] =
    new ChannelMagnet[E] {
      implicit val topicTag: TopicTag[E]       = topicTag0
      implicit val decoder: Decoder[String, E] = decoder0
    }
}
