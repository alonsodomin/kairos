package io.quckoo.client.http

import io.quckoo.auth.{Credentials, Passport}
import io.quckoo.serialization.Base64
import io.quckoo.serialization.json._
import io.quckoo.client.core._
import io.quckoo.fault.Fault
import io.quckoo.id.JobId
import io.quckoo.net.QuckooState
import io.quckoo.protocol.registry.{JobEnabled, RegisterJob}

import upickle.default.{Reader => JsonReader, Writer => JsonWriter}

import slogging.LazyLogging

import scala.util.{Try, Failure => Fail}

import scalaz._
import Scalaz._

/**
  * Created by alonsodomin on 10/09/2016.
  */
final class HttpDriver(protected val transport: HttpTransport)
  extends Driver[Protocol.Http] with LazyLogging {
  type TransportRepr = HttpTransport

  private[this] abstract class HttpMarshalling[Cmd[_] <: Command[_], In: JsonWriter, Rslt: JsonReader]
    extends Marshalling[Cmd, In, Rslt] {

    override val from: Unmarshall[HttpResponse, Rslt] = {
      case HttpSuccess(payload) =>
        payload.as[Rslt]

      case err: HttpError =>
        Fail(HttpErrorException(err))
    }

  }

  val ops = new Ops {
    implicit val authenticateOp: Marshalling[AnonCmd, Credentials, Passport] =
      new Marshalling[AnonCmd, Credentials, Passport] {

        override val to: Marshall[AnonCmd, Credentials, HttpRequest] = { cmd =>
          import Base64._

          Try(s"${cmd.payload.username}:${cmd.payload.password}".getBytes("UTF-8").toBase64).map { creds =>
            val hdrs = Map(AuthorizationHeader -> s"Basic $creds")
            HttpRequest(HttpMethod.Post, LoginURI, cmd.timeout, headers = hdrs, None)
          }
        }

        override val from: Unmarshall[HttpResponse, Passport] = {
          case HttpSuccess(payload) =>
            payload.asString().map(new Passport(_))

          case err: HttpError =>
            Fail(HttpErrorException(err))
        }
    }

    override implicit val clusterStateOp: Marshalling[AuthCmd, Unit, QuckooState] =
      new HttpMarshalling[AuthCmd, Unit, QuckooState] {

        override val to: Marshall[AuthCmd, Unit, HttpRequest] = { cmd =>
          logger.debug("Retrieving current cluster state...")
          val hrds = Map(cmd.passport.asHttpHeader)
          Try(HttpRequest(HttpMethod.Get, ClusterStateURI, cmd.timeout, headers = hrds, None))
        }

    }

    override implicit val enableJobOp: Marshalling[AuthCmd, JobId, JobEnabled] =
      new HttpMarshalling[AuthCmd, JobId, JobEnabled] {

        override val to: Marshall[AuthCmd, JobId, HttpRequest] = { cmd =>
          val hrds = Map(cmd.passport.asHttpHeader)
          Try(HttpRequest(HttpMethod.Post, s"$JobsURI/${cmd.payload}/enable", cmd.timeout, hrds, None))
        }

      }

    override implicit val registerJobOp: Marshalling[AuthCmd, RegisterJob, ValidationNel[Fault, JobId]] =
      new HttpMarshalling[AuthCmd, RegisterJob, ValidationNel[Fault, JobId]] {

        override val to: Marshall[AuthCmd, RegisterJob, HttpRequest] = { cmd =>
          DataBuffer(cmd.payload) map { entity =>
            val hdrs = JsonRequestHeaders + cmd.passport.asHttpHeader
            HttpRequest(HttpMethod.Put, JobsURI, cmd.timeout, hdrs, Some(entity))
          }
        }

      }
  }

}
