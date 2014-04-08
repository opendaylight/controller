/*
 * Copyright Inocybe Technlogies, 2014.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messaging.datastore.operations

import akka.util.Timeout
import akka.routing._
import akka.pattern.{ask, AskSupport }
import akka.actor.{Actor, ActorRef, Props}
import akka.cluster.routing._
import akka.cluster.Cluster
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.Subscribe
import ca.inocybe.cerebrum._
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import spray.json._

class ClusterBroker extends Actor{
 
  implicit val timeout = Timeout(5 seconds)
  
  // Broadcast cluster aware routers
  val refs = for(i <- 1 to 3) yield {
    val routees = "user/b%s".format(i)
    val routerName = "B%s".format(i)
    val routerClusterConfig = ClusterRouterSettings(totalInstances = 100, routeesPath = routees, allowLocalRoutees = true, useRole = Some("Node"))
    context.actorOf(Props.empty.withRouter(ClusterRouterConfig(BroadcastRouter(), routerClusterConfig)), name = routerName)
  }
  
  // Main hashing router
  val hashingRouter = context.actorOf(Props.empty.withRouter(ConsistentHashingRouter(routees = refs)))
  
  // Register the hashing router to the "Router" topic
  val mediator = DistributedPubSubExtension(context.system).mediator
  mediator ! Subscribe("Router", hashingRouter)
  
  // Register SixthSense to the "Info" topic
  mediator ! Subscribe("Info", self)
     
  def receive = {
    case Nodes =>
      val temp = context.actorOf(Props(classOf[Temp], sender))
      Cluster(context.system).sendCurrentClusterState(temp)
    case Data => 
      val originalSender = sender
      val list = refs.map { x =>
        val temp = context.actorOf(Props(classOf[ShortTermMemory], x))
        (ask(temp, Data).mapTo[List[(String, String, List[MemoryUnit])]])
      }
      val futureList = Future.sequence(list)
      implicit val jsoner = FrontalLobe.elementsJson
      futureList.map{x => 
        originalSender ! OperationOK(Some(x.foldLeft(List[(String, String, List[MemoryUnit])]())((x, y) => x ++ y).toJson))}
    case message  => println("Receive the following message: %s".format(message))
  }
}