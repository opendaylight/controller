package org.opendaylight.controller

import akka.actor.{ActorLogging, ActorRef}
import akka.persistence.{Persistent, Processor}
import scala.collection.mutable


class ShardReplicator(val memberName : String,
                      val shardName : String,
                      val clusteringServiceActor : ActorRef) extends Processor with ActorLogging{

  val replicationQueue = mutable.Queue[(Any,Long)]();
  var replicaAddress : String = null;

  clusteringServiceActor ! RegisterListener(self, memberName)

  override def receive: Receive = {
    case Persistent(payload, sequenceNumber) => {
      replicationQueue.enqueue((Persistent(Replicated(payload)), sequenceNumber))

      if(replicaAddress != null){
        flushQueue
      }
    }

    case MemberAvailable(address: String) => {
      log.info("Member is available : " + address)

      replicaAddress = ClusteringUtils.shardAddress(address, context.parent.path.name);

      flushQueue
    }

    case MemberUnAvailable(address: String) => {
      log.info("Member is unavailable : " + address)

      replicaAddress = null;
    }

  }

  private def flushQueue {
    // flush the queue
    val shard = context.system.actorSelection(replicaAddress);
    while(replicationQueue.size > 0){
      val (message, sequenceNumber) = replicationQueue.dequeue();
      deleteMessage(sequenceNumber);
      shard ! message
    }
  }
}
