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

package io.quckoo.console.scheduler

import org.scalajs.dom.html

import CronTriggerInputState._

/**
  * Created by alonsodomin on 03/09/2016.
  */
class CronTriggerInputObserver($: HtmlDomZipper) {

  val expressionInput = $("#cronTrigger").domAs[html.Input]
  val parseError = $.collect01("#cronParseError").
    mapZippers(_.domAs[html.Div]).map(_.innerHTML)

}