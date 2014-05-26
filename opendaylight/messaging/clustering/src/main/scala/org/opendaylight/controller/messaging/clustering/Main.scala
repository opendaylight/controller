package org.opendaylight.controller.messaging.clustering

import akka.actor.ActorSystem
import akka.actor.PoisonPill
import akka.actor.Props
import akka.contrib.pattern.ClusterSingletonManager

object Main extends App {
  //  Start the actor system.
  //if (args.nonEmpty) System.setProperty("akka.remote.netty.tcp.port", args(0))
  val system = ActorSystem("cluster")

  // Initialize the cluster listener.
  system.actorOf(ClusterSingletonManager.props(
    singletonProps = Props(classOf[SimpleClusterListener]),
    singletonName = "ClusterListener",
    terminationMessage = PoisonPill,
    role = None))

  // Initialize the RestManager as a singleton
  system.actorOf(ClusterSingletonManager.props(
    singletonProps = Props(classOf[RestManager]),
    singletonName = "RestManager",
    terminationMessage = PoisonPill,
    role = None))

  // Initialize the MetricsListener for the local machine
  system.actorOf(Props[MetricsListener])
}