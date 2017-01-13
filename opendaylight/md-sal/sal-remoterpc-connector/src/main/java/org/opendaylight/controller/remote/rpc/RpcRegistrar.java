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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcImplementationRegistration;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry.Messages.UpdateRemoteEndpoints;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry.RemoteRpcEndpoint;

/**
 * Actor handling registration of RPCs available on remote nodes with the local {@link DOMRpcProviderService}.
 *
 * @author Robert Varga
 */
final class RpcRegistrar extends AbstractUntypedActor {
    private final Map<Address, DOMRpcImplementationRegistration<?>> regs = new HashMap<>();
    private final DOMRpcProviderService rpcProviderService;
    private final RemoteRpcProviderConfig config;

    RpcRegistrar(final RemoteRpcProviderConfig config, final DOMRpcProviderService rpcProviderService) {
        this.config = Preconditions.checkNotNull(config);
        this.rpcProviderService = Preconditions.checkNotNull(rpcProviderService);
    }

    public static Props props(final RemoteRpcProviderConfig config, final DOMRpcProviderService rpcProviderService) {
        Preconditions.checkNotNull(rpcProviderService, "DOMRpcProviderService cannot be null");
        return Props.create(RpcRegistrar.class, config, rpcProviderService);
    }

    @Override
    public void postStop() throws Exception {
        regs.values().forEach(DOMRpcImplementationRegistration::close);
        regs.clear();

        super.postStop();
    }

    @Override
    protected void handleReceive(final Object message) throws Exception {
        if (message instanceof UpdateRemoteEndpoints) {
            updateRemoteEndpoints(((UpdateRemoteEndpoints) message).getEndpoints());
        } else {
            unknownMessage(message);
        }
    }

    private void updateRemoteEndpoints(final Map<Address, Optional<RemoteRpcEndpoint>> endpoints) {
        /*
         * Updating RPC providers is a two-step process. We first add the newly-discovered RPCs and then close
         * the old registration. This minimizes churn observed by listeners, as they will not observe RPC
         * unavailability which would occur if we were to do it the other way around.
         *
         * Note that when an RPC moves from one remote node to another, we also do not want to expose the gap,
         * hence we register all new implementations before closing all registrations.
         */
        final Collection<DOMRpcImplementationRegistration<?>> prevRegs = new ArrayList<>(endpoints.size());

        for (Entry<Address, Optional<RemoteRpcEndpoint>> e : endpoints.entrySet()) {
            LOG.debug("Updating RPC registrations for {}", e.getKey());

            final DOMRpcImplementationRegistration<?> prevReg;
            final Optional<RemoteRpcEndpoint> maybeEndpoint = e.getValue();
            if (maybeEndpoint.isPresent()) {
                final RemoteRpcEndpoint endpoint = maybeEndpoint.get();
                final RemoteRpcImplementation impl = new RemoteRpcImplementation(endpoint.getRouter(), config);
                prevReg = regs.put(e.getKey(), rpcProviderService.registerRpcImplementation(impl,
                    endpoint.getRpcs()));
            } else {
                prevReg = regs.remove(e.getKey());
            }

            if (prevReg != null) {
                prevRegs.add(prevReg);
            }
        }

        for (DOMRpcImplementationRegistration<?> r : prevRegs) {
            r.close();
        }
    }
}
