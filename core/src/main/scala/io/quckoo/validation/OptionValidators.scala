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

package io.quckoo.validation

import scalaz._
import Scalaz._

import Violation.Undefined

/**
  * Created by alonsodomin on 21/10/2016.
  */
trait OptionValidators {

  def definedK[F[_]: Applicative, A]: ValidatorK[F, Option[A]] =
    Validator[F, Option[A]](a => Applicative[F].pure(a.isEmpty), _ => Undefined)

  def defined[A]: Validator[Option[A]] = definedK[Id, A]

}