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

import io.quckoo.time._

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra._
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.annotation.implicitNotFound

/**
  * Created by alonsodomin on 07/04/2016.
  */
object Input {

  @implicitNotFound("Type $A is not supported as Input component")
  sealed trait Converter[A] {
    def from: String => A
    def to: A => String
  }
  object Converter {
    abstract sealed class BaseConverter[A] extends Converter[A] {
      override def to: A => String = _.toString
    }

    implicit val string: Converter[String] = new Converter[String] {
      def to: String => String = identity
      def from: String => String = identity
    }

    implicit val password: Converter[Password] = new Converter[Password] {
      def to: Password => String = _.value
      def from: String => Password = Password(_)
    }

    implicit val int: Converter[Int] = new BaseConverter[Int] {
      override def from: String => Int = _.toInt
    }

    implicit val long: Converter[Long] = new BaseConverter[Long] {
      override def from: String => Long = _.toLong
    }

    implicit val date: Converter[Date] = new BaseConverter[Date] {
      override def from: String => Date = MomentJSDate.parse
    }

    implicit val time: Converter[Time] = new BaseConverter[Time] {
      override def from: String => Time = MomentJSTime.parse
    }

  }

  @implicitNotFound("Type $A is not supported as Input component")
  sealed abstract class Type[A](val html: String)
  object Type {
    implicit val string   = new Type[String]("text") {}
    implicit val password = new Type[Password]("password") {}
    implicit val int      = new Type[Int]("number") {}
    implicit val long     = new Type[Long]("number") {}
    implicit val date     = new Type[Date]("date") {}
    implicit val time     = new Type[Time]("time") {}
  }

  type OnUpdate[A] = Option[A] => Callback

  final case class Props[A](
      value: Option[A],
      defaultValue: Option[A],
      onUpdate: OnUpdate[A],
      attrs: Seq[TagMod]
  )(implicit val converter: Converter[A], val `type`: Type[A])

  class Backend[A]($: BackendScope[Props[A], Unit]) {

    def onUpdate(props: Props[A])(evt: ReactEventI): Callback = {
      def convertNewValue: CallbackTo[Option[A]] = CallbackTo {
        if (evt.target.value.isEmpty) None
        else Some(props.converter.from(evt.target.value))
      }

      def propagateChange(value: Option[A]): Callback =
        props.onUpdate(value)

      convertNewValue >>= propagateChange
    }

    def render(props: Props[A]) = {
      def defaultValueAttr: Option[TagMod] =
        props.defaultValue.map(v => ^.defaultValue := props.converter.to(v))
      def valueAttr: TagMod =
        ^.value := props.value.map(v => props.converter.to(v)).getOrElse("")

      <.input(^.`type` := props.`type`.html,
        ^.`class` := "form-control",
        defaultValueAttr.getOrElse(valueAttr),
        ^.onChange ==> onUpdate(props),
        ^.onBlur ==> onUpdate(props),
        props.attrs
      )
    }

  }

  implicit def propsReuse[A : Reusability]: Reusability[Props[A]] =
    Reusability.by[Props[A], (Option[A], Option[A])](p => (p.value, p.defaultValue))

  def component[A : Reusability] = ReactComponentB[Props[A]]("Input").
    stateless.
    renderBackend[Backend[A]].
    configure(Reusability.shouldComponentUpdate[Props[A], Unit, Backend[A], TopNode]).
    build

  def apply[A : Reusability](value: Option[A], onUpdate: OnUpdate[A], attrs: TagMod*)(implicit C: Converter[A], T: Type[A]) =
    component[A].apply(Props(value, None, onUpdate, attrs))

  def apply[A : Reusability](value: Option[A], defaultValue: Option[A], onUpdate: OnUpdate[A], attrs: TagMod*)(implicit C: Converter[A], T: Type[A]) =
    component[A].apply(Props(value, defaultValue, onUpdate, attrs))

}

