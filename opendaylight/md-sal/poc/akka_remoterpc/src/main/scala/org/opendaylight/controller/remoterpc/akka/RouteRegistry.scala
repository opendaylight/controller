package org.opendaylight.controller.remoterpc.akka

import akka.actor.{Actor, ActorLogging, Address}

import akka.cluster.{MemberStatus, Cluster}
import akka.cluster.ClusterEvent.{MemberRemoved, CurrentClusterState, MemberUp, MemberEvent}
import scala.concurrent.forkjoin.ThreadLocalRandom
import scala.collection.mutable
import org.opendaylight.controller.remoterpc.akka.RouteRegistry.{GossipTick, Bucket}

object RouteRegistry {

  case object GossipTick

  case class Status(registry: mutable.Map[Address, Bucket])

  case class RegisterRpc(rpc: String)

  final case class Bucket(
    version: Long,
    rpcs: List[String]
  )

  var registry: mutable.Map[Address, Bucket] = mutable.Map.empty[Address, Bucket]
}

/**
 * Maintains a registry of RPCs. This registry is replicated on all members of the cluster.
 * Registry is a map with following structure:
 *
 * Member Address - Bucket
 *
 * where Bucket contains bucket version and a list of RPCs
 *
 * Every member in the cluster maintains its own bucket.
 */
class RouteRegistry extends Actor with ActorLogging {


  val nextVersion = {
    var version = 0L
    () ⇒ {
      val current = System.currentTimeMillis
      version = if (current > version) current else version + 1
      version
    }
  }

  def put(key: Address, rpcList: List[String]): Unit = {
    val bucket: Bucket = RouteRegistry.registry.getOrElse(key, Bucket(0L, List.empty))
    val v = nextVersion()
    RouteRegistry.registry += (key -> bucket.copy(version = v, rpcs = rpcList))
  }

  def add(key: Address, rpc: String): Unit = {
    val bucket: Bucket = RouteRegistry.registry.getOrElse(key, Bucket(0L, List.empty))
    val v = nextVersion()
    RouteRegistry.registry += (key -> bucket.copy(version = v, rpcs = rpc +: bucket.rpcs))
  }

  /*
   Cluster related
   */

  import context.dispatcher
  import cluster.selfAddress
  import scala.concurrent.duration._

  val gossipTask = context.system.scheduler.schedule(5 seconds, 5 seconds, self, GossipTick)

  val cluster = Cluster(context.system)
  var nodes: Set[Address] = Set.empty

  override def preStart(): Unit = {
    super.preStart()
    require(!cluster.isTerminated, "Cluster node must not be terminated")
    cluster.subscribe(self, classOf[MemberEvent])

  }

  override def postStop(): Unit = {
    super.postStop()
    cluster unsubscribe self
    gossipTask.cancel()
  }

  def receive = {
    case RouteRegistry.RegisterRpc(rpc) =>
      add(selfAddress, rpc)
      println(RouteRegistry.registry)

    case GossipTick =>
      println("Got gossiptick ")
      val others: Vector[Address] = (nodes - selfAddress).toVector

      if (others.nonEmpty) {
        var randomIndex: Int = ThreadLocalRandom.current().nextInt(others.size);
        println("random index" + randomIndex)

        if (randomIndex < 0) randomIndex = randomIndex * (-1)

        val chosenNode: Address = others(randomIndex)

        if (chosenNode != null) {
          val path = self.path.toStringWithAddress(chosenNode)
          println("remote actor path " + path)
          context.actorSelection(path) ! RouteRegistry.Status(mutable.Map(selfAddress -> RouteRegistry.registry(selfAddress)))
        }
      }

    case RouteRegistry.Status(rpcs) =>
      val filteredList = rpcs.filterKeys( _ != selfAddress )
      filteredList.map( RouteRegistry.registry += _)
      println("updated registry" + RouteRegistry.registry)

    case state: CurrentClusterState ⇒
      nodes = state.members.collect {
        case m if m.status != MemberStatus.Joining ⇒ m.address
      }

    case MemberUp(m) ⇒
      println("Memberup " + m.address)
      nodes += m.address

    case MemberRemoved(m, _) ⇒
      if (m.address == selfAddress)
        context stop self
      else {
        nodes -= m.address
        RouteRegistry.registry -= m.address
      }

    case _ =>
    //ignore
  }
}
