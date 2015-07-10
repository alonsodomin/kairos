package modules

import akka.actor.{AddressFromURIString, RootActorPath}
import akka.contrib.pattern.ClusterClient
import akka.japi.Util._
import com.google.inject.AbstractModule
import com.typesafe.config.ConfigFactory
import play.api.Play.current
import play.api.libs.concurrent.Akka

/**
 * Created by domingueza on 09/07/15.
 */
class ConsoleModule extends AbstractModule {

  override def configure(): Unit = {
    val chronosConf = ConfigFactory.load("chronos")

    val initialContacts = immutableSeq(chronosConf.getStringList("chronos.seed-nodes")).map {
      case AddressFromURIString(addr) => Akka.system.actorSelection(RootActorPath(addr) / "user" / "receptionist")
    }.toSet

    val chronosClient = Akka.system.actorOf(ClusterClient.props(initialContacts), "chronosClient")



  }

}