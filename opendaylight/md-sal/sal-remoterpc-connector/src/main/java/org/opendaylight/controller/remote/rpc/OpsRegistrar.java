/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;

import akka.actor.Address;
import akka.actor.Props;
import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.controller.remote.rpc.registry.ActionRegistry;
import org.opendaylight.controller.remote.rpc.registry.ActionRegistry.Messages.UpdateRemoteActionEndpoints;
import org.opendaylight.controller.remote.rpc.registry.ActionRegistry.RemoteActionEndpoint;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry.Messages.UpdateRemoteEndpoints;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry.RemoteRpcEndpoint;
import org.opendaylight.mdsal.dom.api.DOMActionImplementation;
import org.opendaylight.mdsal.dom.api.DOMActionProviderService;
import org.opendaylight.mdsal.dom.api.DOMRpcImplementation;
import org.opendaylight.mdsal.dom.api.DOMRpcProviderService;
import org.opendaylight.yangtools.concepts.ObjectRegistration;


/**
 * Actor handling registration of RPCs and Actions available on remote nodes with the local
 * {@link DOMRpcProviderService} and {@link DOMActionProviderService}.
 *
 * @author Robert Varga
 */
final class OpsRegistrar extends AbstractUntypedActor {
    private final Map<Address, ObjectRegistration<DOMRpcImplementation>> rpcRegs = new HashMap<>();
    private final Map<Address, ObjectRegistration<DOMActionImplementation>> actionRegs = new HashMap<>();
    private final DOMRpcProviderService rpcProviderService;
    private final RemoteOpsProviderConfig config;
    private final DOMActionProviderService actionProviderService;

    OpsRegistrar(final RemoteOpsProviderConfig config, final DOMRpcProviderService rpcProviderService,
                 final DOMActionProviderService actionProviderService) {
        this.config = Preconditions.checkNotNull(config);
        this.rpcProviderService = Preconditions.checkNotNull(rpcProviderService);
        this.actionProviderService = Preconditions.checkNotNull(actionProviderService);
    }

    public static Props props(final RemoteOpsProviderConfig config, final DOMRpcProviderService rpcProviderService,
                              final DOMActionProviderService actionProviderService) {
        Preconditions.checkNotNull(rpcProviderService, "DOMRpcProviderService cannot be null");
        Preconditions.checkNotNull(actionProviderService, "DOMActionProviderService cannot be null");
        return Props.create(OpsRegistrar.class, config, rpcProviderService, actionProviderService);
    }

    @Override
    public void postStop() throws Exception {
        rpcRegs.clear();
        actionRegs.clear();

        super.postStop();
    }

    @Override
    protected void handleReceive(final Object message) {
        if (message instanceof RpcRegistry.Messages.UpdateRemoteEndpoints) {
            LOG.debug("Handling updateRemoteEndpoints message");
            updateRemoteRpcEndpoints(((UpdateRemoteEndpoints) message).getRpcEndpoints());
        } else if (message instanceof ActionRegistry.Messages.UpdateRemoteActionEndpoints) {
            LOG.debug("Handling updateRemoteActionEndpoints message");
            updateRemoteActionEndpoints(((UpdateRemoteActionEndpoints) message).getActionEndpoints());
        } else {
            unknownMessage(message);
        }
    }

    private void updateRemoteRpcEndpoints(final Map<Address, Optional<RemoteRpcEndpoint>> rpcEndpoints) {
        /*
         * Updating RPC providers is a two-step process. We first add the newly-discovered RPCs and then close
         * the old registration. This minimizes churn observed by listeners, as they will not observe RPC
         * unavailability which would occur if we were to do it the other way around.
         *
         * Note that when an RPC moves from one remote node to another, we also do not want to expose the gap,
         * hence we register all new implementations before closing all registrations.
         */
        for (Entry<Address, Optional<RemoteRpcEndpoint>> e : rpcEndpoints.entrySet()) {
            LOG.debug("Updating RPC registrations for {}", e.getKey());

            final Optional<RemoteRpcEndpoint> maybeEndpoint = e.getValue();
            if (maybeEndpoint.isPresent()) {
                final RemoteRpcEndpoint endpoint = maybeEndpoint.get();
                final RemoteOpsImplementation impl = new RemoteOpsImplementation(endpoint.getRouter(), config);
                rpcRegs.put(e.getKey(), rpcProviderService.registerRpcImplementation(impl,
                        endpoint.getRpcs()));
            } else {
                rpcRegs.remove(e.getKey());
            }

        }

    }

    /**
     * Updates the action endpoints, Adding new registrations first before removing previous registrations.
     */
    private void updateRemoteActionEndpoints(final Map<Address,
            Optional<ActionRegistry.RemoteActionEndpoint>> actionEndpoints) {
        /*
         * Updating Action providers is a two-step process. We first add the newly-discovered RPCs and then close
         * the old registration. This minimizes churn observed by listeners, as they will not observe RPC
         * unavailability which would occur if we were to do it the other way around.
         *
         * Note that when an Action moves from one remote node to another, we also do not want to expose the gap,
         * hence we register all new implementations before closing all registrations.
         */

        for (Entry<Address, Optional<RemoteActionEndpoint>> e : actionEndpoints.entrySet()) {
            LOG.debug("Updating Action registrations for {}", e.getKey());

            final Optional<RemoteActionEndpoint> maybeEndpoint = e.getValue();
            if (maybeEndpoint.isPresent()) {
                final RemoteActionEndpoint endpoint = maybeEndpoint.get();
                final RemoteOpsImplementation impl = new RemoteOpsImplementation(endpoint.getRouter(), config);
                actionRegs.put(e.getKey(),
                        actionProviderService.registerActionImplementation(impl, endpoint.getActions()));
            } else {
                actionRegs.remove(e.getKey());
            }
        }
    }
}
