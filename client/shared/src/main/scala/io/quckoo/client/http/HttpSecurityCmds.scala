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

package io.quckoo.client.http

import io.quckoo.auth.{Credentials, InvalidCredentials, Passport}
import io.quckoo.client.core.CmdMarshalling.{Anon, Auth}
import io.quckoo.client.core._
import io.quckoo.serialization.DataBuffer
import io.quckoo.util.Attempt

/**
  * Created by alonsodomin on 19/09/2016.
  */
trait HttpSecurityCmds extends HttpMarshalling with SecurityCmds[HttpProtocol] {

  private[this] def unmarshallPassport[O <: CmdMarshalling[HttpProtocol]] =
    Unmarshall[HttpResponse, Passport] { res =>
      if (res.isSuccess) Passport(res.entity.asString())
      else
        Attempt.fail {
          if (res.statusCode == 401) InvalidCredentials
          else HttpError(res.statusLine)
        }
    }

  implicit lazy val authenticateCmd: AuthenticateCmd =
    new Anon[HttpProtocol, Credentials, Passport] {

      override val marshall = Marshall[AnonCmd, Credentials, HttpRequest] { cmd =>
        DataBuffer.fromString(s"${cmd.payload.username}:${cmd.payload.password}").toBase64.map {
          creds =>
            val authHdr = AuthorizationHeader -> s"Basic $creds"

            HttpRequest(
              HttpMethod.Post,
              LoginURI,
              cmd.timeout,
              headers = httpHeaders(None, cmd.timeout) + authHdr)
        }
      }

      override val unmarshall = unmarshallPassport[AuthenticateCmd]
    }

  implicit lazy val refreshPassportCmd: RefreshPassportCmd =
    new Auth[HttpProtocol, Unit, Passport] {
      override val marshall = Marshall[AuthCmd, Unit, HttpRequest] { cmd =>
        Attempt.success {
          HttpRequest(
            HttpMethod.Post,
            AuthRefreshURI,
            cmd.timeout,
            httpHeaders(Some(cmd.passport), cmd.timeout))
        }
      }
      override val unmarshall = unmarshallPassport[RefreshPassportCmd]
    }

  implicit lazy val signOutCmd: SingOutCmd = new Auth[HttpProtocol, Unit, Unit] {
    override val marshall = Marshall[AuthCmd, Unit, HttpRequest] { cmd =>
      Attempt.success {
        HttpRequest(
          HttpMethod.Post,
          LogoutURI,
          cmd.timeout,
          httpHeaders(Some(cmd.passport), cmd.timeout))
      }
    }

    override val unmarshall = Unmarshall[HttpResponse, Unit] { res =>
      if (res.isSuccess) Attempt.unit
      else Attempt.fail(HttpError(res.statusLine))
    }
  }
}
