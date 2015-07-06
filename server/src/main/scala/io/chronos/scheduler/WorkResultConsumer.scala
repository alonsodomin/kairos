package io.chronos.scheduler

import akka.actor.{Actor, ActorLogging}
import akka.contrib.pattern.{DistributedPubSubExtension, DistributedPubSubMediator}
import io.chronos.scheduler.butler.Master
import io.chronos.scheduler.worker.WorkResult

/**
 * Created by aalonsodominguez on 05/07/15.
 */
class WorkResultConsumer extends Actor with ActorLogging {

  val mediator = DistributedPubSubExtension(context.system).mediator
  mediator ! DistributedPubSubMediator.Subscribe(Master.ResultsTopic, self)

  def receive = {
    case _: DistributedPubSubMediator.SubscribeAck =>
    case WorkResult(workId, result) =>
      log.info("Consumed result: {}", result)
  }

}
