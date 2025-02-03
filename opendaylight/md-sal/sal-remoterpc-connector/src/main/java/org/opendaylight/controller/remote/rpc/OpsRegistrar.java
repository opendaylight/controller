/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.pekko.actor.Address;
import org.apache.pekko.actor.Props;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.controller.remote.rpc.registry.ActionRegistry.RemoteActionEndpoint;
import org.opendaylight.controller.remote.rpc.registry.ActionRegistry.UpdateRemoteActionEndpoints;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry.RemoteRpcEndpoint;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry.UpdateRemoteEndpoints;
import org.opendaylight.mdsal.dom.api.DOMActionProviderService;
import org.opendaylight.mdsal.dom.api.DOMRpcProviderService;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Actor handling registration of RPCs and Actions available on remote nodes with the local
 * {@link DOMRpcProviderService} and {@link DOMActionProviderService}.
 */
final class OpsRegistrar extends AbstractUntypedActor {
    private static final Logger LOG = LoggerFactory.getLogger(OpsRegistrar.class);

    private final Map<Address, Registration> rpcRegs = new HashMap<>();
    private final Map<Address, Registration> actionRegs = new HashMap<>();
    private final DOMRpcProviderService rpcProviderService;
    private final RemoteOpsProviderConfig config;
    private final DOMActionProviderService actionProviderService;

    OpsRegistrar(final String logName, final RemoteOpsProviderConfig config,
            final DOMRpcProviderService rpcProviderService, final DOMActionProviderService actionProviderService) {
        super(logName);
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
        rpcRegs.values().forEach(Registration::close);
        rpcRegs.clear();
        actionRegs.values().forEach(Registration::close);
        actionRegs.clear();

        super.postStop();
    }

    @Override
    protected void handleReceive(final Object message) {
        if (message instanceof UpdateRemoteEndpoints updateEndpoints) {
            updateRemoteRpcEndpoints(updateEndpoints.getRpcEndpoints());
        } else if (message instanceof UpdateRemoteActionEndpoints updateEndpoints) {
            updateRemoteActionEndpoints(updateEndpoints.getActionEndpoints());
        } else {
            unknownMessage(message);
        }
    }

    private void updateRemoteRpcEndpoints(final Map<Address, Optional<RemoteRpcEndpoint>> rpcEndpoints) {
        LOG.debug("{}: Handling updateRemoteEndpoints message", logName);

        /*
         * Updating RPC providers is a two-step process. We first add the newly-discovered RPCs and then close
         * the old registration. This minimizes churn observed by listeners, as they will not observe RPC
         * unavailability which would occur if we were to do it the other way around.
         *
         * Note that when an RPC moves from one remote node to another, we also do not want to expose the gap,
         * hence we register all new implementations before closing all registrations.
         */
        final var prevRegs = new ArrayList<Registration>(rpcEndpoints.size());

        for (var entry : rpcEndpoints.entrySet()) {
            LOG.debug("{}: Updating RPC registrations for {}", logName, entry.getKey());

            final Registration prevReg;
            final var maybeEndpoint = entry.getValue();
            if (maybeEndpoint.isPresent()) {
                final var endpoint = maybeEndpoint.orElseThrow();
                final var impl = new RemoteRpcImplementation(endpoint.getRouter(), config);
                prevReg = rpcRegs.put(entry.getKey(), rpcProviderService.registerRpcImplementation(impl,
                    endpoint.getRpcs()));
            } else {
                prevReg = rpcRegs.remove(entry.getKey());
            }

            if (prevReg != null) {
                prevRegs.add(prevReg);
            }
        }

        prevRegs.forEach(Registration::close);
    }

    /**
     * Updates the action endpoints, Adding new registrations first before removing previous registrations.
     */
    private void updateRemoteActionEndpoints(final Map<Address, Optional<RemoteActionEndpoint>> actionEndpoints) {
        LOG.debug("{}: Handling updateRemoteActionEndpoints message", logName);

        /*
         * Updating Action providers is a two-step process. We first add the newly-discovered RPCs and then close
         * the old registration. This minimizes churn observed by listeners, as they will not observe RPC
         * unavailability which would occur if we were to do it the other way around.
         *
         * Note that when an Action moves from one remote node to another, we also do not want to expose the gap,
         * hence we register all new implementations before closing all registrations.
         */
        final var prevRegs = new ArrayList<Registration>(actionEndpoints.size());

        for (var entry : actionEndpoints.entrySet()) {
            LOG.debug("{}: Updating action registrations for {}", logName, entry.getKey());

            final Registration prevReg;
            final var maybeEndpoint = entry.getValue();
            if (maybeEndpoint.isPresent()) {
                final RemoteActionEndpoint endpoint = maybeEndpoint.orElseThrow();
                final RemoteActionImplementation impl = new RemoteActionImplementation(endpoint.getRouter(), config);
                prevReg = actionRegs.put(entry.getKey(), actionProviderService.registerActionImplementation(impl,
                    endpoint.getActions()));
            } else {
                prevReg = actionRegs.remove(entry.getKey());
            }

            if (prevReg != null) {
                prevRegs.add(prevReg);
            }
        }

        prevRegs.forEach(Registration::close);
    }
}
