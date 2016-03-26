package io.quckoo.console.security

import diode.react.ModelProxy

import io.quckoo.console.SiteMap.ConsoleRoute
import io.quckoo.console.components._
import io.quckoo.console.core.{ConsoleScope, Login}
import io.quckoo.protocol.client.SignIn

import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ReactComponentB}

import scalacss.Defaults._
import scalacss.ScalaCssReact._

/**
 * Created by alonsodomin on 13/10/2015.
 */
object LoginPageView {

  object Style extends StyleSheet.Inline {
    import dsl._

    val formPlacement = style(
      width(350 px),
      height(300 px),
      position.absolute,
      left(50 %%),
      top(50 %%),
      marginLeft(-150 px),
      marginTop(-180 px)
    )
  }

  case class Props(proxy: ModelProxy[ConsoleScope], referral: Option[ConsoleRoute])

  class LoginBackend($: BackendScope[Props, Unit]) {

    def loginHandler(props: Props)(loginReq: SignIn): Callback =
      props.proxy.dispatch(Login(loginReq, props.referral))

    def render(props: Props) =
      <.div(Style.formPlacement,
        props.proxy().notification,
        Panel(Panel.Props("Sign in", ContextStyle.primary),
          LoginForm(loginHandler(props))
        )
      )
  }

  private[this] val component = ReactComponentB[Props]("LoginPage").
    stateless.
    renderBackend[LoginBackend].
    build

  def apply(proxy: ModelProxy[ConsoleScope], referral: Option[ConsoleRoute] = None) =
    component(Props(proxy, referral))

}