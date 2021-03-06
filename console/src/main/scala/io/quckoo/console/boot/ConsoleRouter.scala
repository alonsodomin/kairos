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

package io.quckoo.console.boot

import diode.react.ModelProxy

import io.quckoo.console.ConsoleRoute
import io.quckoo.console.core.ConsoleScope
import io.quckoo.console.dashboard.DashboardPage
import io.quckoo.console.layout.Layout
import io.quckoo.console.log.LogRecord
import io.quckoo.console.registry.RegistryPage
import io.quckoo.console.scheduler.SchedulerPage
import io.quckoo.console.security.LoginPage

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.router._
import japgolly.scalajs.react.vdom.html_<^._

import monix.reactive.Observable

/**
  * Created by aalonsodominguez on 12/10/2015.
  */
object ConsoleRouter {
  import ConsoleRoute._

  private[this] def publicPages(proxy: ModelProxy[ConsoleScope]) =
    RouterConfigDsl[ConsoleRoute].buildRule { dsl =>
      import dsl._

      (emptyRule
        | staticRoute(root, Root) ~> redirectToPage(Dashboard)(Redirect.Push)
        | staticRoute("#login", Login) ~> render(LoginPage(proxy)))
    }

  private[this] def privatePages(proxy: ModelProxy[ConsoleScope]) =
    RouterConfigDsl[ConsoleRoute].buildRule { dsl =>
      import dsl._

      def isLoggedIn: CallbackTo[Boolean] =
        CallbackTo { proxy().passport.isDefined }

      def redirectToLogin(referral: ConsoleRoute) =
        Some(render(LoginPage(proxy, Some(referral))))

      (emptyRule
        | staticRoute("#home", Dashboard) ~> render(DashboardPage(proxy))
        | staticRoute("#registry", Registry) ~> render(RegistryPage(proxy))
        | staticRoute("#scheduler", Scheduler) ~> render(SchedulerPage(proxy)))
        .addCondition(isLoggedIn)(redirectToLogin)
    }

  def config(proxy: ModelProxy[ConsoleScope], logStream: Observable[LogRecord]) =
    RouterConfigDsl[ConsoleRoute].buildConfig { dsl =>
      import dsl._

      (emptyRule
        | publicPages(proxy)
        | privatePages(proxy))
        .notFound(redirectToPage(Root)(Redirect.Replace))
        .renderWith(Layout(proxy, logStream) _)
        .verify(ConsoleRoute.values.head, ConsoleRoute.values.tail: _*)
    }

  val baseUrl = BaseUrl.fromWindowOrigin_/

}
