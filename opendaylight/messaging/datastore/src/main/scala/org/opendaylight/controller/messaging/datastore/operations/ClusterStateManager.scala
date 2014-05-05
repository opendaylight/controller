/*
 * Copyright Inocybe Technlogies, 2014.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messaging.datastore.operations

import akka.actor.ActorRef
import akka.actor.Actor
import akka.cluster.ClusterEvent.CurrentClusterState
import akka.actor.PoisonPill
import spray.json._

class Temp(replyTo: ActorRef) extends Actor {
  def receive = {
    case state: CurrentClusterState =>
      val list = state.members.map { x =>
        val address = x.address
        JsObject("address" -> JsString(address.host.get),
          "port" -> JsNumber(address.port.get))
      }.toList
      replyTo ! OperationOK(Some(JsArray(list)))
      self ! PoisonPill
  }
}