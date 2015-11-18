package io.kairos.console.server

import akka.actor.ActorSystem
import akka.pattern._
import akka.stream.ActorMaterializer
import akka.util.Timeout
import io.kairos.console.server.core.UserAuthenticator
import io.kairos.console.server.http.HttpRouter
import io.kairos.console.server.security.AuthInfo

import scala.concurrent.Future
import scala.concurrent.duration._

/**
 * Created by alonsodomin on 14/10/2015.
 */
object Server {

  final val DefaultSessionTimeout: FiniteDuration = 30 minutes

}

class Server(implicit system: ActorSystem, materializer: ActorMaterializer)
  extends HttpRouter with ServerFacade {
  import Server._
  import UserAuthenticator._

  val userAuth = system.actorOf(UserAuthenticator.props(DefaultSessionTimeout), "authenticator")

  def authenticate(username: String, password: Array[Char]): Future[Option[AuthInfo]] = {
    import system.dispatcher
    
    implicit val timeout = Timeout(5 seconds)
    userAuth ? Authenticate(username, password) map {
      case AuthenticationSuccess(authInfo) => Some(authInfo)
      case AuthenticationFailed => None
    }
  }

}