/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry.mbeans;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.pekko.actor.Address;
import org.opendaylight.controller.remote.rpc.registry.RoutingTable;
import org.opendaylight.controller.remote.rpc.registry.gossip.Bucket;
import org.opendaylight.controller.remote.rpc.registry.gossip.BucketStoreAccess;
import org.opendaylight.mdsal.dom.api.DOMRpcIdentifier;

public class RemoteRpcRegistryMXBeanImpl extends AbstractRegistryMXBean<RoutingTable, DOMRpcIdentifier>
        implements RemoteRpcRegistryMXBean {
    public RemoteRpcRegistryMXBeanImpl(final BucketStoreAccess rpcRegistryAccess, final Duration timeout) {
        super("RemoteRpcRegistry", "RemoteRpcBroker", rpcRegistryAccess, timeout);
    }

    @Override
    public Set<String> getGlobalRpc() {
        RoutingTable table = localData();
        Set<String> globalRpc = new HashSet<>(table.getItems().size());
        for (DOMRpcIdentifier route : table.getItems()) {
            if (route.getContextReference().isEmpty()) {
                globalRpc.add(route.getType().toString());
            }
        }

        log.debug("Locally registered global RPCs {}", globalRpc);
        return globalRpc;
    }

    @Override
    public Set<String> getLocalRegisteredRoutedRpc() {
        RoutingTable table = localData();
        Set<String> routedRpc = new HashSet<>(table.getItems().size());
        for (DOMRpcIdentifier route : table.getItems()) {
            if (!route.getContextReference().isEmpty()) {
                routedRpc.add(ROUTE_CONSTANT + route.getContextReference() + NAME_CONSTANT + route.getType());
            }
        }

        log.debug("Locally registered routed RPCs {}", routedRpc);
        return routedRpc;
    }

    @Override
    public Map<String, String> findRpcByName(final String name) {
        RoutingTable localTable = localData();
        // Get all RPCs from local bucket
        Map<String, String> rpcMap = new HashMap<>(getRpcMemberMapByName(localTable, name, LOCAL_CONSTANT));

        // Get all RPCs from remote bucket
        Map<Address, Bucket<RoutingTable>> buckets = remoteBuckets();
        for (Entry<Address, Bucket<RoutingTable>> entry : buckets.entrySet()) {
            RoutingTable table = entry.getValue().getData();
            rpcMap.putAll(getRpcMemberMapByName(table, name, entry.getKey().toString()));
        }

        log.debug("list of RPCs {} searched by name {}", rpcMap, name);
        return rpcMap;
    }

    @Override
    public Map<String, String> findRpcByRoute(final String routeId) {
        RoutingTable localTable = localData();
        Map<String, String> rpcMap = new HashMap<>(getRpcMemberMapByRoute(localTable, routeId, LOCAL_CONSTANT));

        Map<Address, Bucket<RoutingTable>> buckets = remoteBuckets();
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
        Set<DOMRpcIdentifier> routes = table.getItems();
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
    private static Map<String, String> getRpcMemberMapByName(final RoutingTable table, final String name,
                                                             final String address) {
        Set<DOMRpcIdentifier> routes = table.getItems();
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
        return bucketVersions();
    }
}
