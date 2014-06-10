package org.opendaylight.controller.datastore.clustered.actor

import akka.persistence.{Persistent, Processor}
import org.opendaylight.controller.datastore.clustered.messages.{SimulateResume, SimulateRestart}
import org.opendaylight.controller.datastore.clustered.exception.{ShardRestartException, ShardResumeException}

import scala.collection.mutable.HashMap

import scala.collection.immutable.TreeMap
import org.opendaylight.controller.datastore.clustered.model.Families
import akka.actor.ActorLogging
import java.util.{TimerTask, Timer}

/**
 * Represents a shard in the cluster
 * Currently uses the Family Model as the shard dat.
 * @author: syedbahm
 *          Date: 5/15/14
 */

class FamilyShard extends Processor with ActorLogging{
  override def processorId = "families-shard"

  var tree = new Families()
  var persistStart = 0L
  var snapshotStart = 0L


  //prestart called when recovery is completed - unstash the messages stashed
  override def preStart: Unit = {
    super.preStart()
    println("familyShard:Starting familyShard instance hashcode # {}",this.hashCode())
    self ! "UNHOLD"
  }

  override def postStop() {
    println("familyShard:Stopping familyShard instance hashcode # {}", this.hashCode());
  }



  def recoveryCompleted: Unit = {
    //here we will be deleting all the older snapshots?
    println("FamilyShard:recovery completed message @ (in ms) " + System.currentTimeMillis())
  }

  def receive: Receive = {
    case "UNHOLD" =>
      recoveryCompleted
      unstashAll
      self ! "output"
      startTimer
    case "persist-start" => {
      persistStart = System.currentTimeMillis();
      println("FamilyShard:Start persisting of families @" + persistStart)

    }
    case SimulateRestart =>
      println("FamilyShard: simulate restart called")
      context.stop(self)

    case SimulateResume => {
      println("FamilyShard: simulate resume called")
      throw new ShardResumeException()
    }

    //A family is added for each persistent message  received
    case Persistent(s, snr) => {
      if (!recoveryRunning) {

        val l: TreeMap[String, HashMap[String, Any]] = s match {
          case x: TreeMap[String, HashMap[String, Any]] => x // this extracts the value in a as an Int
          case _ => {
            println("FamilyShard:Error: wasn't a map"); null
          }
        }
        tree.addAFamily(l)

      } else {
        stash;
      }
    }


    case "output" => {
      println("FamilyShard:current tree size  = " + tree.getFamilies.size + " @" + System.currentTimeMillis)
      var x = 0
      while (x <  tree.getFamilies.size) {

        if (x % 1000 == 0) {
          println("FamilyShard:contents of tree at location" + x + " " + tree.getFamilies.toList(x));
        }
        x = x + 1000
      }
    }


    case "persist-done" => {
      println("FamilyShard:Persisting " + tree.getFamilies.size + " families took (in ms) " + (System.currentTimeMillis() - persistStart));
      self ! "output"
    }


    //here we will remove all messages from journal
    case "flush" => {

      val start = System.currentTimeMillis()
      deleteMessages(scala.Long.MaxValue, true)
      val end = System.currentTimeMillis();
      println("FamilyShard:emptying the journal took (in ms)" + (end - start));

    }
    case _ =>
      if (recoveryRunning) {
        stash()
      } else {
        println("FamilyShard:Error Unhandled message ")
      }

  }

  def startTimer:Unit  = {
    println("FamilyShard: periodic send SimulateRestart ")
    //here we will create a timer that will send periodic messages on
    new Timer().schedule(new TimerTask() {
      override def run(): Unit = {
        println("\n\nFamilyShard:Sending SimulateRestart message to FamilyShard")
        self  ! SimulateRestart
        Thread.sleep(5000)
      }
    }
      , 10000,30000)
  }
}
