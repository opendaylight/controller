package org.opendaylight.controller

import akka.actor._
import akka.util.Timeout

/** Main Server class
 *
 */
object Server {

  ShardingStrategyManager.registerShardingStrategy("module", new ModuleShardingStrategy(_))
  ShardingStrategyManager.registerShardingStrategy("hash", new HashShardingStrategy(_))

  val system = ActorSystem("opendaylight");

  val clusteringServiceProps = Props(new ClusteringService());
  val clusteringServiceActor = system.actorOf(clusteringServiceProps, "clustering-service" );

  val shardManagerActor = system.actorOf(Props(new ShardManager(clusteringServiceActor)), "shard-manager");

  val messageSenderActor = system.actorOf(Props(new MessageSender(shardManagerActor)), "message-sender" );

  def main(args : Array[String]) {

  }

}
