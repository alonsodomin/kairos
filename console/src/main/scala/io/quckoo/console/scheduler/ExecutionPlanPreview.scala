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

import java.time.{Clock, ZonedDateTime}

import io.quckoo.Trigger
import io.quckoo.console.components._

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Created by alonsodomin on 09/04/2016.
  */
object ExecutionPlanPreview {

  case class Props(trigger: Trigger, clock: Clock)
  case class State(maxRows: Int)

  class Backend($ : BackendScope[Props, State]) {

    def generateTimeline(props: Props, state: State): Stream[ZonedDateTime] = {
      import Trigger._

      def genNext(prev: ReferenceTime): (ReferenceTime, Boolean) =
        props.trigger
          .nextExecutionTime(prev)(props.clock)
          .map(next => (LastExecutionTime(next), true))
          .getOrElse(prev -> false)

      val first  = genNext(ScheduledTime(ZonedDateTime.now(props.clock)))
      val stream = Stream.iterate(first) { case (prev, _) => genNext(prev) }
      stream.takeWhile { case (_, continue) => continue } map (_._1.when) take state.maxRows
    }

    def onRowsSelectionUpdate(evt: ReactEventFromInput): Callback =
      evt.extract(_.target.value.toInt)(value => $.modState(_.copy(maxRows = value)))

    def render(props: Props, state: State) =
      <.div(
        <.div(
          ^.`class` := "form-group",
          <.label(^.`class` := "col-sm-2", "Trigger"),
          <.div(^.`class` := "col-sm-10", props.trigger.toString())
        ),
        <.div(
          ^.`class` := "form-group",
          <.label(^.`class` := "col-sm-2", "Max Rows"),
          <.div(
            ^.`class` := "col-sm-2",
            <.select(
              ^.id := "previewRows",
              ^.`class` := "form-control",
              ^.value := state.maxRows,
              ^.onChange ==> onRowsSelectionUpdate,
              <.option(^.value := 10, "10"),
              <.option(^.value := 25, "25"),
              <.option(^.value := 50, "50")
            )
          )
        ),
        <.table(
          ^.`class` := "table table-striped",
          <.thead(
            <.tr(<.th("Expected Executions"))
          ),
          <.tbody(
            generateTimeline(props, state).zipWithIndex.toVdomArray {
              case (time, idx) =>
                <.tr(^.key := s"executionplan_timeline_$idx", <.td(DateTimeDisplay(time)))
            }
          )
        )
      )

  }

  val component = ScalaComponent
    .builder[Props]("ExecutionPlanPreview")
    .initialState(State(10))
    .renderBackend[Backend]
    .build

  def apply(trigger: Trigger)(implicit clock: Clock) =
    component(Props(trigger, clock))

}
