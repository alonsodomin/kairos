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

package io.quckoo.serialization.base64

import scala.collection.immutable.HashMap

/**
  * Created by alonsodomin on 20/10/2016.
  */
class Scheme(val encodeTable: IndexedSeq[Char]) {
  lazy val decodeTable = HashMap(encodeTable.zipWithIndex: _*)
}
