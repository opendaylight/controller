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
import java.util.Map.Entry;
import java.util.Set;
import org.opendaylight.controller.md.sal.common.util.jmx.AbstractMXBean;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.remote.rpc.registry.RoutingTable;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry;
import org.opendaylight.controller.remote.rpc.registry.gossip.Bucket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RemoteRpcRegistryMXBeanImpl extends AbstractMXBean implements RemoteRpcRegistryMXBean {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private static final String LOCAL_CONSTANT = "local";

    private static final String ROUTE_CONSTANT = "route:";

    private static final String NAME_CONSTANT = " | name:";

    private final RpcRegistry rpcRegistry;

    public RemoteRpcRegistryMXBeanImpl(final RpcRegistry rpcRegistry) {
        super("RemoteRpcRegistry", "RemoteRpcBroker", null);
        this.rpcRegistry = rpcRegistry;
        registerMBean();
    }

    @Override
    public Set<String> getGlobalRpc() {
        RoutingTable table = rpcRegistry.getLocalData();
        Set<String> globalRpc = new HashSet<>(table.getRoutes().size());
        for (DOMRpcIdentifier route : table.getRoutes()) {
            if (route.getContextReference().isEmpty()) {
                globalRpc.add(route.getType().toString());
            }
        }

        log.debug("Locally registered global RPCs {}", globalRpc);
        return globalRpc;
    }

    @Override
    public Set<String> getLocalRegisteredRoutedRpc() {
        RoutingTable table = rpcRegistry.getLocalData();
        Set<String> routedRpc = new HashSet<>(table.getRoutes().size());
        for (DOMRpcIdentifier route : table.getRoutes()) {
            if (!route.getContextReference().isEmpty()) {
                routedRpc.add(ROUTE_CONSTANT + route.getContextReference() + NAME_CONSTANT + route.getType());
            }
        }

        log.debug("Locally registered routed RPCs {}", routedRpc);
        return routedRpc;
    }

    @Override
    public Map<String, String> findRpcByName(final String name) {
        RoutingTable localTable = rpcRegistry.getLocalData();
        // Get all RPCs from local bucket
        Map<String, String> rpcMap = new HashMap<>(getRpcMemberMapByName(localTable, name, LOCAL_CONSTANT));

        // Get all RPCs from remote bucket
        Map<Address, Bucket<RoutingTable>> buckets = rpcRegistry.getRemoteBuckets();
        for (Entry<Address, Bucket<RoutingTable>> entry : buckets.entrySet()) {
            RoutingTable table = entry.getValue().getData();
            rpcMap.putAll(getRpcMemberMapByName(table, name, entry.getKey().toString()));
        }

        log.debug("list of RPCs {} searched by name {}", rpcMap, name);
        return rpcMap;
    }

    @Override
    public Map<String, String> findRpcByRoute(final String routeId) {
        RoutingTable localTable = rpcRegistry.getLocalData();
        Map<String, String> rpcMap = new HashMap<>(getRpcMemberMapByRoute(localTable, routeId, LOCAL_CONSTANT));

        Map<Address, Bucket<RoutingTable>> buckets = rpcRegistry.getRemoteBuckets();
        for (Entry<Address, Bucket<RoutingTable>> entry : buckets.entrySet()) {
            RoutingTable table = entry.getValue().getData();
            rpcMap.putAll(getRpcMemberMapByRoute(table, routeId, entry.getKey().toString()));
        }

        log.debug("list of RPCs {} searched by route {}", rpcMap, routeId);
        return rpcMap;
    }

    /**
     * Search if the routing table route String contains routeName.
     */
    private static Map<String,String> getRpcMemberMapByRoute(final RoutingTable table, final String routeName,
                                                      final String address) {
        Set<DOMRpcIdentifier> routes = table.getRoutes();
        Map<String, String> rpcMap = new HashMap<>(routes.size());
        for (DOMRpcIdentifier route : routes) {
            if (!route.getContextReference().isEmpty()) {
                String routeString = route.getContextReference().toString();
                if (routeString.contains(routeName)) {
                    rpcMap.put(ROUTE_CONSTANT + routeString + NAME_CONSTANT + route.getType(), address);
                }
            }
        }
        return rpcMap;
    }

    /**
     * Search if the routing table route type contains name.
     */
    private static Map<String, String>  getRpcMemberMapByName(final RoutingTable table, final String name,
                                                       final String address) {
        Set<DOMRpcIdentifier> routes = table.getRoutes();
        Map<String, String> rpcMap = new HashMap<>(routes.size());
        for (DOMRpcIdentifier route : routes) {
            if (!route.getContextReference().isEmpty()) {
                String type = route.getType().toString();
                if (type.contains(name)) {
                    rpcMap.put(ROUTE_CONSTANT + route.getContextReference() + NAME_CONSTANT + type, address);
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
