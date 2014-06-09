package org.opendaylight.controller.datastore.clustered

import akka.actor.{Props, ActorSystem}
import akka.persistence.Persistent
import org.opendaylight.controller.datastore.clustered.actor.ShardReplica
import java.util.{TimerTask, Timer}
import org.opendaylight.controller.datastore.clustered.persistence.model.Families
import scala.collection.immutable.TreeMap
import scala.collection.mutable
import org.opendaylight.controller.datastore.clustered.actor.TreeShard
import com.typesafe.config.ConfigFactory

/**
 * Sends messages to Shard to trigger processing of snapshot or persisting of data
 *
 *
 * @author: syedbahm
 *          Date: 5/15/14
 *
 *
 *
 */
class ShardManager {
  val system = ActorSystem("clustered-datastore-persistence")
  //val processor = system.actorOf(Props(classOf[Shard]), "clustered-datastore-persistence-snapshot")
  val processor = system.actorOf(Props(classOf[TreeShard]), "clustered-datastore-persistence-snapshot")
  val view = system.actorOf(Props(classOf[ShardReplica]), "clustered-datastore-persistence-view")

  System.out.println("ShardManager started @" + System.currentTimeMillis())


  def createPersistingOfNFamiles(start: Int, n: Int) = {
    val start = System.currentTimeMillis()


    processor ! "persist-start"
    var family = new Families
    for (x <- 1 to n) {

      family.addAFamily(x, 3, 2)
      var tv: TreeMap[String, mutable.HashMap[String, Any]] = family.getFamilies;
      tv += ("MsgId" -> (mutable.HashMap("MsgId" -> x)))

      processor ! Persistent(tv)
      family.resetFamilies
    }
    println("Sending of persist messages of " + n + " families took (just delivering messages) in (ms) " + (System.currentTimeMillis() - start));
    //print the number of entries and time taken
    processor ! "persist-done"


  }

  // sends messages of about 15k families every 1 minute for 10 rounds
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


  def snapshotAfterPersist(count: Int, runCount: Int) = {
    var i: Int = 1;
    while (i < (runCount * count)) {
      createPersistingOfNFamiles(i, count);
      processor ! "flush"
      processor ! "snapshot"
      i = i + count
    }
    true;
  }


  def output = {
    //print the number of actual TreeMap entries
    processor ! "output"
  }

  def shutdown = {
    system.shutdown
  }

  def reset = {
    processor ! "flush"
    processor ! "deleteSnapshots"
  }


  def viewSnapshot = {
    view ! "snapshot"
  }

}
