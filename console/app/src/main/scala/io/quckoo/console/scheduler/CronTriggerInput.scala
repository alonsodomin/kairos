package io.quckoo.console.scheduler

import cron4s._

import io.quckoo.Trigger
import io.quckoo.console.components._

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Created by alonsodomin on 02/09/2016.
  */
object CronTriggerInput {

  private[this] val errorMessage = ReactComponentB[(String, ParseError)]("CronTriggerInput.ErrorMessage").
    stateless.
    render_P { case (input, error) =>
      <.div(^.id := "cronParseError", ^.color.red,
        error.message, <.br,
        input, <.br,
        Iterator.fill(error.position.column-2)(NBSP).mkString + "^"
      )
    } build

  case class Props(value: Option[Trigger.Cron], onUpdate: Option[Trigger.Cron] => Callback)
  case class State(inputExpr: Option[String], parseError: Option[ParseError] = None)

  //implicit val propsReuse = Reusability.caseClass[Trigger.Cron]

  class Backend($: BackendScope[Props, State]) {

    private[this] def doValidate(value: Option[String]) = {
      import scalaz._
      import Scalaz._

      def updateError(err: Option[ParseError]): Callback =
        $.modState(_.copy(parseError = err)) >> $.props.flatMap(_.onUpdate(None))

      def invokeCallback(trigger: Option[Trigger.Cron]): Callback =
        updateError(None) >> $.props.flatMap(_.onUpdate(trigger))

      EitherT(value.map(Cron(_).disjunction)).map(Trigger.Cron).cozip.
        fold(updateError, invokeCallback)
    }

    def onUpdate(value: Option[String]) =
      $.modState(_.copy(inputExpr = value)) >> doValidate(value)

    val expressionInput = Input[String](onUpdate)

    def render(props: Props, state: State) = {
      <.div(^.`class` := "form-group",
        <.label(^.`class` := "col-sm-2 control-label", "Expression"),
        <.div(^.`class` := "col-sm-10",
          expressionInput(state.inputExpr, ^.id := "cronTrigger")
        ),
        <.div(^.`class` := "col-sm-offset-2",
          state.inputExpr.zip(state.parseError).
            map(p => errorMessage.withKey("cronError")(p))
        )
      )
    }

  }

  val component = ReactComponentB[Props]("CronTriggerInput").
    initialState_P(props => State(props.value.map(_.expr.toString()))).
    renderBackend[Backend].
    build

  def apply(value: Option[Trigger.Cron], onUpdate: Option[Trigger.Cron] => Callback) =
    component(Props(value, onUpdate))

}