package io.chronos.id

import java.util.UUID

import io.chronos.JobSpec

/**
 * Created by aalonsodominguez on 24/08/15.
 */
object JobId {

  def apply(jobSpec: JobSpec): JobId = {
    val id = UUID.nameUUIDFromBytes(jobSpec.moduleId.toString.getBytes("UTF-8"))
    new JobId(id.toString)
  }

}

final class JobId private (private val id: String) extends Serializable {

  override def equals(other: Any): Boolean = other match {
    case that: JobId => that.id equals this.id
    case _           => false
  }

  override def hashCode = id.hashCode

  override def toString = id

}
