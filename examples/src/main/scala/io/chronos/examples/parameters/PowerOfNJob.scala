package io.chronos.examples.parameters

import io.chronos.Job

/**
 * Created by domingueza on 06/07/15.
 */
class PowerOfNJob extends Job {
  var n = 2

  override def execute(): String = {
    val n2 = n * n
    s"$n * $n = $n2"
  }

}