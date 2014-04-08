/*
 * Copyright Inocybe Technlogies, 2014.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messaging.clustering

import akka.actor.Actor
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.ClusterMetricsChanged
import spray.json._
import scala.collection.mutable.Queue
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.Subscribe
import akka.actor.Address
import akka.actor.ActorRef

class MetricsListener extends Actor{
  
  val selfAddress = Cluster(context.system).selfAddress
  val metricsQ = new Queue[JsObject]
  val maxElements = 20
  
  // Subscribe to the pub/sub module
  val mediator = DistributedPubSubExtension(context.system).mediator
  mediator ! Subscribe("metrics", self)
  
  // Suscribe to the metrics module
  override def preStart(): Unit =
    Cluster(context.system).subscribe(self, classOf[ClusterMetricsChanged])
  override def postStop(): Unit =
    Cluster(context.system).unsubscribe(self)
  
  def receive = {
    case ("getMetrics", address: Address, sender: ActorRef) =>
      if(address.equals(selfAddress)){ // Only reply if my address is received in parameter
        sender ! JsObject("host" -> JsString(address.hostPort), "data" -> JsArray(metricsQ.toList))
      }
    case ClusterMetricsChanged(metrics) =>
      val myMetrics = metrics.filter{_.address == selfAddress}.head
      
      // Store values in the queue
      val values = myMetrics.metrics.map(x => 
        JsObject("name" -> JsString(x.name), "value" -> JsNumber(x.value.floatValue())))
      updateQueue(JsObject("metrics" -> JsArray(values.toList)))
    case _ => // Ignore any other message
  }
  
  // Updates the metrics queue
  def updateQueue(v : JsObject) = {
    if(metricsQ.length >= maxElements) metricsQ.dequeue
    metricsQ.enqueue(v)
  }

}