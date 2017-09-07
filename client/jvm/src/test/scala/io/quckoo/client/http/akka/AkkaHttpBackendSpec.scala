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

package io.quckoo.client.http.akka

import akka.actor.ActorSystem

import io.circe.generic.auto._

import io.quckoo.{ArtifactId, JobId}
import io.quckoo.client.http.{HttpMethod, HttpRequest, MockServer}
import io.quckoo.serialization.DataBuffer
import io.quckoo.serialization.json._

import org.mockserver.model.{
  JsonBody,
  HttpRequest => MockHttpRequest,
  HttpResponse => MockHttpResponse
}
import org.mockserver.verify.VerificationTimes

import org.scalatest.{Matchers, fixture}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Created by alonsodomin on 20/09/2016.
  */
class AkkaHttpBackendSpec extends fixture.FlatSpec with MockServer with Matchers {
  implicit val actorSystem = ActorSystem("AkkaHttpBackendSpec")

  override protected def afterAll(): Unit = {
    Await.ready(actorSystem.terminate(), Duration.Inf)
    super.afterAll()
  }

  "on send" should "parse error codes correctly in any HTTP method" in { mockServer =>
    val transport = new HttpAkkaBackend("localhost", mockServer.getPort)

    for (method <- HttpMethod.values) {
      val mockHttpRequest =
        MockHttpRequest.request("/nowhere").withMethod(method.entryName)
      val mockHttpResponse = MockHttpResponse.notFoundResponse()

      mockServer.when(mockHttpRequest).respond(mockHttpResponse)

      val response = Await.result(
        transport.send(HttpRequest(method, "/nowhere", Duration.Inf, Map.empty)),
        Duration.Inf
      )

      response.statusCode shouldBe 404

      mockServer.verify(mockHttpRequest, VerificationTimes.once())
    }
  }

  it should "send JSON body request and parse the JSON output" in { mockServer =>
    val transport = new HttpAkkaBackend("localhost", mockServer.getPort)

    val input  = ArtifactId("com.example", "example", "latest")
    val output = JobId("fooId")

    DataBuffer(input)
      .flatMap(in => DataBuffer(output).map(out => (in, out)))
      .foreach {
        case (in, out) =>
          val mockHttpRequest = MockHttpRequest
            .request("/path")
            .withMethod("POST")
            .withHeader("Content-Type", "application/json")
            .withBody(JsonBody.json(in.asString()))
          val mockHttpResponse =
            MockHttpResponse.response.withBody(JsonBody.json(out.asString()))

          mockServer.when(mockHttpRequest).respond(mockHttpResponse)

          val headers = Map("Content-Type" -> "application/json")
          val response = Await.result(
            transport.send(HttpRequest(HttpMethod.Post, "/path", Duration.Inf, headers, in)),
            Duration.Inf
          )

          mockServer.verify(mockHttpRequest, VerificationTimes.once())
          response.entity.asString() shouldBe out.asString()
      }
  }

  it should "send Authorization header" in { mockServer =>
    val transport = new HttpAkkaBackend("localhost", mockServer.getPort)

    val mockHttpRequest = MockHttpRequest
      .request("/path")
      .withMethod("POST")
      .withHeader("Authorization", "foo")
    val mockHttpResponse = MockHttpResponse.response.withBody("OK!")

    mockServer.when(mockHttpRequest).respond(mockHttpResponse)

    val headers = Map("Authorization" -> "foo")
    val response = Await.result(
      transport.send(HttpRequest(HttpMethod.Post, "/path", Duration.Inf, headers)),
      Duration.Inf
    )

    mockServer.verify(mockHttpRequest, VerificationTimes.once())
    response.entity.asString() shouldBe "OK!"
  }

}
