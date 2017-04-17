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

package io.quckoo.client.http

import enumeratum._
import enumeratum.EnumEntry._

import io.quckoo.serialization.DataBuffer

import scala.concurrent.duration.Duration

/**
  * Created by alonsodomin on 10/09/2016.
  */
sealed trait HttpMethod extends EnumEntry with Uppercase
object HttpMethod extends Enum[HttpMethod] {
  val values = findValues

  case object Get    extends HttpMethod
  case object Put    extends HttpMethod
  case object Post   extends HttpMethod
  case object Delete extends HttpMethod
}

final case class HttpRequest(
    method: HttpMethod,
    url: String,
    timeout: Duration,
    headers: Map[String, String],
    entity: DataBuffer = DataBuffer.Empty
)

final case class HttpResponse(statusCode: Int,
                              statusLine: String,
                              entity: DataBuffer = DataBuffer.Empty) {
  def isFailure: Boolean = statusCode >= 400
  def isSuccess: Boolean = !isFailure
}
final case class HttpError(statusLine: String) extends Exception(statusLine)

final case class HttpServerSentEvent(data: DataBuffer)
