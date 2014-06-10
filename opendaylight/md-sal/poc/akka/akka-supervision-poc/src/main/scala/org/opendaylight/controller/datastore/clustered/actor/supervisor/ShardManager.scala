package org.opendaylight.controller.datastore.clustered.actor.supervisor

import akka.actor.SupervisorStrategy._
import akka.actor._
import akka.persistence.Persistent
import org.opendaylight.controller.datastore.clustered.actor.{FamilyShard, TreeShard}
import org.opendaylight.controller.datastore.clustered.actor.monitor._
import java.util.{TimerTask, Timer}
import org.opendaylight.controller.datastore.clustered.messages._
import org.opendaylight.controller.datastore.clustered.exception.{ShardRestartException, ShardResumeException}
import org.opendaylight.controller.datastore.clustered.messages.DownShard
import org.opendaylight.controller.datastore.clustered.messages.ResetShards
import org.opendaylight.controller.datastore.clustered.messages.Register
import akka.actor.SupervisorStrategy
import org.opendaylight.controller.datastore.clustered.model.Families
import scala.collection.mutable.HashMap
import scala.collection.immutable.TreeMap
import akka.actor.OneForOneStrategy
import scala.concurrent.duration._

/**
 *
 * @author: syedbahm
 *
 */
class ShardManager extends Actor with ActorLogging {

  var treeShardActor = context.actorOf(Props[TreeShard], name = "TreeShardActor")
  var familyShardActor = context.actorOf(Props[FamilyShard], name = "FamilyShardActor")
  val monitor = context.system.actorOf(Props[MonitorActor], name = "monitor")

  /*
    we will be registering the two child actors treeShardActor, familyShardActor
    with MonitorActor that will notify us if these actor terminated
   */

  override def preStart() {
    monitor ! new Register(treeShardActor, self)
    monitor ! new Register(familyShardActor,self)
  }

  //we are using one for one strategy i.e just start the actor which goes down
  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 60 seconds) {

    case _: ShardResumeException => Resume
    case _: ShardRestartException => Restart
    case _: IllegalArgumentException => Stop
    case _: Exception => Escalate
  }

  def createPersistingOfNFamiles(start: Int, n: Int) = {
    val start = System.currentTimeMillis()


    treeShardActor ! "persist-start"
    familyShardActor ! "persist-start"
    var family = new Families
    for (x <- 1 to n) {

      family.addAFamily(x, 3, 2)
      var tv: TreeMap[String, HashMap[String, Any]] = family.getFamilies;
      tv += ("MsgId" -> (HashMap("MsgId" -> x)))

      treeShardActor ! Persistent(tv)
      familyShardActor ! Persistent(tv)
      family.resetFamilies
    }
    println("ShardManager:Sending of persist messages of " + n + " families took (just delivering messages) in (ms) " + (System.currentTimeMillis() - start));
    //print the number of entries and time taken
    treeShardActor ! "persist-done"
    familyShardActor ! "persist-done"


  }
  def reset = {
    treeShardActor ! "flush"
    familyShardActor ! "flush"
  }

  def persist(count: Int, runCount: Int) = {
    new Timer().schedule(new TimerTask() {
      override def run(): Unit = {
        var i = 1;
        while (i < (runCount * count)) {

          createPersistingOfNFamiles(i, count);
          i = i + count;
        }
      }
    }, 0)

  }

  def receive = {

    case msg:ResetShards =>
      reset

    case msg: Persist => {
      persist(msg.numberOfFamilies, msg.numberOfRun);
    }

    case mesg: DownShard => {
      println("\n\n====ShardManager:Got a DownShard message, restarting the shard" + mesg.text)
      if (mesg.text == treeShardActor.toString()) {
        println("ShardManager:restarting actor " + mesg.text)
        treeShardActor = context.actorOf(Props[TreeShard], name = "TreeShardActor")
        monitor ! new Register(treeShardActor,self)
      }
      else {
        familyShardActor = context.actorOf(Props[FamilyShard], name = "FamilyShardActor")
        monitor ! new Register(familyShardActor,self)
      }
    }
    case msg:Output =>
      treeShardActor ! "output"
      familyShardActor ! "output"

  }





}
