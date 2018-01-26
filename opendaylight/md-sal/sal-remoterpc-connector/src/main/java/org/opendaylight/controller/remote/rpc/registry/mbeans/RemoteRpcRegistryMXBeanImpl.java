/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc.registry.mbeans;

import akka.actor.Address;
import akka.util.Timeout;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.opendaylight.controller.md.sal.common.util.jmx.AbstractMXBean;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.remote.rpc.registry.RoutingTable;
import org.opendaylight.controller.remote.rpc.registry.gossip.Bucket;
import org.opendaylight.controller.remote.rpc.registry.gossip.BucketStoreAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;


public class RemoteRpcRegistryMXBeanImpl extends AbstractMXBean implements RemoteRpcRegistryMXBean {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private static final String LOCAL_CONSTANT = "local";

    private static final String ROUTE_CONSTANT = "route:";

    private static final String NAME_CONSTANT = " | name:";

    private final BucketStoreAccess rpcRegistryAccess;
    private final Timeout timeout;

    public RemoteRpcRegistryMXBeanImpl(final BucketStoreAccess rpcRegistryAccess, Timeout timeout) {
        super("RemoteRpcRegistry", "RemoteRpcBroker", null);
        this.rpcRegistryAccess = rpcRegistryAccess;
        this.timeout = timeout;
        registerMBean();
    }

    @SuppressWarnings({"unchecked", "checkstyle:IllegalCatch", "rawtypes"})
    private RoutingTable getLocalData() {
        try {
            return (RoutingTable) Await.result((Future) rpcRegistryAccess.getLocalData(), timeout.duration());
        } catch (Exception e) {
            throw new RuntimeException("getLocalData failed", e);
        }
    }

    @SuppressWarnings({"unchecked", "checkstyle:IllegalCatch", "rawtypes"})
    private Map<Address, Bucket<RoutingTable>> getRemoteBuckets() {
        try {
            return (Map<Address, Bucket<RoutingTable>>) Await.result((Future)rpcRegistryAccess.getRemoteBuckets(),
                    timeout.duration());
        } catch (Exception e) {
            throw new RuntimeException("getRemoteBuckets failed", e);
        }
    }

    @Override
    public Set<String> getGlobalRpc() {
        RoutingTable table = getLocalData();
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
        RoutingTable table = getLocalData();
        Set<String> routedRpc = new HashSet<>(table.getRoutes().size());
        for (DOMRpcIdentifier route : table.getRoutes()) {
            if (!route.getContextReference().isEmpty()) {
                StringBuilder builder = new StringBuilder(ROUTE_CONSTANT);
                builder.append(route.getContextReference().toString()).append(NAME_CONSTANT).append(route.getType());
                routedRpc.add(builder.toString());
            }
        }

        log.debug("Locally registered routed RPCs {}", routedRpc);
        return routedRpc;
    }

    @Override
    public Map<String, String> findRpcByName(final String name) {
        RoutingTable localTable = getLocalData();
        // Get all RPCs from local bucket
        Map<String, String> rpcMap = new HashMap<>(getRpcMemberMapByName(localTable, name, LOCAL_CONSTANT));

        // Get all RPCs from remote bucket
        Map<Address, Bucket<RoutingTable>> buckets = getRemoteBuckets();
        for (Entry<Address, Bucket<RoutingTable>> entry : buckets.entrySet()) {
            RoutingTable table = entry.getValue().getData();
            rpcMap.putAll(getRpcMemberMapByName(table, name, entry.getKey().toString()));
        }

        log.debug("list of RPCs {} searched by name {}", rpcMap, name);
        return rpcMap;
    }

    @Override
    public Map<String, String> findRpcByRoute(final String routeId) {
        RoutingTable localTable = getLocalData();
        Map<String, String> rpcMap = new HashMap<>(getRpcMemberMapByRoute(localTable, routeId, LOCAL_CONSTANT));

        Map<Address, Bucket<RoutingTable>> buckets = getRemoteBuckets();
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
                    StringBuilder builder = new StringBuilder(ROUTE_CONSTANT);
                    builder.append(routeString).append(NAME_CONSTANT).append(route.getType());
                    rpcMap.put(builder.toString(), address);
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
                    StringBuilder builder = new StringBuilder(ROUTE_CONSTANT);
                    builder.append(route.getContextReference()).append(NAME_CONSTANT).append(type);
                    rpcMap.put(builder.toString(), address);
                }
            }
        }
        return rpcMap;
    }

    @Override
    @SuppressWarnings({"unchecked", "checkstyle:IllegalCatch", "rawtypes"})
    public String getBucketVersions() {
        try {
            return Await.result((Future)rpcRegistryAccess.getBucketVersions(), timeout.duration()).toString();
        } catch (Exception e) {
            throw new RuntimeException("getVersions failed", e);
        }
    }
}
