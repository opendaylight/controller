/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.Set;
import org.apache.pekko.actor.ActorRef;
import org.opendaylight.controller.remote.rpc.registry.ActionRegistry;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry.AddOrUpdateRoutes;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry.RemoveRoutes;
import org.opendaylight.mdsal.dom.api.DOMActionAvailabilityExtension;
import org.opendaylight.mdsal.dom.api.DOMActionImplementation;
import org.opendaylight.mdsal.dom.api.DOMActionInstance;
import org.opendaylight.mdsal.dom.api.DOMRpcAvailabilityListener;
import org.opendaylight.mdsal.dom.api.DOMRpcIdentifier;
import org.opendaylight.mdsal.dom.api.DOMRpcImplementation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link DOMRpcAvailabilityListener} reacting to RPC implementations different than
 * {@link RemoteRpcImplementation} or {@link RemoteActionImplementation}.
 * The knowledge of such implementations is forwarded to {@link RpcRegistry} and {@link ActionRegistry},
 * which is responsible for advertising their presence to other nodes.
 */
final class OpsListener implements DOMRpcAvailabilityListener, DOMActionAvailabilityExtension.AvailabilityListener {
    private static final Logger LOG = LoggerFactory.getLogger(OpsListener.class);

    private final ActorRef rpcRegistry;
    private final ActorRef actionRegistry;

    OpsListener(final ActorRef rpcRegistry, final ActorRef actionRegistry) {
        this.rpcRegistry = requireNonNull(rpcRegistry);
        this.actionRegistry = requireNonNull(actionRegistry);
    }

    @Override
    public void onRpcAvailable(final Collection<DOMRpcIdentifier> rpcs) {
        checkArgument(rpcs != null, "Input Collection of DOMRpcIdentifier can not be null.");
        LOG.debug("Adding registration for [{}]", rpcs);

        rpcRegistry.tell(new AddOrUpdateRoutes(rpcs), ActorRef.noSender());
    }

    @Override
    public void onRpcUnavailable(final Collection<DOMRpcIdentifier> rpcs) {
        checkArgument(rpcs != null, "Input Collection of DOMRpcIdentifier can not be null.");

        LOG.debug("Removing registration for [{}]", rpcs);
        rpcRegistry.tell(new RemoveRoutes(rpcs), ActorRef.noSender());
    }

    @Override
    public boolean acceptsImplementation(final DOMRpcImplementation impl) {
        return !(impl instanceof RemoteRpcImplementation);
    }

    @Override
    public boolean acceptsImplementation(final DOMActionImplementation impl) {
        return !(impl instanceof RemoteActionImplementation);
    }

    @Override
    public void onActionsChanged(final Set<DOMActionInstance> removed, final Set<DOMActionInstance> added) {
        LOG.debug("adding registration for [{}]", added);
        LOG.debug("removing registration for [{}]", removed);
        actionRegistry.tell(new ActionRegistry.UpdateActions(added, removed), ActorRef.noSender());
    }
}
