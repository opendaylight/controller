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
import org.opendaylight.controller.remote.rpc.registry.gossip.Copier;
import org.opendaylight.controller.sal.connector.api.RpcRouter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class RoutingTable implements Copier<RoutingTable>, Serializable {

    private Map<RpcRouter.RouteIdentifier<?, ?, ?>, Long> table = new HashMap<>();
    private ActorRef router;

    @Override
    public RoutingTable copy() {
        RoutingTable copy = new RoutingTable();
        copy.setTable(new HashMap<>(table));
        copy.setRouter(this.getRouter());

        return copy;
    }

    public Option<Pair<ActorRef, Long>> getRouterFor(RpcRouter.RouteIdentifier<?, ?, ?> routeId){
        Long updatedTime = table.get(routeId);

        if (updatedTime == null || router == null)
            return Option.none();
        else
            return Option.option(new Pair<>(router, updatedTime));
    }

    public void addRoute(RpcRouter.RouteIdentifier<?,?,?> routeId){
        table.put(routeId, System.currentTimeMillis());
    }

    public void removeRoute(RpcRouter.RouteIdentifier<?, ?, ?> routeId){
        table.remove(routeId);
    }

    public Boolean contains(RpcRouter.RouteIdentifier<?, ?, ?> routeId){
        return table.containsKey(routeId);
    }

    public Boolean isEmpty(){
        return table.isEmpty();
    }
    ///
    /// Getter, Setters
    ///
    //TODO: Remove public
    public Map<RpcRouter.RouteIdentifier<?, ?, ?>, Long> getTable() {
        return table;
    }

    void setTable(Map<RpcRouter.RouteIdentifier<?, ?, ?>, Long> table) {
        this.table = table;
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
