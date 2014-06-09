package org.opendaylight.controller

import akka.actor.{Props, ActorRef, ActorLogging}
import akka.persistence._
import akka.persistence.SnapshotOffer
import akka.persistence.SaveSnapshotFailure
import akka.persistence.SaveSnapshotSuccess

/** KVShard represents a Shard which stores it's data in a Key-Value store
 *
 * @param memberName The name of the cluster member to which this Shard belongs
 * @param moduleName The name of the module
 * @param shardName The name of the Shard
 * @param members The list of cluster members which run a replica of this Shard including the current member
 * @param clusteringServiceActor A reference to the Clustering Service actor
 */
class KVShard(val memberName : String,
              val moduleName : String,
              val shardName : String,
              val members : List[(String, Int)],
              val clusteringServiceActor : ActorRef) extends Processor with ActorLogging {

  /** The state of this Shard */
  val data : scala.collection.mutable.Map[String, String] = scala.collection.mutable.Map();

  /** The list of cluster members which run a replica of this Shard except the current member */
  val replicas = members.filter(_._1 != memberName);

  /** The list of ShardReplicators which represent each replica Shard */ 
  val shardReplicators = replicas.map( (x : (String, Int)) => {
    val actorRef = context.actorOf(Props(new ShardReplicator(x._1, moduleName, shardName, clusteringServiceActor)), "replicator@" + x._1);
    log.info("Created replicator : " + actorRef.toString)
    actorRef
  })

  override def receive: Receive = {
    case p @ Persistent(payload, sequenceNumber) => {

      payload match {
        // A KeyValue payload indicates that this is a message from a consumer
        case KeyValue(k,v) => {
          updateData(k,v, sequenceNumber)

          // Send the Persistent message to the ShardReplicators for replicating
          if(recoveryFinished){
            shardReplicators.foreach(_ ! Persistent(payload))
          }

          sender ! PersistedToShard
        }

        /** A Replicated payload indicates that this is a message from a replicator */
        case Replicated(payload) => payload match {
          case KeyValue(k,v) => {
            updateData(k,v, sequenceNumber, true)
          }

          // Confirm that the replicated message was received by this Shard
          val cp = p.asInstanceOf[ConfirmablePersistent];
          cp.confirm();
        }

      }
    }

    case SaveSnapshotSuccess(metadata)         => log.info("Snapshot was successfully saved");
    case SaveSnapshotFailure(metadata, reason) => log.info("Saving snapshot failed");
    case SnapshotOffer(_, snapShot:scala.collection.mutable.Map[String, String]) => {
      data ++= snapShot;
    }

    case EchoRequest(message) => {
      sender ! EchoResponse(message + " done")
    }
  }

  def updateData(k : String, v : String, sequenceNumber : Long, replicated : Boolean = false) {
    if(replicated) {
      log.info("Replicated entry : " + k + ":" + v + " for sequence number : " + sequenceNumber);
    } else {
      log.info("Adding entry : " + k + ":" + v + " for sequence number : " + sequenceNumber);
    }

    data(k) = v;

    if (sequenceNumber % 1000 == 0) {
      saveSnapshot(data);
    }
  }
}
