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

import io.quckoo.auth.{Credentials, Passport}

/**
  * Created by alonsodomin on 19/09/2016.
  */
trait SecurityCmds[P <: Protocol] {
  type AuthenticateCmd    = CmdMarshalling.Anon[P, Credentials, Passport]
  type RefreshPassportCmd = CmdMarshalling.Auth[P, Unit, Passport]
  type SingOutCmd         = CmdMarshalling.Auth[P, Unit, Unit]

  implicit def authenticateCmd: AuthenticateCmd
  implicit def refreshPassportCmd: RefreshPassportCmd
  implicit def signOutCmd: SingOutCmd
}
