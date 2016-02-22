package io.kairos.console.client

import io.kairos._

import scalaz._

/**
  * Created by alonsodomin on 21/02/2016.
  */
package object validation {
  import Scalaz._

  type Validator[T] = T => Validated[T]

  def notEmptyStr(fieldId: String)(str: String): Validated[String] = {
    if (str.isEmpty) Required(fieldId).asInstanceOf[Fault].failureNel[String]
    else str.successNel[Fault]
  }

}
