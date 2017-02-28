/*
 * Copyright 2016 Antonio Alonso Dominguez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quckoo.cluster

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.pattern._
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, OverflowStrategy}
import akka.stream.scaladsl.Source
import akka.util.Timeout

import io.quckoo._
import io.quckoo.auth.Passport
import io.quckoo.cluster.config.ClusterSettings
import io.quckoo.cluster.core._
import io.quckoo.cluster.http.HttpRouter
import io.quckoo.cluster.journal.QuckooProductionJournal
import io.quckoo.net.QuckooState
import io.quckoo.protocol.registry._
import io.quckoo.protocol.scheduler._
import io.quckoo.protocol.cluster._
import io.quckoo.protocol.worker.WorkerEvent

import slogging._

import org.threeten.bp.Clock

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.concurrent.duration._

import scalaz._
import Scalaz._

/**
  * Created by alonsodomin on 13/12/2015.
  */
object QuckooFacade extends LazyLogging {

  final val DefaultTimeout: FiniteDuration = 2500 millis
  final val DefaultBufferSize = 100

  def start(settings: ClusterSettings)(implicit system: ActorSystem, clock: Clock): Future[Unit] = {
    def startHttpListener(facade: QuckooFacade)(implicit ec: ExecutionContext) = {
      implicit val materializer = ActorMaterializer(ActorMaterializerSettings(system), "http")

      Http()
        .bindAndHandle(facade.router, settings.http.bindInterface, settings.http.bindPort)
        .map(_ =>
          logger.info(s"HTTP server started on ${settings.http.bindInterface}:${settings.http.bindPort}"))
    }

    val promise  = Promise[Unit]()
    val journal  = new QuckooProductionJournal
    val guardian = system.actorOf(QuckooGuardian.props(settings, journal, promise), "quckoo")

    import system.dispatcher
    (promise.future |@| startHttpListener(new QuckooFacade(guardian)))((_, _) => ())
  }

}

final class QuckooFacade(core: ActorRef)(implicit system: ActorSystem, clock: Clock)
    extends HttpRouter with QuckooServer with LazyLogging {

  import QuckooFacade._

  implicit val materializer = ActorMaterializer()

  def cancelPlan(planId: PlanId)(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[ExecutionPlanNotFound \/ ExecutionPlanCancelled] = {
    implicit val to = Timeout(timeout)
    (core ? CancelExecutionPlan(planId)).map {
      case msg: ExecutionPlanNotFound  => msg.left[ExecutionPlanCancelled]
      case msg: ExecutionPlanCancelled => msg.right[ExecutionPlanNotFound]
    }
  }

  def executionPlans(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[Map[PlanId, ExecutionPlan]] = {
    val executionPlans = Source
      .actorRef[(PlanId, ExecutionPlan)](bufferSize = DefaultBufferSize, OverflowStrategy.fail)
      .mapMaterializedValue(upstream => core.tell(GetExecutionPlans, upstream))

    executionPlans.runFold(Map.empty[PlanId, ExecutionPlan])((map, pair) => map + pair)
  }

  def executionPlan(planId: PlanId)(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[Option[ExecutionPlan]] = {
    def internalRequest: Future[Option[ExecutionPlan]] = {
      implicit val to = Timeout(timeout)
      (core ? GetExecutionPlan(planId)).map {
        case ExecutionPlanNotFound(`planId`) => None
        case plan: ExecutionPlan             => Some(plan)
      }
    }

    /*implicit val sch = system.scheduler
    retry(internalRequest, 250 millis, 3)*/
    internalRequest
  }

  def executions(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[Map[TaskId, TaskExecution]] = {
    val tasks = Source
      .actorRef[(TaskId, TaskExecution)](bufferSize = DefaultBufferSize, OverflowStrategy.fail)
      .mapMaterializedValue(upstream => core.tell(GetTaskExecutions, upstream))
    tasks.runFold(Map.empty[TaskId, TaskExecution])((map, pair) => map + pair)
  }

  def execution(taskId: TaskId)(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[Option[TaskExecution]] = {
    implicit val to = Timeout(timeout)
    (core ? GetTaskExecution(taskId)) map {
      case task: TaskExecution             => Some(task)
      case TaskExecutionNotFound(`taskId`) => None
    }
  }

  def scheduleJob(schedule: ScheduleJob)(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[Fault \/ ExecutionPlanStarted] = {
    implicit val to = Timeout(timeout)
    (core ? schedule) map {
      case fault: Fault                  => fault.left[ExecutionPlanStarted]
      case started: ExecutionPlanStarted => started.right[Fault]
    }
  }

  def schedulerTopic: Source[SchedulerEvent, NotUsed] =
    Topic.source[SchedulerEvent]

  def masterTopic: Source[MasterEvent, NotUsed] =
    Topic.source[MasterEvent]

  def workerTopic: Source[WorkerEvent, NotUsed] =
    Topic.source[WorkerEvent]

  def enableJob(jobId: JobId)(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[JobNotFound \/ JobEnabled] = {
    implicit val to = Timeout(timeout)
    (core ? EnableJob(jobId)).map {
      case msg: JobNotFound => msg.left[JobEnabled]
      case msg: JobEnabled  => msg.right[JobNotFound]
    }
  }

  def disableJob(jobId: JobId)(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[JobNotFound \/ JobDisabled] = {
    implicit val to = Timeout(timeout)
    (core ? DisableJob(jobId)).map {
      case msg: JobNotFound => msg.left[JobDisabled]
      case msg: JobDisabled => msg.right[JobNotFound]
    }
  }

  def fetchJob(jobId: JobId)(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[Option[JobSpec]] = {
    implicit val to = Timeout(timeout)
    (core ? GetJob(jobId)).map {
      case JobNotFound(_) => None
      case spec: JobSpec  => Some(spec)
    }
  }

  def registerJob(jobSpec: JobSpec)(
      implicit ec: ExecutionContext,
      timeout: FiniteDuration,
      passport: Passport
  ): Future[ValidationNel[Fault, JobId]] = {
    import scalaz._
    import Scalaz._

    val validatedJobSpec = JobSpec.valid.async
      .run(jobSpec)
      .map(_.leftMap(ValidationFault).leftMap(_.asInstanceOf[Fault]))

    EitherT(validatedJobSpec.map(_.disjunction)).flatMapF { validJobSpec =>
      implicit val to = Timeout(timeout)
      logger.info(s"Registering job spec: $validJobSpec")

      (core ? RegisterJob(validJobSpec)) map {
        case JobAccepted(jobId, _) => jobId.right[Fault]
        case JobRejected(_, error) => error.left[JobId]
      }
    }.run.map(_.validationNel)
  }

  def fetchJobs(implicit ec: ExecutionContext,
                timeout: FiniteDuration,
                passport: Passport): Future[Map[JobId, JobSpec]] = {
    Source
      .actorRef[(JobId, JobSpec)](bufferSize = DefaultBufferSize, OverflowStrategy.fail)
      .mapMaterializedValue { upstream =>
        core.tell(GetJobs, upstream)
      }
      .runFold(Map.empty[JobId, JobSpec]) { (map, pair) =>
        map + pair
      }
  }

  def registryTopic: Source[RegistryEvent, NotUsed] =
    Topic.source[RegistryEvent]

  def clusterState(implicit ec: ExecutionContext,
                   timeout: FiniteDuration,
                   passport: Passport): Future[QuckooState] = {
    implicit val to = Timeout(timeout)
    (core ? GetClusterStatus).mapTo[QuckooState]
  }

}
