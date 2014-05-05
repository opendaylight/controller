/*
 * Copyright Inocybe Technlogies, 2014.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messaging.datastore.memory

import akka.actor.{ActorRef, Actor, PoisonPill}
import scala.collection.mutable
import akka.cluster.ClusterEvent.CurrentClusterState
import akka.cluster.Cluster

class InMemoryDataStore(router: ActorRef) extends Actor {

  var nbNodes = -1
  val list = mutable.ListBuffer[(String, String, List[MemoryUnit])]()
  var ori = context.system.deadLetters

  def receive = {
    case state: CurrentClusterState =>
      nbNodes = state.members.size
      router ! (self, Data)
    case Data => 
      ori = sender
      Cluster(context.system).sendCurrentClusterState(self)
    case (nType: String, name: String, items: List[MemoryUnit]) =>
      list += Tuple3(nType, name, items)
      if (list.size == nbNodes) {
        ori ! list.toList
        self ! PoisonPill
      }
    case _ => 
  }
}