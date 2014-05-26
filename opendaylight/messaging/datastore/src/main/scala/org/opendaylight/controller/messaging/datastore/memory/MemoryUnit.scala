/*
 * Copyright Inocybe Technlogies, 2014.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messaging.datastore.memory

import akka.cluster.VectorClock

case class MemoryUnit(key: String, var value: String, clock: VectorClock) {
  override final def toString = {
    "=== MEMORY UNIT ===\nKey: %s\nValue: %s\nVectorClock: %s\n==================="
    .format(key, value, clock)
  }
}