package org.opendaylight.controller

import akka.cluster.ClusterEvent._
import akka.cluster.ClusterEvent.MemberUp
import akka.cluster.ClusterEvent.UnreachableMember
import akka.cluster.Cluster
import akka.actor._
import scala.collection.{mutable, JavaConversions}
import com.typesafe.config.{ConfigValue, ConfigObject, Config}
import akka.cluster.ClusterEvent.MemberRemoved
import akka.cluster.ClusterEvent.MemberUp
import akka.cluster.ClusterEvent.UnreachableMember


class ClusteringService extends Actor with ActorLogging{

  val cluster = Cluster(context.system)
  val memberNameToAddress = scala.collection.mutable.Map[String, Address]()

  val memberName = cluster.getSelfRoles.iterator().hasNext match {
    case true => { cluster.getSelfRoles.iterator().next() }
    case _ => { null }
  };

  val localShards = memberName match {
    case null => { null }
    case _ => {ConfigurationUtils.shardsByMemberName(memberName)}
  }


  val memberNameToListenerActors = mutable.Map[String, mutable.Set[ActorPath]]();


  // subscribe to cluster changes, re-subscribe when restart
  override def preStart(): Unit = {

    //#subscribe
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent], classOf[UnreachableMember])
    //#subscribe
  }
  override def postStop(): Unit = cluster.unsubscribe(self)

  def receive = {
    case memberUp @ MemberUp(member) => {

      log.info("Member is Up: {}", member.address)

      member.roles.foreach((memberName : String)  => {
        memberNameToAddress(memberName) = member.address

        val actors = memberNameToListenerActors.getOrElse(memberName, null);

        if( actors != null ) {
          actors.foreach((path : ActorPath) => {
            val actor : ActorSelection = context.system.actorSelection(path)
            log.info("Notifying actor " + actor + " of MemberUp")
            actor forward memberUp
          })
        }

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

        val actorPaths = memberNameToListenerActors.getOrElse(memberName, Set[ActorPath]())
        val actors = actorPaths.map( (path : ActorPath) => { context.system.actorSelection(path) }  )
        actors.foreach((actor : ActorSelection) => { actor forward memberRemoved })

      })

    }

    case RegisterListener(actorPath, memberName) => {
      val actor : ActorSelection = context.system.actorSelection(actorPath)

      log.info("RegisteringListener for member " + memberName + " listener = " + actor)

      var actors = memberNameToListenerActors.getOrElse(memberName, mutable.Set[ActorPath]());

      actors += actorPath

      memberNameToListenerActors(memberName) = actors

      log.info("Registered listeners : " + actors)

      val address = memberNameToAddress.getOrElse(memberName, None)
      if(address != None){
        actor ! MemberAddress(address.toString)
      }

    }

    case MemberAddressByName(memberName) => {
      val address = memberNameToAddress.getOrElse(memberName, None)
      if(address != None){
        sender ! MemberAddress(address.toString)
      }

    }


    case _: MemberEvent => // ignore
  }

}
