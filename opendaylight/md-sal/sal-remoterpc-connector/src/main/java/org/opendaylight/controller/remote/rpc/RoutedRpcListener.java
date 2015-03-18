/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc;


import akka.actor.ActorRef;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChange;
import org.opendaylight.controller.md.sal.common.api.routing.RouteChangeListener;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry;
import org.opendaylight.controller.sal.connector.api.RpcRouter;
import org.opendaylight.controller.sal.core.api.RpcRoutingContext;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoutedRpcListener implements RouteChangeListener<RpcRoutingContext, YangInstanceIdentifier>{
    private static final Logger LOG = LoggerFactory.getLogger(RoutedRpcListener.class);
    private final ActorRef rpcRegistry;

    public RoutedRpcListener(final ActorRef rpcRegistry) {
        Preconditions.checkNotNull(rpcRegistry, "rpc registry actor should not be null");
        this.rpcRegistry = rpcRegistry;
    }

    @Override
    public void onRouteChange(final RouteChange<RpcRoutingContext, YangInstanceIdentifier> routeChange) {
        final Map<RpcRoutingContext, Set<YangInstanceIdentifier>> announcements = routeChange.getAnnouncements();
        if(announcements != null && announcements.size() > 0){
            announce(getRouteIdentifiers(announcements));
        }

        final Map<RpcRoutingContext, Set<YangInstanceIdentifier>> removals = routeChange.getRemovals();
        if(removals != null && removals.size() > 0 ) {
            remove(getRouteIdentifiers(removals));
        }
    }

    /**
     *
     * @param announcements
     */
    private void announce(final Set<RpcRouter.RouteIdentifier<?, ?, ?>> announcements) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("Announcing [{}]", announcements);
        }
        final RpcRegistry.Messages.AddOrUpdateRoutes addRpcMsg =
                new RpcRegistry.Messages.AddOrUpdateRoutes(new ArrayList<>(announcements));
        rpcRegistry.tell(addRpcMsg, ActorRef.noSender());
    }

    /**
     *
     * @param removals
     */
    private void remove(final Set<RpcRouter.RouteIdentifier<?, ?, ?>> removals){
        if(LOG.isDebugEnabled()) {
            LOG.debug("Removing [{}]", removals);
        }
        final RpcRegistry.Messages.RemoveRoutes removeRpcMsg =
                new RpcRegistry.Messages.RemoveRoutes(new ArrayList<>(removals));
        rpcRegistry.tell(removeRpcMsg, ActorRef.noSender());
    }

    /**
     *
     * @param changes
     * @return
     */
    private Set<RpcRouter.RouteIdentifier<?, ?, ?>> getRouteIdentifiers(
            final Map<RpcRoutingContext, Set<YangInstanceIdentifier>> changes) {

        final Set<RpcRouter.RouteIdentifier<?, ?, ?>> routeIdSet = new HashSet<>();
        for (final RpcRoutingContext context : changes.keySet()){
            for (final YangInstanceIdentifier instanceId : changes.get(context)){
                final RouteIdentifierImpl routeId = new RouteIdentifierImpl(null, context.getRpc(), instanceId);
                routeIdSet.add(routeId);
            }
        }
        return routeIdSet;
    }
}
