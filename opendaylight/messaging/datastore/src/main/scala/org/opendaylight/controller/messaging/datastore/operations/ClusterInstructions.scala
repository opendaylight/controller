/*
 * Copyright Inocybe Technlogies, 2014.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.messaging.datastore.operations

trait ClusterInstructions
object Data extends ClusterInstructions
case class Memorize(key: String, value: String) extends ClusterInstructions
case class Reinforce(key: String, value: String) extends ClusterInstructions
case class Recall(key: String) extends ClusterInstructions
case class Forget(key: String) extends ClusterInstructions