package org.opendaylight.controller.datastore.clustered.actor

import akka.persistence.{Persistent, Processor}
import scala.collection.mutable.HashMap

import scala.collection.immutable.TreeMap
import akka.actor.ActorLogging
import org.opendaylight.controller.datastore.clustered.messages.{SimulateResume, SimulateRestart}
import org.opendaylight.controller.datastore.clustered.exception.{ShardResumeException, ShardRestartException}
import java.util.{TimerTask, Timer}

/**
 * Represents a shard in the cluster
 * Utilize a TreeMap internally to capture the state
 * @author: syedbahm
 *          Date: 5/15/14
 */

class TreeShard extends Processor with ActorLogging {
  override def processorId = "tree-shard"

  var tree = TreeMap.empty[String, Any]
  var persistStart = 0L


  override def preStart: Unit = {
    super.preStart()
    println("TreedShard:Starting TreeShard instance hashcode # {}", this.hashCode())
    self ! "UNHOLD"
    self ! "output"

  }

  override def postStop() {
    println("TreeShard:Stopping TreeShard instance hashcode # {}", this.hashCode());
  }

  var initializing = true;


  def recoveryCompleted: Unit = {

    println("TreeShard:recovery completed message @ (in ms) " + System.currentTimeMillis())
  }



  def receive: Receive = {
    case "UNHOLD" =>
      recoveryCompleted
      unstashAll
      startTimer

    case "persist-start" => {
      persistStart = System.currentTimeMillis();
      println("TreeShard:Start persisting of TreeMap  @" + persistStart)

    }
    case SimulateRestart =>
      println("TreeShard: restart called")
      context.stop(self)

    case SimulateResume => {
      println("TreeShard: Resume called ")
      throw new ShardResumeException()
    }
    //A family is added for each persistent message  received
    case Persistent(s, snr) => {
      if (!recoveryRunning) {

        val l: TreeMap[String, Any] = s match {
          case x: TreeMap[String, Any] => x // this extracts the value in a as an Int
          case _ => {
            println("TreeShard:Error: wasn't a map"); null
          }
        }
        tree = tree ++ l

      } else {
        stash;
    }
    }


    case "output" => {
      println("TreeShard:current tree size  = " + tree.size + " @" + System.currentTimeMillis)
      var x = 0
      while (x < tree.size) {

        if (x % 1000 == 0) {
          println("TreeShard:contents of tree at location" + x + " " + tree.toList(x));
        }
        x = x + 1000
      }
    }

    case "persist-done" => {
      println("TreeShard:Persisting " + tree.size + " families took (in ms) " + (System.currentTimeMillis() - persistStart));
      self ! "output"

    }


    //here we will remove all messages from journal
    case "flush" => {

      val start = System.currentTimeMillis()
      deleteMessages(scala.Long.MaxValue, true)
      val end = System.currentTimeMillis();
      println("TreeShard:emptying the journal took (in ms)" + (end - start));

    }

    case _ =>
      if (recoveryRunning) {
        stash()
      } else {
        println("TreeShard:Error Unhandled message ")
      }

  }


  def startTimer:Unit  = {
    //here we will create a timer that will send periodic messages on
    new Timer().schedule(new TimerTask() {
      override def run(): Unit = {
        println("\n\n!!!Sending periodic output message to show TreeShard is up & running!!!")

        self ! "output"

      }
    }
      , 10000,10000)
  }


}
