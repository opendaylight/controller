package org.opendaylight.controller

import akka.actor.{Address, Props, ActorLogging, ActorRef}
import akka.persistence._
import scala.collection.mutable
import akka.util.Timeout
import scala.concurrent.duration._
import akka.cluster.ClusterEvent.MemberUp

/** A ShardReplicator represents a remote replica.
 *
 * It stores messages for the remote replica and sends it to the remote replica using a PersistentChannel
 * The ShardReplicator is a Processor itself and does persist messages despite the fact that it uses a Persistent
 * Channel. This is to take care of the scenario where the member where the replica is running has not come up
 * yet. If the member has not joined the cluster yet then we cannot know it's remote address and so we cannot
 * construct an ActorRef for that remote member which is a requirement for using a Channel.
 *
 * @param memberName The name of the cluster member on which the replica runs
 * @param moduleName The name of the module
 * @param shardName The shard to which this replicator should send replication messages
 * @param clusteringServiceActor The clustering service actor
 */
class ShardReplicator(val memberName : String,
                      val moduleName : String,
                      val shardName : String,
                      val clusteringServiceActor : ActorRef) extends Processor with ActorLogging{

  val replicationQueue = mutable.Queue[(Persistent,Long)]();
  var replicaAddress : String = null;
  var replicaActor : ActorRef = null;
  val channel = context.actorOf(
    PersistentChannel.props(
      PersistentChannelSettings(
        redeliverInterval = 30 seconds,
        redeliverMax = 15,
        pendingConfirmationsMax = 10000,
        pendingConfirmationsMin = 2000,
        //If replyPersistent is set to true we will get the Persistent message back. We don't want this as long
        //as we're command sourced because then the Persistent message will get saved to the journal
        //we probably should be event sourced in that case
        replyPersistent = false
      )
    ),
    name = "replicationChannel"
  );

  clusteringServiceActor ! RegisterListener(self.path, memberName)

  override def receive: Receive = {
    case Persistent(payload, sequenceNumber) => {
      replicationQueue.enqueue((Persistent(Replicated(payload)), sequenceNumber))

      if(replicaActor != null){
        flushQueue(replicaActor)
      }
    }

    case MemberUp(member) => {
      replicate(member.address.toString)

    }

    case MemberAddress(memberAddress) => {
      replicate(memberAddress)
    }



    case PersistenceFailure => ???

  }

  private def replicate(memberAddress : String) {

    log.info("Member is available : " + memberAddress.toString)

    val replicaAddress = ClusteringUtils.shardAddress(memberAddress.toString, moduleName, shardName);

    // figure out where the replica is running and point to that actor
    val shard = context.system.actorSelection(replicaAddress);
    implicit val timeout = Timeout(5 seconds)
    val future = shard.resolveOne()
    import context.dispatcher
    future onSuccess {
      case actor @ (_: ActorRef) => {
        replicaActor = actor
        flushQueue(replicaActor)
      }
    }

  }

  private def flushQueue(actor : ActorRef) {
    while (replicationQueue.size > 0) {
      val (message, sequenceNumber) = replicationQueue.dequeue();
      deleteMessage(sequenceNumber);
      //shard ! message
      channel ! Deliver(message, actor.path)
    }
  }
}
