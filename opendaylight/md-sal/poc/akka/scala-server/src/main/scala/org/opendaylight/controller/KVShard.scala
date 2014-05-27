package org.opendaylight.controller

import akka.actor.{Props, ActorRef, ActorLogging}
import akka.persistence._
import akka.persistence.SnapshotOffer
import akka.persistence.SaveSnapshotFailure
import akka.persistence.SaveSnapshotSuccess

class KVShard(val memberName : String,
              val shardName : String,
              val members : List[(String, Int)],
              val clusteringServiceActor : ActorRef) extends Processor with ActorLogging {
  val data : scala.collection.mutable.Map[String, String] = scala.collection.mutable.Map();

  val replicas = members.filter(_._1 != memberName);
  val replicaActors = replicas.map( (x : (String, Int)) => {
    val actorRef = context.actorOf(Props(new ShardReplicator(x._1, shardName, clusteringServiceActor)), "replicator@" + x._1);
    log.info("Created replicator : " + actorRef.toString)
    actorRef
  })

  override def receive: Receive = {
    case Persistent(payload, sequenceNumber) => {

      payload match {
        case KeyValue(k,v) => {
          updateData(k,v, sequenceNumber)

          replicaActors.foreach({
            _ ! Persistent(payload)
          })

        }

        case Replicated(payload) => payload match {
          case KeyValue(k,v) => {
            updateData(k,v, sequenceNumber)
          }
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

  def updateData(k : String, v : String, sequenceNumber : Long) {
    log.info("Adding entry : " + k + ":" + v + " for sequence number : " + sequenceNumber);
    data(k) = v;

    if (sequenceNumber % 1000 == 0) {
      saveSnapshot(data);
    }
  }
}
