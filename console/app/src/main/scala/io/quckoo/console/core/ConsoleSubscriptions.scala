package io.quckoo.console.core

import io.quckoo.client.QuckooClient
import io.quckoo.protocol.cluster.MasterEvent
import io.quckoo.protocol.scheduler.TaskQueueUpdated
import io.quckoo.protocol.worker.WorkerEvent
import monix.execution.Scheduler.Implicits.global

/**
  * Created by alonsodomin on 04/04/2016.
  */
private[core] trait ConsoleSubscriptions {

  def subscribeClusterState(implicit client: QuckooClient): Unit = {
    client.masterEvents.subscribe(new SimpleEventSubscriber[MasterEvent])
    client.workerEvents.subscribe(new SimpleEventSubscriber[WorkerEvent])
    client.queueMetrics.subscribe(new SimpleEventSubscriber[TaskQueueUpdated])
  }

}
