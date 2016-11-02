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

import io.quckoo.console.components.Notification._
import io.quckoo.fault.Fault

import japgolly.scalajs.react.ReactNode
import japgolly.scalajs.react.vdom.prefix_<^._

import org.scalajs.jquery.jQuery

import scala.scalajs.js
import scala.language.implicitConversions

/**
  * Created by alonsodomin on 05/07/2016.
  */
object Notification {

  object Level extends Enumeration {
    val Danger, Warning, Info, Success = Value
  }

  private final val AlertType = Map(
    Level.Danger  -> "danger",
    Level.Warning -> "warning",
    Level.Info    -> "info",
    Level.Success -> "success"
  )

  private final val AlertStyle = Map(
    Level.Danger  -> ContextStyle.danger,
    Level.Warning -> ContextStyle.warning,
    Level.Info    -> ContextStyle.info,
    Level.Success -> ContextStyle.success
  )

  private final val AlertIcon = Map(
    Level.Danger  -> Icons.exclamationCircle,
    Level.Warning -> Icons.exclamationTriangle,
    Level.Info    -> Icons.questionCircle,
    Level.Success -> Icons.checkCircle
  )

  final case class Props(
      message: String,
      title: Option[String] = None,
      url: Option[String] = None,
      closable: Boolean = true
  )

  sealed trait PropsMagnet {
    def apply(): Props
  }
  object PropsMagnet {

    implicit def fromString(string: String): PropsMagnet =
      () => Props(string)

    implicit def fromFault(fault: Fault): PropsMagnet =
      () => Props(fault.toString, title = Some("Error"))

    implicit def fromThrowable(throwable: Throwable): PropsMagnet =
      () => Props(throwable.getMessage, title = Some(throwable.getClass.getSimpleName))

  }

  def apply(level: Level.Value, magnet: PropsMagnet): Notification =
    Notification(level, magnet.apply())

  def info(magnet: PropsMagnet) = apply(Level.Info, magnet)

  def success(magnet: PropsMagnet) = apply(Level.Success, magnet)

  def warning(magnet: PropsMagnet) = apply(Level.Warning, magnet)

  def danger(magnet: PropsMagnet) = apply(Level.Danger, magnet)

}

final case class Notification private[components] (
    level: Level.Value = Level.Info,
    props: Props
) {

  def growl(): Unit = {
    val settings = js.Dynamic.literal(
      "type"          -> AlertType(level),
      "allow_dismiss" -> props.closable
    )
    jQuery.showNotification(props.message, settings)
    ()
  }

  def inline: ReactNode = {
    Alert(
      AlertStyle(level),
      <.div(
        ^.`class` := "row",
        <.div(^.`class` := "col-sm-2", AlertIcon(level)),
        <.div(^.`class` := "col-sm-10", props.message)))
  }

}