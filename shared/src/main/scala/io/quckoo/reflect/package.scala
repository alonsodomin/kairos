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

package io.quckoo

import cats.Monad
import cats.effect.IO
import cats.free.Free

/**
  * Created by alonsodomin on 04/05/2017.
  */
package object reflect {
  type ReflectIO[A] = Free[ReflectOp, A]

  implicit class ReflectIOOps[A](val self: ReflectIO[A]) extends AnyVal {

    def to[F[_]: Monad](implicit interpreter: ReflectorInterpreter[F]): F[A] =
      self.foldMap(interpreter)

  }

}
