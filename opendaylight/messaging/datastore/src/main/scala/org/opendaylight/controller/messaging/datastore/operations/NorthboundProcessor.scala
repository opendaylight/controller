/*
 * Copyright Inocybe Technlogies, 2014.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messaging.datastore.operations

import spray.json._
import akka.cluster.VectorClock
import ca.inocybe.cerebrum.MemoryUnit

object NorthBoundProcessor extends DefaultJsonProtocol {

  implicit object vectorClockJson extends RootJsonFormat[VectorClock] {
    def read(value: JsValue) = throw new DeserializationException("Not able to read VectorClock objects")
    def write(vc: VectorClock) = {
      JsString(vc.versions.toList.reverse.head._1.toString)
    }
  }

  implicit val memoryUnitJson = jsonFormat(MemoryUnit.apply, "key", "value", "clock")
  implicit val listMemoryUnitJson = listFormat[MemoryUnit]

  implicit object tupleJson extends RootJsonFormat[(String, String, List[MemoryUnit])] {
    def read(value: JsValue) = throw new DeserializationException("Not able to read Tuple2 of this kind")
    def write(tuple: (String, String, List[MemoryUnit])) = {
      val splitted = tuple._2.split("@")(1).split(":")
      JsObject(
          "type" -> JsString(tuple._1), 
          "node" -> JsObject("address" -> JsString(splitted(0)), "port" -> JsString(splitted(1))),
          "items" -> tuple._3.toJson)
    }
  }
  
  implicit val elementsJson = listFormat[(String, String, List[MemoryUnit])]
}