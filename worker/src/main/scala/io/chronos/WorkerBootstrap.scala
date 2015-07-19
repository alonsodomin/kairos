package io.chronos

import java.nio.file.Paths

import akka.actor.{ActorSystem, AddressFromURIString, RootActorPath}
import akka.contrib.pattern.ClusterClient
import akka.japi.Util._
import com.typesafe.config.ConfigFactory
import io.chronos.resolver.IvyJobModuleResolver
import io.chronos.worker.{JobExecutor, Worker}
import org.codehaus.plexus.classworlds.ClassWorld

/**
 * Created by domingueza on 09/07/15.
 */
object WorkerBootstrap extends App {

  val defaultPort = 0

  val conf = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + defaultPort)
    .withFallback(ConfigFactory.load("worker"))
  val system = ActorSystem("WorkerSystem", conf)
  val initialContacts = immutableSeq(conf.getStringList("contact-points")).map {
    case AddressFromURIString(addr) => system.actorSelection(RootActorPath(addr) / "user" / "receptionist")
  }.toSet

  val classWorld = new ClassWorld()

  val workDir = Paths.get(conf.getString("ivy.workDir"))
  val moduleResolver = new IvyJobModuleResolver(workDir)

  val clusterClient = system.actorOf(ClusterClient.props(initialContacts), "clusterClient")
  system.actorOf(Worker.props(clusterClient, JobExecutor.props(classWorld, moduleResolver)), "worker")

}
