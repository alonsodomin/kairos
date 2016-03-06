package io.kairos.cluster.boot

import java.net.InetAddress
import java.util.{HashMap => JHashMap}

import com.typesafe.config.{Config, ConfigFactory}
import io.kairos.cluster.KairosClusterSettings

import scala.collection.JavaConversions._

/**
 * Created by aalonsodominguez on 03/10/2015.
 */
object Options {

  final val AkkaRemoteNettyHost = "akka.remote.netty.tcp.hostname"
  final val AkkaRemoteNettyPort = "akka.remote.netty.tcp.port"
  final val AkkaRemoteNettyBindHost = "akka.remote.netty.tcp.bind-hostname"
  final val AkkaRemoteNettyBindPort = "akka.remote.netty.tcp.bind-port"

  final val AkkaClusterSeedNodes = "akka.cluster.seed-nodes"

  final val CassandraJournalContactPoints = "cassandra-journal.contact-points"
  final val CassandraSnapshotContactPoints = "cassandra-snapshot-store.contact-points"

  final val KairosHttpBindPort = "kairos.http.bind-port"

  private final val HostAndPort = """(.+?):(\d+)""".r

}

case class Options(bindAddress: Option[String] = None,
    port: Int = KairosClusterSettings.DefaultTcpPort,
    httpPort: Option[Int] = None,
    seed: Boolean = false,
    seedNodes: Seq[String] = Seq(),
    cassandraSeedNodes: Seq[String] = Seq()) {
  import Options._

  def toConfig: Config = {
    val valueMap = new JHashMap[String, Object]()

    val (bindHost, bindPort) = bindAddress.map { addr =>
      val HostAndPort(h, p) = addr
      (h, p.toInt)
    } getOrElse((KairosClusterSettings.DefaultTcpInterface, port))

    valueMap.put(AkkaRemoteNettyHost, bindHost)
    valueMap.put(AkkaRemoteNettyPort, Int.box(bindPort))

    if (bindAddress.isDefined) {
      val localAddress = InetAddress.getLocalHost.getHostAddress
      valueMap.put(AkkaRemoteNettyBindHost, localAddress)
      valueMap.put(AkkaRemoteNettyBindPort, Int.box(port))
    }

    httpPort.foreach(p => valueMap.put(KairosHttpBindPort, Int.box(p)))

    val clusterSeedNodes: Seq[String] = {
      if (seed || seedNodes.isEmpty)
        List(s"akka.tcp://KairosClusterSystem@$bindHost:$bindPort")
      else
        List.empty[String]
    } ::: seedNodes.map({ node =>
      s"akka.tcp://KairosClusterSystem@$node"
    }).toList

    valueMap.put(AkkaClusterSeedNodes, seqAsJavaList(clusterSeedNodes))

    if (cassandraSeedNodes.nonEmpty) {
      valueMap.put(CassandraJournalContactPoints, seqAsJavaList(cassandraSeedNodes))
      valueMap.put(CassandraSnapshotContactPoints, seqAsJavaList(cassandraSeedNodes))
    }

    ConfigFactory.parseMap(valueMap)
  }

}