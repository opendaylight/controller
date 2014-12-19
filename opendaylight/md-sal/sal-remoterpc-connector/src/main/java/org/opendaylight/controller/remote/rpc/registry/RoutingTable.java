/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry;

import akka.actor.ActorRef;
import akka.japi.Option;
import akka.japi.Pair;
import org.opendaylight.controller.sal.connector.api.RpcRouter;

/**
 * @author Thomas Pantelis
 */
public interface RoutingTable {

    Option<Pair<ActorRef, Long>> getRouterFor(RpcRouter.RouteIdentifier<?, ?, ?> routeId);

    boolean contains(RpcRouter.RouteIdentifier<?, ?, ?> routeId);

    boolean isEmpty();

    int size();

    ActorRef getRouter();
}