package io.chronos.protocol

import io.chronos.cluster._
import io.chronos.id.TaskId

/**
 * Created by aalonsodominguez on 05/07/15.
 */
object WorkerProtocol {
  // Messages from workers
  case class RegisterWorker(workerId: WorkerId)
  case class RequestTask(workerId: WorkerId)
  case class TaskDone(workerId: WorkerId, taskId: TaskId, result: Any)
  case class TaskFailed(workerId: WorkerId, taskId: TaskId, cause: TaskFailureCause)

  // Messages to workers
  case object TaskReady
  case class TaskDoneAck(taskId: TaskId)
}