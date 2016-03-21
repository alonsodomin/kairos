package io.kairos.console.client.scheduler

import diode.data.PotMap
import diode.react.ModelProxy
import diode.react.ReactPot._
import io.kairos.ExecutionPlan
import io.kairos.console.client.core.LoadExecutionPlans
import io.kairos.fault.ExceptionThrown
import io.kairos.id.PlanId
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Created by alonsodomin on 30/01/2016.
  */
object ExecutionPlanList {

  case class Props(proxy: ModelProxy[PotMap[PlanId, ExecutionPlan]])

  class Backend($: BackendScope[Props, Unit]) {

    def mounted(props: Props): Callback = {
      def perform: Callback =
        Callback.log("Loading list of execution plans from backend...") >>
          props.proxy.dispatch(LoadExecutionPlans)

      Callback.ifTrue(props.proxy().size == 0, perform)
    }

    def render(p: Props) = {
      val model = p.proxy()
      <.table(^.`class` := "table table-striped",
        <.thead(
          <.tr(
            <.th("Job ID"),
            <.th("Plan ID")
          )
        ),
        <.tbody(
          model.seq.map { case (planId, schedule) =>
            <.tr(^.key := planId.toString(),
              schedule.renderFailed { ex =>
                <.td(^.colSpan := 4, ExceptionThrown(ex).toString())
              },
              schedule.renderPending(_ > 500, _ => "Loading ..."),
              schedule.render { item => List(
                <.td(item.jobId.toString()),
                <.td(item.planId.toString())
              )}
            )
          }
        )
      )
    }

  }

  private[this] val component = ReactComponentB[Props]("ExecutionPlanList").
    stateless.
    renderBackend[Backend].
    componentDidMount($ => $.backend.mounted($.props)).
    build

  def apply(proxy: ModelProxy[PotMap[PlanId, ExecutionPlan]]) = component(Props(proxy))

}