package io.quckoo.cluster.registry

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.Cluster
import akka.cluster.client.ClusterClientReceptionist
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.pattern._
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import io.quckoo.JobSpec
import io.quckoo.cluster.QuckooClusterSettings
import io.quckoo.cluster.core.QuckooJournal
import io.quckoo.id.JobId
import io.quckoo.protocol.registry._
import io.quckoo.resolver.Resolver
import io.quckoo.resolver.ivy.IvyResolve

/**
 * Created by aalonsodominguez on 24/08/15.
 */
object Registry {

  final val EventTag = "registry"

  def props(settings: QuckooClusterSettings) = Props(classOf[Registry], settings)

}

class Registry(settings: QuckooClusterSettings)
    extends Actor with ActorLogging with QuckooJournal {

  import Registry._

  ClusterClientReceptionist(context.system).registerService(self)

  final implicit val materializer = ActorMaterializer(
    ActorMaterializerSettings(context.system), "registry"
  )

  private val cluster = Cluster(context.system)
  private val resolver = context.actorOf(
    Resolver.props(IvyResolve(settings.ivyConfiguration)).withDispatcher("quckoo.resolver.dispatcher"),
    "resolver")
  private lazy val shardRegion = startShardRegion

  def actorSystem = context.system

  def receive: Receive = {
    case GetJobs =>
      import context.dispatcher
      readJournal.currentEventsByTag(EventTag, 0).
        filter(envelope => envelope.event match {
          case evt: RegistryJobEvent => true
          case _                     => false
        }).
        map(_.event.asInstanceOf[RegistryJobEvent]).
        runFold(Map.empty[JobId, JobSpec]) {
          case (map, event) => event match {
            case JobAccepted(jobId, jobSpec) =>
              map + (jobId -> jobSpec)
            case JobDisabled(jobId) if map.contains(jobId) =>
              map + (jobId -> map(jobId).copy(disabled = true))
            case JobEnabled(jobId) if map.contains(jobId) =>
              map + (jobId -> map(jobId).copy(disabled = false))
            case _ => map
          }
        } pipeTo sender()

    case msg: Any =>
      shardRegion.tell(msg, sender())
  }

  private def startShardRegion: ActorRef = if (cluster.selfRoles.contains("registry")) {
    log.info("Starting registry shards...")
    ClusterSharding(context.system).start(
      typeName        = RegistryShard.ShardName,
      entityProps     = RegistryShard.props(resolver),
      settings        = ClusterShardingSettings(context.system).withRole("registry"),
      extractEntityId = RegistryShard.idExtractor,
      extractShardId  = RegistryShard.shardResolver
    )
  } else {
    log.info("Starting registry proxy...")
    ClusterSharding(context.system).startProxy(
      typeName        = RegistryShard.ShardName,
      role            = None,
      extractEntityId = RegistryShard.idExtractor,
      extractShardId  = RegistryShard.shardResolver
    )
  }

}
