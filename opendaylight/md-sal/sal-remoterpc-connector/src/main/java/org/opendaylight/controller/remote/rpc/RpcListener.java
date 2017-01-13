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
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcAvailabilityListener;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementation;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry.Messages.AddOrUpdateRoutes;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry.Messages.RemoveRoutes;
import org.opendaylight.controller.sal.connector.api.RpcRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class RpcListener implements DOMRpcAvailabilityListener {

    private static final Logger LOG = LoggerFactory.getLogger(RpcListener.class);
    private final ActorRef rpcRegistry;

    RpcListener(final ActorRef rpcRegistry) {
        this.rpcRegistry = rpcRegistry;
    }

    @Override
    public void onRpcAvailable(@Nonnull final Collection<DOMRpcIdentifier> rpcs) {
        Preconditions.checkArgument(rpcs != null, "Input Collection of DOMRpcIdentifier can not be null.");
        LOG.debug("Adding registration for [{}]", rpcs);

        final List<RpcRouter.RouteIdentifier<?,?,?>> routeIds = new ArrayList<>(rpcs.size());

        for (final DOMRpcIdentifier rpc : rpcs) {
            // FIXME: Refactor routeId and message to use DOMRpcIdentifier directly.
            final RpcRouter.RouteIdentifier<?,?,?> routeId =
                    new RouteIdentifierImpl(null, rpc.getType().getLastComponent(), rpc.getContextReference());
            routeIds.add(routeId);
        }
        rpcRegistry.tell(new AddOrUpdateRoutes(routeIds), ActorRef.noSender());
    }

    @Override
    public void onRpcUnavailable(@Nonnull final Collection<DOMRpcIdentifier> rpcs) {
        Preconditions.checkArgument(rpcs != null, "Input Collection of DOMRpcIdentifier can not be null.");

        LOG.debug("Removing registration for [{}]", rpcs);

        final List<RpcRouter.RouteIdentifier<?,?,?>> routeIds = new ArrayList<>(rpcs.size());
        for (final DOMRpcIdentifier rpc : rpcs) {
            final RpcRouter.RouteIdentifier<?,?,?> routeId =
                    new RouteIdentifierImpl(null, rpc.getType().getLastComponent(), rpc.getContextReference());
            routeIds.add(routeId);
        }
        rpcRegistry.tell(new RemoveRoutes(routeIds), ActorRef.noSender());
    }

    @Override
    public boolean acceptsImplementation(final DOMRpcImplementation impl) {
        return !(impl instanceof RemoteRpcImplementation);
    }
}
