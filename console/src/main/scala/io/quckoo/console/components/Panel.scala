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

package io.quckoo.console.components

import io.quckoo.console.layout.{CssSettings, ContextStyle, lookAndFeel}

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scalacss.ScalaCssReact._

/**
  * Created by alonsodomin on 20/02/2016.
  */
object Panel {
  import CssSettings._

  final case class Props(
      heading: String,
      style: ContextStyle.Value,
      onHeaderClick: Option[Callback],
      addStyles: Seq[StyleA]
  )

  val component = ScalaComponent
    .builder[Props]("Panel")
    .stateless
    .renderPC { (_, p, c) =>
      <.div(
        lookAndFeel.panelOpt(p.style),
        p.addStyles.toTagMod,
        <.div(
          lookAndFeel.panelHeading,
          p.heading,
          p.onHeaderClick.map(cb => ^.onClick --> cb).whenDefined,
          p.onHeaderClick.map(_ => ^.cursor.pointer).whenDefined
        ),
        <.div(lookAndFeel.panelBody, c)
    )
  } build

  def apply(heading: String,
            style: ContextStyle.Value = ContextStyle.default,
            onHeaderClick: Option[Callback] = None,
            addStyles: Seq[StyleA] = Seq.empty) =
    component(Props(heading, style, onHeaderClick, addStyles)) _

}
