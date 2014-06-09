package org.opendaylight.controller.datastore.clustered.actor

import akka.persistence._
import scala.collection.immutable.TreeMap
import scala.collection.mutable.HashMap
import org.opendaylight.controller.datastore.clustered.persistence.model.Families
import akka.persistence.SaveSnapshotFailure
import akka.persistence.SaveSnapshotSuccess

/**
 *
 * @author: syedbahm
 *          Date: 5/20/14
 *
 *
 *
 */
class ShardReplica extends View {

  //150k entries
  var tree = new Families()
  var persistStart = 0L
  var snapshotStart = 0L

  override def processorId = "families-shard"

  override def viewId = "families-shard-replica"

  //prestart called when recovery is completed - unstash the messages stashed
  override def preStart: Unit = {
    super.preStart()
    self ! "UNHOLD"
  }

  var initializing = true;


  def recoveryCompleted: Unit = {
    //here we will be deleting all the older snapshots?
    println("recovery completed message @ (in ms) " + System.currentTimeMillis())
  }


  def deleteOlderSnapshots: Unit = {
    // deleting older than 2 minutes snapshots
    deleteSnapshots(SnapshotSelectionCriteria(
      maxSequenceNr = scala.Long.MaxValue,
      maxTimestamp = org.joda.time.DateTime.now().minusMinutes(2).getMillis())) //we are deleting older snapshots.
  }

  def deleteAllOlderSnapshots: Unit = {
    deleteSnapshots(SnapshotSelectionCriteria(
      maxSequenceNr = scala.Long.MaxValue,
      maxTimestamp = org.joda.time.DateTime.now().getMillis())) //we are deleting older snapshots.
  }


  override def receive: Receive = {

    case "persist-start" => {
      persistStart = System.currentTimeMillis();
      println("Start persisting of families @" + persistStart)

    }


    //A family is added for each persistent message  received
    case Persistent(s, snr) => {

      val l: TreeMap[String, HashMap[String, Any]] = s match {
        case x: TreeMap[String, HashMap[String, Any]] => x // this extracts the value in a as an Int
        case _ => {
          println("ingnorable in view case :" + s);
          null
        }
      }
      tree.addAFamily(l)


    }

    case SaveSnapshotSuccess(metadata) => {
      println("Taking of snapshot of ShardReplica was successful, snapshot took in(ms)" + (System.currentTimeMillis - snapshotStart))
      deleteOlderSnapshots;
    }

    case SaveSnapshotFailure(metadata, reason) =>
      println("Failure of taking snapshot of ShardReplica" + reason)

    case SnapshotOffer(_, s: Families) => {
      println("Shard Replica snapshot Offer occurred @" + System.currentTimeMillis());
      tree = s
      println("size of the Shard Replica snapshot offered tree =" + tree.getFamilies.size);
    }

    case "output" => {
      println("ShardReplica current tree size  = " + tree.getFamilies.size + " @" + System.currentTimeMillis)
      var x = 1
      while (x < tree.getFamilies.size / 10) {

        println("3 grand child from second son of family " + x + "is " + tree.getFamilies.get("/family/" + x + "/child/" + "2/grandchild/3"));
        x = x + 1000
      }
    }

    case "persist-done" => {
      println("Persisting " + tree.getFamilies.size + " families took (in ms) " + (System.currentTimeMillis() - persistStart));
      //println("current tree size  = " + tree.getFamilies.size)
    }
    case "snapshot" => {
      snapshotStart = System.currentTimeMillis();
      println("snapshot: started at " + snapshotStart);

      saveSnapshot(tree)
    }

    case "deleteSnapshots" => {
      deleteAllOlderSnapshots
    }
    case _ =>
      println("ShardReplica: ignoring message ")
  }
}
