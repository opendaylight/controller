/*
 * Copyright Inocybe Technlogies, 2014.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messaging.datastore

import akka.cluster.Cluster
import akka.actor.PoisonPill
import akka.cluster.ClusterEvent.ClusterDomainEvent
import akka.contrib.pattern.ClusterSingletonManager
import akka.actor.{ Actor, ActorLogging, ActorSystem, Props }

import scala.concurrent.duration._

import java.util.UUID
import java.lang.System
import org.osgi.framework.BundleActivator
import org.osgi.framework.BundleContext
import org.opendaylight.controller.messaging.datastore.operations._
import org.opendaylight.controller.messaging.datastore.memory._
class Activator extends BundleActivator {
  def stop(context: BundleContext) {
    //nothing done here for now...
  }
  def start(context: BundleContext) {
    
    // Start the actor system.
    //if (args.nonEmpty) System.setProperty("akka.remote.netty.tcp.port", args(0))
    val system = ActorSystem("DB")

    // Initialize the cluster listener.
    val clusterListener = system.actorOf(Props[SimpleClusterListener], name = "clusterListener")
    Cluster(system).subscribe(clusterListener, classOf[ClusterDomainEvent])

    initSingletons
    initNeurons

    // Start the webserver as a singleton.
    // Start the sixthSense actor as a singleton.
    def initSingletons = {
      //system.actorOf(ClusterSingletonManager.props(singletonProps = handOverData => Props[ClusterBroker], "active",
        //PoisonPill, Some("Router")), "ClusterBroker-%s".format(UUID.randomUUID))
      system.actorOf(ClusterSingletonManager.props(singletonProps = Props(classOf[ClusterBroker]), singletonName = "ClusterBroker",
      terminationMessage = PoisonPill,
      role = Some("Router")), name = "ClusterBrokerActor")
//      system.actorOf(ClusterSingletonManager.props(singletonProps = handOverData => Props[OperationsProcessor], "active",
  //      PoisonPill, None), "OperationsProcessor-%s".format(UUID.randomUUID))
    system.actorOf(ClusterSingletonManager.props(singletonProps = Props(classOf[OperationsProcessor]), singletonName = "OperationsProcessor",
    terminationMessage = PoisonPill,
    role = None), name = "OperationsProcessorActor")
    }

    // Neurons to be initialized in each node.
    def initNeurons = {
      for (i <- 1 to 3) {
        system.actorOf(Props[DataStore], "b%s".format(i))
      }
    }
    
  }
}
