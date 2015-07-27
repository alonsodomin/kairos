package io.chronos

/**
 * Created by aalonsodominguez on 19/07/2015.
 */
package object protocol {

  case class ResolutionFailed(unresolvedDependencies: Seq[String])

  type JobRejectedCause = Either[ResolutionFailed, Throwable]
  type ExecutionFailedCause = Either[ResolutionFailed, Throwable]

}
