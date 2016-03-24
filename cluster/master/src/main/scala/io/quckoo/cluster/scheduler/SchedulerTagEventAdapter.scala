package io.quckoo.cluster.scheduler

import akka.persistence.journal.{Tagged, WriteEventAdapter}
import io.quckoo.protocol.SchedulerProtocol

/**
  * Created by alonsodomin on 13/03/2016.
  */
class SchedulerTagEventAdapter extends WriteEventAdapter {
  import SchedulerProtocol._
  import SchedulerTagEventAdapter._

  override def manifest(event: Any): String = ""

  override def toJournal(event: Any): Any = event match {
    case evt: ExecutionDriver.Created => Tagged(evt, Set(tags.ExecutionPlan))
    case evt: TaskScheduled           => Tagged(evt, Set(tags.ExecutionPlan, tags.Task))
    case evt: TaskCompleted           => Tagged(evt, Set(tags.ExecutionPlan, tags.Task))
    case evt: ExecutionPlanStarted    => Tagged(evt, Set(tags.ExecutionPlan))
    case evt: ExecutionPlanFinished   => Tagged(evt, Set(tags.ExecutionPlan))

    case _ => event
  }

}

object SchedulerTagEventAdapter {

  object tags {
    final val ExecutionPlan = "ExecutionPlan"
    final val Task          = "Task"
  }

}