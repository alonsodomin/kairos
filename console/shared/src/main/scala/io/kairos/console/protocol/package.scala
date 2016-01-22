package io.kairos.console

import io.kairos.id.JobId
import io.kairos.protocol.ErrorResponse

import scalaz.ValidationNel

/**
  * Created by alonsodomin on 29/12/2015.
  */
package object protocol {

  type RegisterJobResponse = ValidationNel[ErrorResponse, JobId]

}