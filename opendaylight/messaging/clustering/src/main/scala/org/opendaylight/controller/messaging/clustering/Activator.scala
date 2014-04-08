/*
 * Copyright Inocybe Technlogies, 2014.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messaging.clustering

import akka.actor.{ActorSystem, Props}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.ClusterDomainEvent
import akka.actor.PoisonPill
import akka.contrib.pattern.ClusterSingletonManager
import java.lang.System
import org.osgi.framework.BundleActivator
import org.osgi.framework.BundleContext

class Activator extends BundleActivator {
  def start(context: BundleContext) {
        // Start the actor system.
    if (args.nonEmpty) System.setProperty("akka.remote.netty.tcp.port", args(0))
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
  def stop(context: BundleContext){}
}