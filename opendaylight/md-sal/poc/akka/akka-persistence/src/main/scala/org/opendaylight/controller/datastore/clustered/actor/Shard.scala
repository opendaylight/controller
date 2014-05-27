package org.opendaylight.controller.datastore.clustered.actor

import akka.persistence._
import akka.persistence.SaveSnapshotSuccess
import akka.persistence.SaveSnapshotFailure
import org.opendaylight.controller.datastore.clustered.persistence.model.Families
import scala.collection.mutable.HashMap
import scala.collection.immutable.TreeMap


/**
 * Represents a shard in the cluster
 * Currently uses the Family Model as the shard dat.
 * @author: syedbahm
 *          Date: 5/15/14
 */

class Shard extends Processor {
  override def processorId = "families-shard"

  //150k entries
  var tree = new Families()
  var persistStart = 0L
  var snapshotStart = 0L


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

  def receive: Receive = {
    case "UNHOLD" =>
      recoveryCompleted
      unstashAll
      self ! "output"
    case "persist-start" => {
      persistStart = System.currentTimeMillis();
      println("Start persisting of families @" + persistStart)

    }


    //A family is added for each persistent message  received
    case Persistent(s, snr) => {
      if (!recoveryRunning) {

        val l: TreeMap[String, HashMap[String, Any]] = s match {
          case x: TreeMap[String, HashMap[String, Any]] => x // this extracts the value in a as an Int
          case _ => {
            println("Error: wasn't a map"); null
          }
        }
        tree.addAFamily(l)

      } else {
        stash;
      }
    }

    case SaveSnapshotSuccess(metadata) => {
      println("Taking of snapshot was successful, snapshot took in(ms)" + (System.currentTimeMillis - snapshotStart))
      deleteOlderSnapshots;
    }

    case SaveSnapshotFailure(metadata, reason) =>
      println("Failure of taking snapshot" + reason)

    case SnapshotOffer(_, s: Families) => {
      println("Snapshot Offer occurred @" + System.currentTimeMillis());
      tree = s
      println("size of the snapshot offered tree =" + tree.getFamilies.size);
    }

    case "output" => {
      println("current tree size  = " + tree.getFamilies.size + " @" + System.currentTimeMillis)
      var x = 1
      while (x < tree.getFamilies.size / 10) {

        println("3 grand child from second son of family " + x + "is " + tree.getFamilies.get("/family/" + x + "/child/" + "2/grandchild/3"));
        x = x + x / 10
      }
    }

    case "persist-done" => {
      println("Persisting " + tree.getFamilies.size + " families took (in ms) " + (System.currentTimeMillis() - persistStart));
      //println("current tree size  = " + tree.getFamilies.size)
    }
    case "snapshot" => {
      if (!recoveryRunning) {
        snapshotStart = System.currentTimeMillis();
        println("snapshot: started at " + snapshotStart);

        saveSnapshot(tree)
      }
    }

    //here we will remove all messages from journal
    case "flush" => {

      val start = System.currentTimeMillis()
      deleteMessages(scala.Long.MaxValue, true)
      val end = System.currentTimeMillis();
      println("emptying the journal took (in ms)" + (end - start));

    }
    case "deleteSnapshots" => {

      deleteAllOlderSnapshots

    }
    case _ =>
      if (recoveryRunning) {
        stash()
      } else {
        println("Error Unhandled message ")
      }

  }
}
