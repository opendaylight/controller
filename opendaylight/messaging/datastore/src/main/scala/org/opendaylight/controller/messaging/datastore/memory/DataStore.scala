/*
 * Copyright Inocybe Technlogies, 2014.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messaging.datastore.memory

import scala.collection.mutable
import akka.cluster.{VectorClock, Cluster}
import akka.actor.{Actor, ActorRef, ActorLogging}
import spray.json._
import org.opendaylight.controller.messaging.datastore.operations._


/**
 * Storage unit in the DB. Contains a map of objects
 */
class Neuron extends Actor with ActorLogging{
  
  val name = "%s, %s".format(self.path.name, Cluster(context.system).selfAddress.hostPort)
  val loggingHeader = "Neuro %s: ".format(name)
  
  implicit val memoryUnitJson = FrontalLobe.memoryUnitJson
  
  override def preStart = {
    log.info(loggingHeader + "Started.")
  }
  
  val map = mutable.Map.empty[String, MemoryUnit]
  
  def receive = {

    case Memorize(key, value) =>
      if (map.get(key).isDefined) {
        sender ! OperationFailed
      } else {
        val vc = new VectorClock :+ name
        val memU = MemoryUnit(key, value.toString, vc)
        map += key -> memU
        sender ! OperationOK(None)
      }
      
    /**
     * Retrieve a memory unit from the node
     */
    case Recall(key) => 
      val value = map.get(key)
      if(value.isDefined){
        sender ! OperationOK(Some(value.get.toJson))
      }else{
        sender ! OperationNotFound()
      }
    
    /**
     * Update an existing memory unit from the node.
     */
    case Reinforce(key, value) =>
      if(map.get(key).isDefined){
        val vc = map.get(key).get.clock :+ name
        map += key -> MemoryUnit(key, value, vc)
        sender ! OperationOK(None)
      }else{
        sender ! OperationNotFound()
      }
      
    /**
     * Remove an memory unit from the node. 
     */
    case Forget(key) =>
      if(map.get(key).isDefined){
        map -= key
        sender ! OperationOK(None)
      }else{
        sender ! OperationNotFound()
      }
        
    case (ref: ActorRef, Data) =>
      val splitted = name.split(",")
      ref ! (splitted(0), splitted(1), map.foldLeft(List[MemoryUnit]())((x, y) => x ++ List(y._2)))
      
    // TODO Define default behavior for unmatched messages.
    case message => println(loggingHeader + "received this: %s".format(message))
  }
}