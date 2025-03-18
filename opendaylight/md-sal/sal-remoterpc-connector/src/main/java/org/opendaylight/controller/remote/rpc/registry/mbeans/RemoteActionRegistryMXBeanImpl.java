/*
 * Copyright (c) 2019 Nordix Foundation.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc.registry.mbeans;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.pekko.actor.Address;
import org.opendaylight.controller.remote.rpc.registry.ActionRoutingTable;
import org.opendaylight.controller.remote.rpc.registry.gossip.Bucket;
import org.opendaylight.controller.remote.rpc.registry.gossip.BucketStoreAccess;
import org.opendaylight.mdsal.dom.api.DOMActionInstance;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

public class RemoteActionRegistryMXBeanImpl extends AbstractRegistryMXBean<ActionRoutingTable, DOMActionInstance>
        implements RemoteActionRegistryMXBean {
    public RemoteActionRegistryMXBeanImpl(final BucketStoreAccess actionRegistryAccess, final Duration timeout) {
        super("RemoteActionRegistry", "RemoteActionBroker", actionRegistryAccess, timeout);
    }

    @Override
    public Set<String> getLocalRegisteredAction() {
        ActionRoutingTable table = localData();
        Set<String> routedAction = new HashSet<>(table.getItems().size());
        for (DOMActionInstance route : table.getItems()) {
            final YangInstanceIdentifier actionPath = YangInstanceIdentifier.of(route.getType().lastNodeIdentifier());
            if (!actionPath.isEmpty()) {
                routedAction.add(ROUTE_CONSTANT + actionPath + NAME_CONSTANT + route.getType());
            }
        }

        log.debug("Locally registered routed RPCs {}", routedAction);
        return routedAction;
    }

    @Override
    public Map<String, String> findActionByName(final String name) {
        ActionRoutingTable localTable = localData();
        // Get all Actions from local bucket
        Map<String, String> rpcMap = new HashMap<>(getActionMemberMapByName(localTable, name, LOCAL_CONSTANT));

        // Get all Actions from remote bucket
        Map<Address, Bucket<ActionRoutingTable>> buckets = remoteBuckets();
        for (Map.Entry<Address, Bucket<ActionRoutingTable>> entry : buckets.entrySet()) {
            ActionRoutingTable table = entry.getValue().getData();
            rpcMap.putAll(getActionMemberMapByName(table, name, entry.getKey().toString()));
        }

        log.debug("list of Actions {} searched by name {}", rpcMap, name);
        return rpcMap;
    }

    @Override
    public Map<String, String> findActionByRoute(final String routeId) {
        ActionRoutingTable localTable = localData();
        Map<String, String> rpcMap = new HashMap<>(getActionMemberMapByAction(localTable, routeId, LOCAL_CONSTANT));

        Map<Address, Bucket<ActionRoutingTable>> buckets = remoteBuckets();
        for (Map.Entry<Address, Bucket<ActionRoutingTable>> entry : buckets.entrySet()) {
            ActionRoutingTable table = entry.getValue().getData();
            rpcMap.putAll(getActionMemberMapByAction(table, routeId, entry.getKey().toString()));
        }

        log.debug("list of Actions {} searched by route {}", rpcMap, routeId);
        return rpcMap;
    }

    /**
     * Search if the routing table route String contains routeName.
     */
    private static Map<String, String> getActionMemberMapByAction(final ActionRoutingTable table,
                                                                  final String routeName, final String address) {
        Collection<DOMActionInstance> routes = table.getItems();
        Map<String, String> actionMap = new HashMap<>(routes.size());
        for (DOMActionInstance route : routes) {
            final YangInstanceIdentifier actionPath = YangInstanceIdentifier.of(route.getType().lastNodeIdentifier());
            if (!actionPath.isEmpty()) {
                String routeString = actionPath.toString();
                if (routeString.contains(routeName)) {
                    actionMap.put(ROUTE_CONSTANT + routeString + NAME_CONSTANT + route.getType(), address);
                }
            }
        }
        return actionMap;
    }

    /**
     * Search if the routing table route type contains name.
     */
    private static Map<String, String> getActionMemberMapByName(final ActionRoutingTable table, final String name,
                                                                final String address) {
        Collection<DOMActionInstance> routes = table.getItems();
        Map<String, String> actionMap = new HashMap<>(routes.size());
        for (DOMActionInstance route : routes) {
            final YangInstanceIdentifier actionPath = YangInstanceIdentifier.of(route.getType().lastNodeIdentifier());
            if (!actionPath.isEmpty()) {
                String type = route.getType().toString();
                if (type.contains(name)) {
                    actionMap.put(ROUTE_CONSTANT + actionPath + NAME_CONSTANT + type, address);
                }
            }
        }
        return actionMap;
    }

    @Override
    public String getBucketVersions() {
        return bucketVersions();
    }
}
