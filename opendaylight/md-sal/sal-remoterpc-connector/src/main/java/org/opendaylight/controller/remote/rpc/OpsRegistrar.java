/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;

import static java.util.Objects.requireNonNull;

import akka.actor.Address;
import akka.actor.Props;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.controller.remote.rpc.registry.ActionRegistry.Messages.UpdateRemoteActionEndpoints;
import org.opendaylight.controller.remote.rpc.registry.ActionRegistry.RemoteActionEndpoint;
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
 */
final class OpsRegistrar extends AbstractUntypedActor {
    private final Map<Address, ObjectRegistration<DOMRpcImplementation>> rpcRegs = new HashMap<>();
    private final Map<Address, ObjectRegistration<DOMActionImplementation>> actionRegs = new HashMap<>();
    private final DOMRpcProviderService rpcProviderService;
    private final RemoteOpsProviderConfig config;
    private final DOMActionProviderService actionProviderService;

    OpsRegistrar(final RemoteOpsProviderConfig config, final DOMRpcProviderService rpcProviderService,
                 final DOMActionProviderService actionProviderService) {
        this.config = requireNonNull(config);
        this.rpcProviderService = requireNonNull(rpcProviderService);
        this.actionProviderService = requireNonNull(actionProviderService);
    }

    public static Props props(final RemoteOpsProviderConfig config, final DOMRpcProviderService rpcProviderService,
                              final DOMActionProviderService actionProviderService) {
        return Props.create(OpsRegistrar.class, requireNonNull(config),
            requireNonNull(rpcProviderService, "DOMRpcProviderService cannot be null"),
            requireNonNull(actionProviderService, "DOMActionProviderService cannot be null"));
    }

    @Override
    public void postStop() throws Exception {
        rpcRegs.values().forEach(ObjectRegistration::close);
        rpcRegs.clear();
        actionRegs.values().forEach(ObjectRegistration::close);
        actionRegs.clear();

        super.postStop();
    }

    @Override
    protected void handleReceive(final Object message) {
        if (message instanceof UpdateRemoteEndpoints updateEndpoints) {
            LOG.debug("Handling updateRemoteEndpoints message");
            updateRemoteRpcEndpoints(updateEndpoints.getRpcEndpoints());
        } else if (message instanceof UpdateRemoteActionEndpoints updateEndpoints) {
            LOG.debug("Handling updateRemoteActionEndpoints message");
            updateRemoteActionEndpoints(updateEndpoints.getActionEndpoints());
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
        final Collection<ObjectRegistration<?>> prevRegs = new ArrayList<>(rpcEndpoints.size());

        for (Entry<Address, Optional<RemoteRpcEndpoint>> e : rpcEndpoints.entrySet()) {
            LOG.debug("Updating RPC registrations for {}", e.getKey());

            final ObjectRegistration<DOMRpcImplementation> prevReg;
            final Optional<RemoteRpcEndpoint> maybeEndpoint = e.getValue();
            if (maybeEndpoint.isPresent()) {
                final RemoteRpcEndpoint endpoint = maybeEndpoint.get();
                final RemoteRpcImplementation impl = new RemoteRpcImplementation(endpoint.getRouter(), config);
                prevReg = rpcRegs.put(e.getKey(), rpcProviderService.registerRpcImplementation(impl,
                    endpoint.getRpcs()));
            } else {
                prevReg = rpcRegs.remove(e.getKey());
            }

            if (prevReg != null) {
                prevRegs.add(prevReg);
            }
        }

        prevRegs.forEach(ObjectRegistration::close);
    }

    /**
     * Updates the action endpoints, Adding new registrations first before removing previous registrations.
     */
    private void updateRemoteActionEndpoints(final Map<Address, Optional<RemoteActionEndpoint>> actionEndpoints) {
        /*
         * Updating Action providers is a two-step process. We first add the newly-discovered RPCs and then close
         * the old registration. This minimizes churn observed by listeners, as they will not observe RPC
         * unavailability which would occur if we were to do it the other way around.
         *
         * Note that when an Action moves from one remote node to another, we also do not want to expose the gap,
         * hence we register all new implementations before closing all registrations.
         */
        final Collection<ObjectRegistration<?>> prevRegs = new ArrayList<>(actionEndpoints.size());

        for (Entry<Address, Optional<RemoteActionEndpoint>> e : actionEndpoints.entrySet()) {
            LOG.debug("Updating action registrations for {}", e.getKey());

            final ObjectRegistration<DOMActionImplementation> prevReg;
            final Optional<RemoteActionEndpoint> maybeEndpoint = e.getValue();
            if (maybeEndpoint.isPresent()) {
                final RemoteActionEndpoint endpoint = maybeEndpoint.get();
                final RemoteActionImplementation impl = new RemoteActionImplementation(endpoint.getRouter(), config);
                prevReg = actionRegs.put(e.getKey(), actionProviderService.registerActionImplementation(impl,
                    endpoint.getActions()));
            } else {
                prevReg = actionRegs.remove(e.getKey());
            }

            if (prevReg != null) {
                prevRegs.add(prevReg);
            }
        }

        prevRegs.forEach(ObjectRegistration::close);
    }
}
