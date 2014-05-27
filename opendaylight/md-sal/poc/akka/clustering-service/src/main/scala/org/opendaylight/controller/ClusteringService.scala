package org.opendaylight.controller

import akka.cluster.ClusterEvent._
import akka.cluster.ClusterEvent.MemberUp
import akka.cluster.ClusterEvent.UnreachableMember
import akka.cluster.Cluster
import akka.actor.{ActorRef, Address, Actor, ActorLogging}
import scala.collection.{mutable, JavaConversions}
import com.typesafe.config.{ConfigValue, ConfigObject, Config}


class ClusteringService extends Actor with ActorLogging{

  val cluster = Cluster(context.system)
  val memberNameToAddress = scala.collection.mutable.Map[String, Address]()
  val moduleNameToMembers = scala.collection.mutable.Map[String, List[(String, Int)]]()

  val memberName = cluster.getSelfRoles.iterator().hasNext match {
    case true => { cluster.getSelfRoles.iterator().next() }
    case _ => { null }
  };

  val localShards = memberName match {
    case null => { null }
    case _ => {ConfigurationUtils.shardsByMemberName(memberName)}
  }


  val memberNameToListenerActors = mutable.Map[String, mutable.Set[ActorRef]]();


  // subscribe to cluster changes, re-subscribe when restart
  override def preStart(): Unit = {

    //#subscribe
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent], classOf[UnreachableMember])
    //#subscribe
  }
  override def postStop(): Unit = cluster.unsubscribe(self)

  def receive = {
    case MemberUp(member) => {

      log.info("Member is Up: {}", member.address)

      member.roles.foreach((memberName : String)  => {
        memberNameToAddress(memberName) = member.address

        val actors = memberNameToListenerActors.getOrElse(memberName, null);

        log.info("Notifying actors " + actors)
        if( actors != null ) {
          actors.foreach((actor : ActorRef) => {
            log.info("Notifying actor " + actor + " of MemberUp")
            actor ! MemberAvailable(member.address.toString)
          })
        }

      })


    }
    case UnreachableMember(member) => {
      log.info("Member detected as unreachable: {}", member)
    }

    case MemberRemoved(member, previousStatus) => {
      log.info("Member is Removed: {} after {}",
        member.address, previousStatus)

      member.roles.foreach((memberName : String)  => {
        memberNameToAddress.remove(memberName);

        memberNameToListenerActors.getOrElse(memberName, Set[ActorRef]()).foreach((actor : ActorRef) => {
          actor ! MemberUnAvailable(member.address.toString)
        })

      })


    }

    case ShardAddressByModuleName(moduleName) => {
      val address = moduleAddress(moduleName)
      if(address != null){
        sender ! ShardFound(moduleName, ClusteringUtils.shardAddress(address.toString, ConfigurationUtils.shardByModuleName(moduleName)))
      } else {
        sender ! ShardNotFound(moduleName)
      }
    }

    case ShardsForMember => {
      sender ! Shards(localShards, memberName)
    }

    case RegisterListener(actor, memberName) => {
      log.info("RegisteringListener for member " + memberName + " listener = " + actor)

      var actors = memberNameToListenerActors.getOrElse(memberName, mutable.Set[ActorRef]());

      actors += actor

      memberNameToListenerActors(memberName) = actors

      log.info("Registered listeners : " + actors)

      val address = memberNameToAddress.getOrElse(memberName, null)
      if(address != null){
        actor ! MemberAvailable(address.toString)
      }
    }

    case _: MemberEvent => // ignore
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
