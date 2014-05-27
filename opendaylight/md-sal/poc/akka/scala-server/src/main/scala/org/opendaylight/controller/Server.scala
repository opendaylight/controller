package org.opendaylight.controller

import akka.actor._
import akka.util.Timeout

object Server {
  val system = ActorSystem("opendaylight");
  val clusteringServiceProps = Props(new ClusteringService());
  val clusteringServiceActor = system.actorOf(clusteringServiceProps, "clustering-service" );
  val shardManagerActor = system.actorOf(Props(new ShardManager(clusteringServiceActor)), "shard-manager")

  def main(args : Array[String]) {

  }

}
