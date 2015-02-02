/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry;

import akka.actor.ActorRef;
import akka.japi.Option;
import akka.japi.Pair;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.opendaylight.controller.remote.rpc.registry.gossip.Copier;
import org.opendaylight.controller.sal.connector.api.RpcRouter;

public class RoutingTable implements Copier<RoutingTable>, Serializable {
    private static final long serialVersionUID = 5592610415175278760L;

    private final Map<RpcRouter.RouteIdentifier<?, ?, ?>, Long> table = new HashMap<>();
    private ActorRef router;

    @Override
    public RoutingTable copy() {
        RoutingTable copy = new RoutingTable();
        copy.table.putAll(table);
        copy.setRouter(this.getRouter());

        return copy;
    }

    public Option<Pair<ActorRef, Long>> getRouterFor(RpcRouter.RouteIdentifier<?, ?, ?> routeId){
        Long updatedTime = table.get(routeId);

        if (updatedTime == null || router == null) {
            return Option.none();
        } else {
            return Option.option(new Pair<>(router, updatedTime));
        }
    }

    public void addRoute(RpcRouter.RouteIdentifier<?,?,?> routeId){
        table.put(routeId, System.currentTimeMillis());
    }

    public void removeRoute(RpcRouter.RouteIdentifier<?, ?, ?> routeId){
        table.remove(routeId);
    }

    public boolean contains(RpcRouter.RouteIdentifier<?, ?, ?> routeId){
        return table.containsKey(routeId);
    }

    public boolean isEmpty(){
        return table.isEmpty();
    }

    public int size() {
        return table.size();
    }

    public ActorRef getRouter() {
        return router;
    }

    public void setRouter(ActorRef router) {
        this.router = router;
    }

    @Override
    public String toString() {
        return "RoutingTable{" +
                "table=" + table +
                ", router=" + router +
                '}';
    }
}
