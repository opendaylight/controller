/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc.registry.mbeans;

import akka.actor.Address;
import org.opendaylight.controller.md.sal.common.util.jmx.AbstractMXBean;
import org.opendaylight.controller.remote.rpc.registry.RoutingTable;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry;
import org.opendaylight.controller.remote.rpc.registry.gossip.Bucket;
import org.opendaylight.controller.sal.connector.api.RpcRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class RemoteRpcRegistryMXBeanImpl extends AbstractMXBean implements RemoteRpcRegistryMXBean {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final String NULL_CONSTANT = "null";

    private final String LOCAL_CONSTANT = "local";

    private final String ROUTE_CONSTANT = "route:";

    private final String NAME_CONSTANT = " | name:";

    private final RpcRegistry rpcRegistry;

    public RemoteRpcRegistryMXBeanImpl(final RpcRegistry rpcRegistry) {
        super("RemoteRpcRegistry", "RemoteRpcBroker", null);
        this.rpcRegistry = rpcRegistry;
        registerMBean();
    }

    @Override
    public Set<String> getGlobalRpc() {
        RoutingTable table = rpcRegistry.getLocalBucket().getData();
        Set<String> globalRpc = new HashSet<>(table.getRoutes().size());
        for(RpcRouter.RouteIdentifier<?, ?, ?> route : table.getRoutes()){
            if(route.getRoute() == null) {
                globalRpc.add(route.getType() != null ? route.getType().toString() : NULL_CONSTANT);
            }
        }
        if(log.isDebugEnabled()) {
            log.debug("Locally registered global RPCs {}", globalRpc);
        }
        return globalRpc;
    }

    @Override
    public Set<String> getLocalRegisteredRoutedRpc() {
        RoutingTable table = rpcRegistry.getLocalBucket().getData();
        Set<String> routedRpc = new HashSet<>(table.getRoutes().size());
        for(RpcRouter.RouteIdentifier<?, ?, ?> route : table.getRoutes()){
            if(route.getRoute() != null) {
                StringBuilder builder = new StringBuilder(ROUTE_CONSTANT);
                builder.append(route.getRoute().toString()).append(NAME_CONSTANT).append(route.getType() != null ?
                    route.getType().toString() : NULL_CONSTANT);
                routedRpc.add(builder.toString());
            }
        }
        if(log.isDebugEnabled()) {
            log.debug("Locally registered routed RPCs {}", routedRpc);
        }
        return routedRpc;
    }

    @Override
    public Map<String, String> findRpcByName(final String name) {
        RoutingTable localTable = rpcRegistry.getLocalBucket().getData();
        // Get all RPCs from local bucket
        Map<String, String> rpcMap = new HashMap<>(getRpcMemberMapByName(localTable, name, LOCAL_CONSTANT));

        // Get all RPCs from remote bucket
        Map<Address, Bucket<RoutingTable>> buckets = rpcRegistry.getRemoteBuckets();
        for(Address address : buckets.keySet()) {
            RoutingTable table = buckets.get(address).getData();
            rpcMap.putAll(getRpcMemberMapByName(table, name, address.toString()));
        }
        if(log.isDebugEnabled()) {
            log.debug("list of RPCs {} searched by name {}", rpcMap, name);
        }
        return rpcMap;
    }

    @Override
    public Map<String, String> findRpcByRoute(String routeId) {
        RoutingTable localTable = rpcRegistry.getLocalBucket().getData();
        Map<String, String> rpcMap = new HashMap<>(getRpcMemberMapByRoute(localTable, routeId, LOCAL_CONSTANT));

        Map<Address, Bucket<RoutingTable>> buckets = rpcRegistry.getRemoteBuckets();
        for(Address address : buckets.keySet()) {
            RoutingTable table = buckets.get(address).getData();
            rpcMap.putAll(getRpcMemberMapByRoute(table, routeId, address.toString()));

        }
        if(log.isDebugEnabled()) {
            log.debug("list of RPCs {} searched by route {}", rpcMap, routeId);
        }
        return rpcMap;
    }

    /**
     * Search if the routing table route String contains routeName
     */

    private Map<String,String> getRpcMemberMapByRoute(final RoutingTable table, final String routeName,
                                                      final String address) {
        Set<RpcRouter.RouteIdentifier<?, ?, ?>> routes = table.getRoutes();
        Map<String, String> rpcMap = new HashMap<>(routes.size());
        for(RpcRouter.RouteIdentifier<?, ?, ?> route : table.getRoutes()){
            if(route.getRoute() != null) {
                String routeString = route.getRoute().toString();
                if(routeString.contains(routeName)) {
                    StringBuilder builder = new StringBuilder(ROUTE_CONSTANT);
                    builder.append(routeString).append(NAME_CONSTANT).append(route.getType() != null ?
                        route.getType().toString() : NULL_CONSTANT);
                    rpcMap.put(builder.toString(), address);
                }
            }
        }
        return rpcMap;
    }

    /**
     * Search if the routing table route type contains name
     */
    private Map<String, String>  getRpcMemberMapByName(final RoutingTable table, final String name,
                                                       final String address) {
        Set<RpcRouter.RouteIdentifier<?, ?, ?>> routes = table.getRoutes();
        Map<String, String> rpcMap = new HashMap<>(routes.size());
        for(RpcRouter.RouteIdentifier<?, ?, ?> route : routes){
            if(route.getType() != null) {
                String type = route.getType().toString();
                if(type.contains(name)) {
                    StringBuilder builder = new StringBuilder(ROUTE_CONSTANT);
                    builder.append(route.getRoute() != null ? route.getRoute().toString(): NULL_CONSTANT)
                        .append(NAME_CONSTANT).append(type);
                    rpcMap.put(builder.toString(), address);
                }
            }
        }
        return rpcMap;
    }



    @Override
    public String getBucketVersions() {
        return rpcRegistry.getVersions().toString();
    }

}