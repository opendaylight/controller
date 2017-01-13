/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry;

import akka.actor.ActorRef;
import com.google.common.base.Preconditions;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.opendaylight.controller.remote.rpc.registry.gossip.Copier;
import org.opendaylight.controller.sal.connector.api.RpcRouter;
import org.opendaylight.controller.sal.connector.api.RpcRouter.RouteIdentifier;

public class RoutingTable implements Copier<RoutingTable>, Serializable {
    private static final long serialVersionUID = 5592610415175278760L;

    private final Map<RouteIdentifier<?, ?, ?>, Long> table;
    private final ActorRef router;

    private RoutingTable(final ActorRef router, final Map<RouteIdentifier<?, ?, ?>, Long> table) {
        this.router = Preconditions.checkNotNull(router);
        this.table = Preconditions.checkNotNull(table);
    }

    RoutingTable(final ActorRef router) {
        this(router, new HashMap<>());
    }

    @Override
    public RoutingTable copy() {
        return new RoutingTable(router, new HashMap<>(table));
    }

    public Set<RpcRouter.RouteIdentifier<?, ?, ?>> getRoutes() {
        return table.keySet();
    }

    public void addRoute(final RpcRouter.RouteIdentifier<?, ?, ?> routeId) {
        table.put(routeId, System.currentTimeMillis());
    }

    public void removeRoute(final RpcRouter.RouteIdentifier<?, ?, ?> routeId) {
        table.remove(routeId);
    }

    public boolean contains(final RpcRouter.RouteIdentifier<?, ?, ?> routeId) {
        return table.containsKey(routeId);
    }

    public boolean isEmpty() {
        return table.isEmpty();
    }

    public int size() {
        return table.size();
    }

    public ActorRef getRouter() {
        return router;
    }

    @Override
    public String toString() {
        return "RoutingTable{" + "table=" + table + ", router=" + router + '}';
    }
}
