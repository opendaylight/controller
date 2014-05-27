package org.opendaylight.controller.datastore.clustered

/**
 *
 * @author: syedbahm
 *          Date: 5/21/14
 *
 *
 *
 */
object Main {

  def main(args: Array[String]): Unit = {
    val shardManager = new ShardManager
    if (args.length < 3) {
      println("usage: Main <option> <number-of-families> <run>\n\t\t where option = p (to time (p)erist)" +
        "\n\t\t\t= ps (to (p)ersist and time just (s)napshot)\n\t\t\t= sr (to time existing (s)napshot (r)ecovery;\n\t\t" +
        "= r (to time (r)ecovery without snapshot just from journal;\n\t\t" +
        "number-of-families = count of number of families to be used;\n\t\t" +
        "run = number of runs\n")
    } else {


      args(0) match {
        case "p" => {
          //let us reset everything before the run
          shardManager.reset;
          shardManager.persist(args(1).toInt, args(2).toInt)
        }

        case "ps" => {
          //let us reset everything before the run
          shardManager.reset
          shardManager.snapshotAfterPersist(args(1).toInt, args(2).toInt)

        }
        case "sr" => {
          println("recovery from journal..")
          //just waiting for now for recovery to complete -- need to register listener later


        }
        case "r" => {
          println("recovery from journal..")

        }
        case "po" => {
          //hidden option to persist and output few persisted items
          shardManager.reset;
          shardManager.persist(args(1).toInt, args(2).toInt)
        }
          Thread.sleep(15000);
          shardManager.output
        case "pso" => {
          //let us reset everything before the run
          shardManager.reset
          shardManager.snapshotAfterPersist(args(1).toInt, args(2).toInt)
          Thread.sleep(15000);
          shardManager.output
        }
        case "psv" => {
          //hidden option to persist and output few persisted items
          shardManager.reset;
          shardManager.snapshotAfterPersist(args(1).toInt, args(2).toInt)
          Thread.sleep(120000);
          shardManager.viewSnapshot
        }

      }
      //currently just timing out to shutdown the sytem
      Thread.sleep(400000);
    }

    shardManager.shutdown
  }

}