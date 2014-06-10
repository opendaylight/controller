package org.opendaylight.controller.datastore.clustered

import org.opendaylight.controller.datastore.clustered.actor.supervisor.ShardManager
import java.util.{TimerTask, Timer}
import akka.actor.{Props, ActorSystem}
import org.opendaylight.controller.datastore.clustered.actor.{TreeShard, FamilyShard}
import java.util
import org.opendaylight.controller.datastore.clustered.messages.{SimulateResume, SimulateRestart, Persist, ResetShards,Output}

/**
 *
 * @author: syedbahm
 *    Date: 6/5/14
 *
 *
 *
 */
object Main {

  val system = ActorSystem("clustered-datastore")

  val shardManager = system.actorOf(Props(classOf[ShardManager]),name="ShardManager");




  def main(args: Array[String]): Unit = {

    if (args.length < 3) {
      println("usage: Main <option> <number-of-families> <run>\n\t\t where option = pm (to time (p)erist) and start monitoring" +
        "number-of-families = count of number of families to be used;\n\t\t" +
        "run = number of runs\n")
    } else {


      args(0) match {
        case "pm" => {
          //let us reset everything before the run
          shardManager ! ResetShards
          shardManager ! Persist(args(1).toInt, args(2).toInt)
          Thread.sleep(10000)// secs to go to next step
          shardManager ! Output

        }

      }
      //currently just timing out to shutdown the sytem
      Thread.sleep(400000);
    }

    system.shutdown()
  }

}