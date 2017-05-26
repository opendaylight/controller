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
import java.util.Collection;
import javax.annotation.Nonnull;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcAvailabilityListener;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementation;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry.Messages.AddOrUpdateRoutes;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry.Messages.RemoveRoutes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link DOMRpcAvailabilityListener} reacting to RPC implementations different than {@link RemoteRpcImplementation}.
 * The knowledge of such implementations is forwarded to {@link RpcRegistry}, which is responsible for advertising
 * their presence to other nodes.
 */
final class RpcListener implements DOMRpcAvailabilityListener {
    private static final Logger LOG = LoggerFactory.getLogger(RpcListener.class);

    private final ActorRef rpcRegistry;

    RpcListener(final ActorRef rpcRegistry) {
        this.rpcRegistry = Preconditions.checkNotNull(rpcRegistry);
    }

    @Override
    public void onRpcAvailable(@Nonnull final Collection<DOMRpcIdentifier> rpcs) {
        Preconditions.checkArgument(rpcs != null, "Input Collection of DOMRpcIdentifier can not be null.");
        LOG.debug("Adding registration for [{}]", rpcs);

        rpcRegistry.tell(new AddOrUpdateRoutes(rpcs), ActorRef.noSender());
    }

    @Override
    public void onRpcUnavailable(@Nonnull final Collection<DOMRpcIdentifier> rpcs) {
        Preconditions.checkArgument(rpcs != null, "Input Collection of DOMRpcIdentifier can not be null.");

        LOG.debug("Removing registration for [{}]", rpcs);
        rpcRegistry.tell(new RemoveRoutes(rpcs), ActorRef.noSender());
    }

    @Override
    public boolean acceptsImplementation(final DOMRpcImplementation impl) {
        return !(impl instanceof RemoteRpcImplementation);
    }
}
