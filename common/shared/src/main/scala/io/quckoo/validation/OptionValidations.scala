package io.quckoo.validation

import io.quckoo.fault.{Required, ValidationFault}
import io.quckoo.Validated

import scalaz._

/**
  * Created by alonsodomin on 31/01/2016.
  */
trait OptionValidations {
  import Scalaz._

  def defined[T](t: Option[T])(msg: => String): Validated[Option[T]] = {
    if (t.isEmpty) Required(msg).failureNel[Option[T]]
    else t.successNel[ValidationFault]
  }

}