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

package io.quckoo.client.http.akka

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpHeader.ParsingResult
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpHeader, HttpMethods}
import akka.http.scaladsl.model.{
  HttpMethod => AkkaHttpMethod,
  HttpRequest => AkkaHttpRequest,
  HttpResponse => AkkaHttpResponse
}
import akka.http.scaladsl.{Http => AkkaHttp}
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.ByteString

import de.heikoseeberger.akkasse.ServerSentEvent
import de.heikoseeberger.akkasse.client.EventSource

import io.quckoo.client.core.Channel
import io.quckoo.client.http.{HttpMethod, HttpRequest, HttpResponse, _}
import io.quckoo.serialization.DataBuffer

import monix.reactive.Observable

import scala.collection.immutable
import scala.concurrent.Future

import scalaz.Kleisli

/**
  * Created by alonsodomin on 11/09/2016.
  */
private[http] final class HttpAkkaBackend(host: String, port: Int = 80)(
    implicit val actorSystem: ActorSystem)
    extends HttpBackend {

  implicit val materializer =
    ActorMaterializer(ActorMaterializerSettings(actorSystem), "quckoo-http")

  val connection = AkkaHttp().outgoingConnection(host, port)

  override def open[Ch <: Channel[HttpProtocol]](channel: Ch) =
    Kleisli[Observable, Unit, HttpServerSentEvent] { _ =>
      val publisherSink = Sink.asPublisher[ServerSentEvent](fanout = true)

      val eventSource = EventSource(s"http://$host:$port" + EventsURI, AkkaHttp().singleRequest(_))
        .runWith(publisherSink)

      Observable.fromReactivePublisher(eventSource).map { sse =>
        HttpServerSentEvent(DataBuffer.fromString(sse.toString))
      }
    }

  override def send: Kleisli[Future, HttpRequest, HttpResponse] = Kleisli { req =>
    def method: AkkaHttpMethod = req.method match {
      case HttpMethod.Get    => HttpMethods.GET
      case HttpMethod.Put    => HttpMethods.PUT
      case HttpMethod.Post   => HttpMethods.POST
      case HttpMethod.Delete => HttpMethods.DELETE
    }

    val headers = {
      req.headers
        .filterKeys(_ != "Content-Type")
        .map({
          case (name, value) => HttpHeader.parse(name, value)
        })
        .flatMap {
          case ParsingResult.Ok(header, _) => Seq(header)
          case _                           => Seq()
        }
        .to[immutable.Seq]
    }

    def parseRawResponse(response: AkkaHttpResponse): Future[HttpResponse] = {
      val entityData = response.entity.dataBytes.runFold(ByteString())(_ ++ _)

      import actorSystem.dispatcher
      entityData.map(
        buff =>
          HttpResponse(
            response.status.intValue(),
            response.status.value,
            DataBuffer.fromString(buff.utf8String)))
    }

    val entity = HttpEntity(ContentTypes.`application/json`, req.entity.asString())
    Source
      .single(AkkaHttpRequest(method, uri = req.url, entity = entity, headers = headers))
      .via(connection)
      .mapAsync(1)(parseRawResponse)
      .runWith(Sink.head[HttpResponse])
  }
}
