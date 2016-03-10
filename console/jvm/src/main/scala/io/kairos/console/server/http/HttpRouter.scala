package io.kairos.console.server.http

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route, ValidationRejection}
import akka.stream.ActorMaterializer
import de.heikoseeberger.akkahttpupickle.UpickleSupport
import de.heikoseeberger.akkasse.EventStreamMarshalling
import io.kairos.JobSpec
import io.kairos.console.server.ServerFacade
import io.kairos.id.JobId
import io.kairos.serialization._

trait HttpRouter extends UpickleSupport with AuthDirectives with EventStreamMarshalling {
  this: ServerFacade =>

  import StatusCodes._

  private[this] def registryApi(implicit system: ActorSystem, materializer: ActorMaterializer): Route =
    pathPrefix("jobs") {
      pathEnd {
        get {
          complete(registeredJobs)
        } ~ post {
          entity(as[JobSpec]) { jobSpec =>
            complete(registerJob(jobSpec))
          }
        }
      } ~ path(JavaUUID) { jobId =>
        get {
          complete(fetchJob(JobId(jobId)))
        }
      }
    }

  private[this] def defineApi(implicit system: ActorSystem, materializer: ActorMaterializer): Route =
    path("login") {
      post { authenticateRequest }
    } ~ authorizeRequest {
      path("logout") {
        post {
          invalidateAuth {
            complete(OK)
          }
        }
      } ~ pathPrefix("cluster") {
        path("events") {
          get {
            import system.dispatcher
            complete(events)
          }
        } ~ path("info") {
          get {
            complete(clusterDetails)
          }
        }
      } ~ pathPrefix("registry") {
        registryApi
      }
    }

  private[this] def staticResources: Route = get {
    pathSingleSlash {
      getFromResource("kairos/index.html")
    } ~ getFromResourceDirectory("kairos")
  }

  private[this] def exceptionHandler(log: LoggingAdapter) = ExceptionHandler {
    case exception =>
      extractUri { uri =>
        log.error(exception, s"Request to URI '$uri' threw exception.")
        complete(HttpResponse(InternalServerError, entity = exception.getMessage))
      }
  }

  private[this] def rejectionHandler(log: LoggingAdapter) = RejectionHandler.newBuilder().
    handle { case ValidationRejection(msg, cause) =>
        log.error(s"$msg - reason: $cause")
        complete(HttpResponse(Unauthorized, entity = msg))
    } result()

  def router(implicit system: ActorSystem, materializer: ActorMaterializer): Route =
    logRequest("HTTPRequest") {
      logResult("HTTPResponse") {
        handleExceptions(exceptionHandler(system.log)) {
          handleRejections(rejectionHandler(system.log)) {
            pathPrefix("api") {
              defineApi
            } ~ staticResources
          }
        }
      }
    }

}
