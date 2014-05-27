package org.opendaylight.controller.datastore.clustered.actor

import akka.persistence._
import scala.collection.immutable.TreeMap


/**
 * Represents a shard in the cluster
 * Utilize a TreeMap internally to capture the state
 * @author: syedbahm
 *          Date: 5/15/14
 */

class TreeShard extends Processor {
  override def processorId = "tree-shard"

  var tree = TreeMap.empty[String, Any]
  var persistStart = 0L
  var snapshotStart = 0L


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

    case "persist-start" => {
      persistStart = System.currentTimeMillis();
      println("Start persisting of TreeMap  @" + persistStart)

    }


    //A family is added for each persistent message  received
    case Persistent(s, snr) => {
      if (!recoveryRunning) {

        val l: TreeMap[String, Any] = s match {
          case x: TreeMap[String, Any] => x // this extracts the value in a as an Int
          case _ => {
            println("Error: wasn't a map"); null
          }
        }
        tree = tree ++ l

        if (tree.size % 10001 == 0) {
          println("Taking snapshot of the state when the size of TreeMap is " + tree.size)
          saveSnapshot(tree)
        }

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

    case SnapshotOffer(_, s: TreeMap[String, Any]) => {
      println("Snapshot Offer occurred @" + System.currentTimeMillis());
      tree = s
      println("size of the snapshot offered tree =" + tree.size);
    }

    case "output" => {
      println("current tree size  = " + tree.size + " @" + System.currentTimeMillis)
      var x = 0
      while (x < tree.size) {

        if (x % 1000 == 0) {
          println("contents of tree at location" + x + " " + tree.toList(x));
        }
        x = x + 1000
      }
    }

    case "persist-done" => {
      println("Persisting " + tree.size + " families took (in ms) " + (System.currentTimeMillis() - persistStart));
      self ! "output"
      //println("current tree size  = " + tree.getFamilies.size)
    }
    case "snapshot" => {
      //explicit done
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
