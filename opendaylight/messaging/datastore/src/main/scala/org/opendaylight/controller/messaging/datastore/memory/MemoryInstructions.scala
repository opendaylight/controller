/*
 * Copyright Inocybe Technlogies, 2014.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messaging.datastore.memory

import akka.routing.ConsistentHashingRouter.ConsistentHashable

sealed trait MemoryInstruction
case object Data extends MemoryInstruction

sealed trait MemoryT extends ConsistentHashable with MemoryInstruction{
  val key: Any
  override def consistentHashKey: Any = key
}

case class Memorize(key: String, value: String) extends MemoryT
case class Reinforce(key: String, value: String)extends MemoryT
case class Recall(key: String) 					extends MemoryT
case class Forget(key: String) 					extends MemoryT