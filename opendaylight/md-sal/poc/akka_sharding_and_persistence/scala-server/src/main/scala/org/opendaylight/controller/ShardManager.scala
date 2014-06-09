package org.opendaylight.controller

import akka.actor._
import akka.util.Timeout
import scala.concurrent.duration._
import akka.pattern.ask
import scala.concurrent.ExecutionContext.Implicits.global
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.cluster.ClusterEvent.MemberUp
import akka.cluster.ClusterEvent.UnreachableMember
import scala.collection.mutable

/** ShardManager is an actor which is responsible for creating and supervising all Shard actors that are local to a member */
class ShardManager(val clusteringServiceActor : ActorRef) extends Actor with ActorLogging{

  val cluster = Cluster(context.system)
  val memberNameToAddress = scala.collection.mutable.Map[String, Address]()
  val moduleNameToMembers = scala.collection.mutable.Map[String, List[(String, Int)]]()
  val shardNameToMembers = scala.collection.mutable.Map[String, List[(String, Int)]]()

  val memberName = cluster.getSelfRoles.iterator().hasNext match {
    case true => { cluster.getSelfRoles.iterator().next() }
    case _ => { null }
  };

  val localShards = memberName match {
    case null => { null }
    case _ => {ConfigurationUtils.shardsByMemberName(memberName)}
  }

  implicit val timeout = Timeout(5 seconds) // needed for `?` below

  localShards.foreach((t : (String, String)) => {
    val (moduleName, shardName) = t
    val members = ConfigurationUtils.membersByShardName(moduleName, shardName)
    log.info(s"Creating KVShard actor = ${moduleName}-${shardName}");
    context.actorOf(Props(new KVShard(memberName, moduleName, shardName, members, clusteringServiceActor)), s"${moduleName}-${shardName}");
  })

  // subscribe to cluster changes, re-subscribe when restart
  override def preStart(): Unit = {

    //#subscribe
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent], classOf[UnreachableMember])
    //#subscribe
  }
  override def postStop(): Unit = cluster.unsubscribe(self)

  override def receive: Receive = {
    case memberUp @ MemberUp(member) => {

      log.info("Member is Up: {}", member.address)

      member.roles.foreach((memberName : String)  => {
        memberNameToAddress(memberName) = member.address
      })


    }
    case memberUnreachable @ UnreachableMember(member) => {
      log.info("Member detected as unreachable: {}", member)
    }

    case memberRemoved @ MemberRemoved(member, previousStatus) => {
      log.info("Member is Removed: {} after {}",
        member.address, previousStatus)

      member.roles.foreach((memberName : String)  => {
        memberNameToAddress.remove(memberName);
      })

    }

    case ShardAddressByModuleName(moduleName) => {
      val address = moduleAddress(moduleName)
      if(address != null){
        sender ! ShardFound(moduleName, ClusteringUtils.shardAddress(address.toString, moduleName, ConfigurationUtils.firstShardByModuleName(moduleName)))
      } else {
        sender ! ShardNotFound(moduleName)
      }
    }

    case ShardAddressByModuleNameAndIdentifier(moduleName, identifier) => {
      val shardingStrategy = ShardingStrategyManager.findShardingStrategy(moduleName);
      log.info("Using sharding strategy" + shardingStrategy)
      val shardName = shardingStrategy.findShard(identifier);

      val address = shardAddress(moduleName, shardName)
      if(address != null){
        sender ! ShardFound(moduleName, ClusteringUtils.shardAddress(address.toString, moduleName, ConfigurationUtils.firstShardByModuleName(moduleName)))
      } else {
        sender ! ShardNotFound(moduleName)
      }
    }


    case ShardsForMember => {
      sender ! Shards(localShards, memberName)
    }

    case _: MemberEvent => // ignoree

  }

  def shardAddress(moduleName : String , shardName : String ) : Address = {
    val members = shardNameToMembers.getOrElse(s"${shardName}-${moduleName}", ConfigurationUtils.membersByShardName(moduleName, shardName))

    shardNameToMembers(s"${shardName}-${moduleName}") = members;

    members.foreach((f: (String, Int)) => {
      val address = memberNameToAddress.getOrElse(f._1, null)
      if (address != null) {
        return address
      }
    })

    return null

  }

  def moduleAddress(moduleName : String) : Address = {
    val members = moduleNameToMembers.getOrElse(moduleName, ConfigurationUtils.membersByModuleName(moduleName))

    moduleNameToMembers(moduleName) = members;

    members.foreach((f: (String, Int)) => {
      val address = memberNameToAddress.getOrElse(f._1, null)
      if (address != null) {
        return address
      }
    })

    return null
  }
}
