/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc.registry.mbeans;

import akka.actor.Address;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.opendaylight.controller.md.sal.common.util.jmx.AbstractMXBean;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.remote.rpc.registry.RoutingTable;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry;
import org.opendaylight.controller.remote.rpc.registry.gossip.Bucket;
import org.opendaylight.controller.sal.connector.api.RpcRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
        final Map<String, DOMRpcIdentifier> globalRpcsFromModule = rpcRegistry.getGlobalRpcsFromModules();
        final RoutingTable table = rpcRegistry.getLocalBucket().getData();
        final Set<String> globalRpc = new HashSet<>(table.getRoutes().size());
        for (final RpcRouter.RouteIdentifier<?, ?, ?> route : table.getRoutes()) {
            if (route.getType() != null && globalRpcsFromModule.containsKey(route.getType().toString())) {
                final StringBuffer builder = new StringBuffer();
                if (route.getRoute() != null) {
                    builder.append(ROUTE_CONSTANT).append(route.getRoute().toString());
                }
                builder.append(NAME_CONSTANT)
                        .append(route.getType() != null ? route.getType().toString() : NULL_CONSTANT);
                globalRpc.add(builder.toString());
            }
        }
        if(log.isDebugEnabled()) {
            log.debug("Locally registered global RPCs {}", globalRpc);
        }
        return globalRpc;
    }

    @Override
    public Set<String> getLocalRegisteredRoutedRpc() {
        final Map<String, DOMRpcIdentifier> globalRpcsFromModules = rpcRegistry.getGlobalRpcsFromModules();
        final RoutingTable table = rpcRegistry.getLocalBucket().getData();
        final Set<String> routedRpc = new HashSet<>(table.getRoutes().size());
        for(final RpcRouter.RouteIdentifier<?, ?, ?> route : table.getRoutes()){
            if(route.getRoute() != null) {
                if (route.getType() != null && globalRpcsFromModules.containsKey(route.getType().toString())) {
                    // it is global rpc, so skip
                    continue;
                }
                final StringBuffer builder = new StringBuffer(ROUTE_CONSTANT);
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
        final RoutingTable localTable = rpcRegistry.getLocalBucket().getData();
        // Get all RPCs from local bucket
        final Map<String, String> rpcMap = new HashMap<>(getRpcMemberMapByName(localTable, name, LOCAL_CONSTANT));

        // Get all RPCs from remote bucket
        final Map<Address, Bucket<RoutingTable>> buckets = rpcRegistry.getRemoteBuckets();
        for(final Address address : buckets.keySet()) {
            final RoutingTable table = buckets.get(address).getData();
            rpcMap.putAll(getRpcMemberMapByName(table, name, address.toString()));
        }
        if(log.isDebugEnabled()) {
            log.debug("list of RPCs {} searched by name {}", rpcMap, name);
        }
        return rpcMap;
    }

    @Override
    public Map<String, String> findRpcByRoute(final String routeId) {
        final RoutingTable localTable = rpcRegistry.getLocalBucket().getData();
        final Map<String, String> rpcMap = new HashMap<>(getRpcMemberMapByRoute(localTable, routeId, LOCAL_CONSTANT));

        final Map<Address, Bucket<RoutingTable>> buckets = rpcRegistry.getRemoteBuckets();
        for(final Address address : buckets.keySet()) {
            final RoutingTable table = buckets.get(address).getData();
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
        final Set<RpcRouter.RouteIdentifier<?, ?, ?>> routes = table.getRoutes();
        final Map<String, String> rpcMap = new HashMap<>(routes.size());
        for(final RpcRouter.RouteIdentifier<?, ?, ?> route : table.getRoutes()){
            if(route.getRoute() != null) {
                final String routeString = route.getRoute().toString();
                if(routeString.contains(routeName)) {
                    final StringBuilder builder = new StringBuilder(ROUTE_CONSTANT);
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
        final Set<RpcRouter.RouteIdentifier<?, ?, ?>> routes = table.getRoutes();
        final Map<String, String> rpcMap = new HashMap<>(routes.size());
        for(final RpcRouter.RouteIdentifier<?, ?, ?> route : routes){
            if(route.getType() != null) {
                final String type = route.getType().toString();
                if(type.contains(name)) {
                    final StringBuilder builder = new StringBuilder(ROUTE_CONSTANT);
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