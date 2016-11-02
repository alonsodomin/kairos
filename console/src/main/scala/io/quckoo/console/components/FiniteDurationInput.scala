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

package io.quckoo.console.components

import java.util.concurrent.TimeUnit

import io.quckoo.validation.Validators._
import io.quckoo.console.validation._

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra._
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.concurrent.duration._

import scalaz._

/**
  * Created by alonsodomin on 08/04/2016.
  */
object FiniteDurationInput {

  val SupportedUnits = Seq(
    MILLISECONDS -> "Milliseconds",
    SECONDS      -> "Seconds",
    MINUTES      -> "Minutes",
    HOURS        -> "Hours",
    DAYS         -> "Days"
  )

  case class Props(id: String,
                   value: Option[FiniteDuration],
                   onUpdate: Option[FiniteDuration] => Callback)
  case class State(length: Option[Long], unit: Option[TimeUnit]) {

    def this(duration: Option[FiniteDuration]) =
      this(duration.map(_.length), duration.map(_.unit))

  }

  implicit val propsReuse: Reusability[Props] = Reusability.by(_.value)
  implicit val stateReuse                     = Reusability.caseClass[State]

  class Backend($ : BackendScope[Props, State]) {

    def propagateUpdate: Callback = {
      val valuePair = $.state.map(st => st.length.flatMap(l => st.unit.map(u => (l, u))))

      val value = valuePair.map {
        case Some((length, unit)) =>
          Some(FiniteDuration(length, unit))
        case _ => None
      }

      value.flatMap(v => $.props.flatMap(_.onUpdate(v)))
    }

    def onLengthUpdate(value: Option[Long]): Callback =
      $.modState(_.copy(length = value), propagateUpdate)

    def onUnitUpdate(evt: ReactEventI): Callback = {
      val value = {
        if (evt.target.value.isEmpty) None
        else Some(TimeUnit.valueOf(evt.target.value))
      }
      $.modState(_.copy(unit = value), propagateUpdate)
    }

    val _lengthInput = Input[Long]()
    val validateLength = {
      import Scalaz._
      ValidatedInput[Long]((greaterThan(0L) or equalTo(0L)).callback)
    }

    def lengthInput(id: String, state: State)(onUpdate: Option[Long] => Callback) =
      _lengthInput(state.length, onUpdate, ^.id := s"${id}_length")

    def render(props: Props, state: State) = {
      val id = props.id
      <.div(
        ^.`class` := "container-fluid",
        <.div(
          ^.`class` := "row",
          <.div(^.`class` := "col-sm-4",
            validateLength(onLengthUpdate _)(lengthInput(id, state))
          ),
          <.div(
            ^.`class` := "col-sm-6",
            <.select(
              ^.id := s"${id}_unit",
              ^.`class` := "form-control",
              state.unit.map(u => ^.value := u.toString()),
              ^.onChange ==> onUnitUpdate,
              <.option("Select a time unit..."),
              SupportedUnits.map {
                case (u, text) =>
                  <.option(^.value := u.name(), text)
              }
            )
          )
        )
      )
    }

  }

  val component = ReactComponentB[Props]("FiniteDurationInput")
    .initialState_P(props => new State(props.value))
    .renderBackend[Backend]
    .configure(Reusability.shouldComponentUpdate)
    .build

  def apply(id: String,
            value: Option[FiniteDuration],
            onUpdate: Option[FiniteDuration] => Callback) =
    component(Props(id, value, onUpdate))

}