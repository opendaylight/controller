/*
 * Copyright Inocybe Technlogies, 2014.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messaging.clustering

import akka.actor.{ Actor, ActorLogging }
import akka.cluster.ClusterEvent._
import akka.contrib.pattern.DistributedPubSubMediator
import akka.contrib.pattern.DistributedPubSubExtension
import DistributedPubSubMediator.{Subscribe, SubscribeAck}
import akka.cluster.Cluster
import java.util.UUID
import akka.actor.ActorRef
import scala.collection.mutable
import spray.json._
import akka.actor.Address
import akka.contrib.pattern.DistributedPubSubMediator.Publish

class SimpleClusterListener extends Actor with ActorLogging {
  
  val cluster = Cluster(context.system)
  val IdToAddress = mutable.Map[String, Address]()
  val mediator = DistributedPubSubExtension(context.system).mediator
  mediator ! Subscribe("clusterListener", self)
  
  cluster.subscribe(self, classOf[ClusterDomainEvent])
  cluster.sendCurrentClusterState(self)
  
  def receive = {
    // Get actor address and publish a message with the address and the
    // sender in parameter
    case ("getMetrics", id: String) =>
      mediator ! Publish("metrics", ("getMetrics", findById(id).get._2, sender))
      
    case ("leaveCluster", id: String) =>
      // If it's not the last node on the cluster remove it
      if(cluster.state.members.size > 1){
        val address = findById(id).get._2
        cluster.leave(address)
        cluster.down(address)
        sender ! JsString("Node with address %s left the cluster.".format(address))
      }else{ // Otherwise reply with invalid
        sender ! JsString("Can't remove the last node from the cluster")
      }
      
    // Get cluster state and return information about its members
    case "allnodes" =>
      val reply = cluster.state.members.
      map{x => JsObject("node"   -> JsString(x.address.hostPort), 
        	            "id"     -> JsString(findByAddress(x.address).get._1),
        		        "status" -> JsString(x.status.toString))}.toList
      sender ! JsArray(reply)
      
    case MemberUp(member) =>
      if(!findByAddress(member.address).isDefined)
        IdToAddress(UUID.randomUUID().toString) = member.address
      log.info("Member is Up: {}", member.address)
    case UnreachableMember(member) =>
      log.info("Member detected as unreachable: {}", member)
    case MemberRemoved(member, previousStatus) =>
      log.info("Member is Removed: {} after {}", member.address, previousStatus)
    case state: CurrentClusterState =>
      state.members.foreach{x => 
        if(!findByAddress(x.address).isDefined)
          IdToAddress(UUID.randomUUID().toString) = x.address
      }
      log.info("Current members: {}", state.members.mkString(", "))
    case _ : ClusterDomainEvent => // ignore
    case _ =>
  }
  
  def findByAddress(address: Address) = IdToAddress.find(y => y._2.equals(address))
  def findById(id: String) = IdToAddress.find(y => y._1.equals(id))
}