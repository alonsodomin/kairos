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

package io.quckoo.console.scheduler

import java.time.{LocalDate, LocalDateTime, LocalTime, ZoneId}

import io.quckoo.Trigger
import io.quckoo.console.components._

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Created by alonsodomin on 08/04/2016.
  */
object AtTriggerInput {

  case class Props(value: Option[Trigger.At],
                   onUpdate: Option[Trigger.At] => Callback,
                   readOnly: Boolean)
  case class State(date: Option[LocalDate], time: Option[LocalTime])

  implicit val propsReuse: Reusability[Props] =
    Reusability.caseClassExcept('onUpdate)
  implicit val stateReuse: Reusability[State] = Reusability.caseClass

  class Backend($ : BackendScope[Props, State]) {

    def propagateUpdate: Callback = {
      val value = $.state.map(st => st.date.flatMap(date => st.time.map(time => (date, time))))
      value.flatMap {
        case Some((date, time)) =>
          val dateTime = LocalDateTime.of(date, time)
          val trigger  = Trigger.At(dateTime.atZone(ZoneId.systemDefault))
          $.props.flatMap(_.onUpdate(Some(trigger)))

        case _ =>
          $.props.flatMap(_.onUpdate(None))
      }
    }

    def onDateUpdate(value: Option[LocalDate]): Callback =
      $.modState(_.copy(date = value), propagateUpdate)

    def onTimeUpdate(value: Option[LocalTime]): Callback =
      $.modState(_.copy(time = value), propagateUpdate)

    private[this] val DateInput = Input[LocalDate]
    private[this] val TimeInput = Input[LocalTime]

    def render(props: Props, state: State) =
      <.div(
        <.div(
          ^.`class` := "form-group",
          <.label(^.`class` := "col-sm-2 control-label", "Date"),
          <.div(
            ^.`class` := "col-sm-10",
            DateInput(
              state.date,
              onDateUpdate _,
              ^.id := "atTrigger_date",
              ^.readOnly := props.readOnly
            )
          )
        ),
        <.div(
          ^.`class` := "form-group",
          <.label(^.`class` := "col-sm-2 control-label", "Time"),
          <.div(
            ^.`class` := "col-sm-10",
            TimeInput(
              state.time,
              onTimeUpdate _,
              ^.id := "atTrigger_time",
              ^.readOnly := props.readOnly
            )
          )
        )
      )

  }

  val component = ScalaComponent
    .builder[Props]("AtTriggerInput")
    .initialStateFromProps(_ => State(None, None))
    .renderBackend[Backend]
    .configure(Reusability.shouldComponentUpdate)
    .build

  def apply(value: Option[Trigger.At],
            onUpdate: Option[Trigger.At] => Callback,
            readOnly: Boolean = false) =
    component(Props(value, onUpdate, readOnly))

}
