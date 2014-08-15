/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc.utils;

import akka.actor.ActorRef;

/**
 * This Interface is added to abstract out the way rpc execution could be
 * routed, if more than one node in cluster is capable of executing the rpc.
 *
 * We can pick node randomly, round robin manner or based on last updated time etc.
 */

public interface RoutingLogic {

  ActorRef select();
}
