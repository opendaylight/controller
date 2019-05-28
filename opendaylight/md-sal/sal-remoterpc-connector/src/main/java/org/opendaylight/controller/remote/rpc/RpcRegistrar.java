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
import org.opendaylight.controller.remote.rpc.registry.ActionRegistry;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry.Messages.UpdateRemoteEndpoints;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry.RemoteRpcEndpoint;
import org.opendaylight.controller.remote.rpc.registry.ActionRegistry.RemoteActionEndpoint;
import org.opendaylight.mdsal.dom.api.*;
import org.opendaylight.yangtools.concepts.ObjectRegistration;

/**
 * Actor handling registration of RPCs available on remote nodes with the local {@link DOMRpcProviderService}.
 *
 * @author Robert Varga
 */
final class RpcRegistrar extends AbstractUntypedActor {
    private final Map<Address, ObjectRegistration<DOMRpcImplementation>> rpcRegs = new HashMap<>();
    private final Map<Address, ObjectRegistration<DOMActionImplementation>> actionRegs = new HashMap<>();
    private final DOMRpcProviderService rpcProviderService;
    private final RemoteRpcProviderConfig config;
    private final DOMActionProviderService actionProviderService;

    RpcRegistrar(final RemoteRpcProviderConfig config, final DOMRpcProviderService rpcProviderService, final DOMActionProviderService actionProviderService) {
        this.config = Preconditions.checkNotNull(config);
        this.rpcProviderService = Preconditions.checkNotNull(rpcProviderService);
        this.actionProviderService = Preconditions.checkNotNull(actionProviderService);
    }

    public static Props props(final RemoteRpcProviderConfig config, final DOMRpcProviderService rpcProviderService, final DOMActionProviderService actionProviderService) {
        Preconditions.checkNotNull(rpcProviderService, "DOMRpcProviderService cannot be null");
        Preconditions.checkNotNull(actionProviderService, "DOMActionProviderService cannot be null");
        return Props.create(RpcRegistrar.class, config, rpcProviderService, actionProviderService);
    }

    @Override
    public void postStop() throws Exception {
//        rpcRegs.values().forEach(DOMRpcImplementationRegistration::close);
//        actionRegs.values().forEach(DOMActionImplementationRegistration::close);
        rpcRegs.clear();
        actionRegs.clear();

        super.postStop();
    }

    @Override
    protected void handleReceive(final Object message) {
        if (message instanceof RpcRegistry.Messages.UpdateRemoteEndpoints) {
            updateRemoteEndpoints(((UpdateRemoteEndpoints) message).getRpcEndpoints());
        } else if (message instanceof ActionRegistry.Messages.UpdateRemoteActionEndpoints){
            updateRemoteEndpoints(((UpdateRemoteEndpoints) message).getRpcEndpoints());
        }else {
            unknownMessage(message);
        }
    }

    private void updateRemoteEndpoints(final Map<Address, Optional<RemoteRpcEndpoint>> rpcEndpoints) {
        /*
         * Updating RPC providers is a two-step process. We first add the newly-discovered RPCs and then close
         * the old registration. This minimizes churn observed by listeners, as they will not observe RPC
         * unavailability which would occur if we were to do it the other way around.
         *
         * Note that when an RPC moves from one remote node to another, we also do not want to expose the gap,
         * hence we register all new implementations before closing all registrations.
         */
        final Collection<ObjectRegistration<DOMRpcImplementation>> prevRpcRegs = new ArrayList<>(rpcEndpoints.size());

        for (Entry<Address, Optional<RemoteRpcEndpoint>> e : rpcEndpoints.entrySet()) {
            LOG.debug("Updating RPC registrations for {}", e.getKey());

            final ObjectRegistration<DOMRpcImplementation> prevRpcReg;
            final Optional<RemoteRpcEndpoint> maybeEndpoint = e.getValue();
            if (maybeEndpoint.isPresent()) {
                final RemoteRpcEndpoint endpoint = maybeEndpoint.get();
                final RemoteRpcImplementation impl = new RemoteRpcImplementation(endpoint.getRouter(), config);
                prevRpcReg = rpcRegs.put(e.getKey(), rpcProviderService.registerRpcImplementation(impl,
                        endpoint.getRpcs()));
            } else {
                prevRpcReg = rpcRegs.remove(e.getKey());
            }

            if (prevRpcReg != null) {
                prevRpcRegs.add(prevRpcReg);
            }
        }

//        for (DOMRpcImplementationRegistration<?> r : prevRpcRegs) {
//            r.close();
//        }

    }

    private void updateRemoteActionEndpoints(final Map<Address, Optional<ActionRegistry.RemoteActionEndpoint>> actionEndpoints) {
        /*
         * Updating RPC providers is a two-step process. We first add the newly-discovered RPCs and then close
         * the old registration. This minimizes churn observed by listeners, as they will not observe RPC
         * unavailability which would occur if we were to do it the other way around.
         *
         * Note that when an RPC moves from one remote node to another, we also do not want to expose the gap,
         * hence we register all new implementations before closing all registrations.
         */
        final Collection<ObjectRegistration<DOMActionImplementation>> prevActionRegs = new ArrayList<>(actionEndpoints.size());

        for (Entry<Address, Optional<RemoteActionEndpoint>> e : actionEndpoints.entrySet()) {
            LOG.debug("Updating Action registrations for {}", e.getKey());

            final ObjectRegistration<DOMActionImplementation> prevActionReg;
            final Optional<RemoteActionEndpoint> maybeEndpoint = e.getValue();
            if (maybeEndpoint.isPresent()) {
                final RemoteActionEndpoint endpoint = maybeEndpoint.get();
                final RemoteActionImplementation impl = new RemoteActionImplementation(endpoint.getRouter(), config);
                prevActionReg = actionRegs.put(e.getKey(), actionProviderService.registerActionImplementation(impl, endpoint.getActions()));
            } else {
                prevActionReg = actionRegs.remove(e.getKey());
            }

            if (prevActionReg != null) {
                prevActionRegs.add(prevActionReg);
            }
        }

//        for (DOMActionImplementationRegistration<?> r : prevActionRegs) {
//            r.close();
//        }
    }
}
