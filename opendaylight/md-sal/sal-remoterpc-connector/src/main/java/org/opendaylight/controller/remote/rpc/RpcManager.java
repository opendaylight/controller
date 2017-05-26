/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc;

import akka.actor.ActorRef;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import scala.concurrent.duration.Duration;

/**
 * This class acts as a supervisor, creates all the actors, resumes them, if an exception is thrown. It also registers
 * {@link RpcListener} with the local {@link DOMRpcService}.
 */
public class RpcManager extends AbstractUntypedActor {
    private final DOMRpcProviderService rpcProvisionRegistry;
    private final RemoteRpcProviderConfig config;
    private final DOMRpcService rpcServices;

    private ListenerRegistration<RpcListener> listenerReg;
    private ActorRef rpcInvoker;
    private ActorRef rpcRegistry;
    private ActorRef rpcRegistrar;

    private RpcManager(final DOMRpcProviderService rpcProvisionRegistry,
                       final DOMRpcService rpcServices,
                       final RemoteRpcProviderConfig config) {
        this.rpcProvisionRegistry = Preconditions.checkNotNull(rpcProvisionRegistry);
        this.rpcServices = Preconditions.checkNotNull(rpcServices);
        this.config = Preconditions.checkNotNull(config);
    }

    public static Props props(final DOMRpcProviderService rpcProvisionRegistry, final DOMRpcService rpcServices,
            final RemoteRpcProviderConfig config) {
        Preconditions.checkNotNull(rpcProvisionRegistry, "RpcProviderService can not be null!");
        Preconditions.checkNotNull(rpcServices, "RpcService can not be null!");
        Preconditions.checkNotNull(config, "RemoteRpcProviderConfig can not be null!");
        return Props.create(RpcManager.class, rpcProvisionRegistry, rpcServices, config);
    }

    @Override
    public void preStart() throws Exception {
        super.preStart();

        rpcInvoker = getContext().actorOf(RpcInvoker.props(rpcServices)
            .withMailbox(config.getMailBoxName()), config.getRpcBrokerName());
        LOG.debug("Listening for RPC invocation requests with {}", rpcInvoker);

        rpcRegistrar = getContext().actorOf(RpcRegistrar.props(config, rpcProvisionRegistry)
            .withMailbox(config.getMailBoxName()), config.getRpcRegistrarName());
        LOG.debug("Registering remote RPCs with {}", rpcRegistrar);

        rpcRegistry = getContext().actorOf(RpcRegistry.props(config, rpcInvoker, rpcRegistrar)
                .withMailbox(config.getMailBoxName()), config.getRpcRegistryName());
        LOG.debug("Propagating RPC information with {}", rpcRegistry);

        final RpcListener rpcListener = new RpcListener(rpcRegistry);
        LOG.debug("Registering local availabitility listener {}", rpcListener);
        listenerReg = rpcServices.registerRpcListener(rpcListener);
    }

    @Override
    public void postStop() throws Exception {
        if (listenerReg != null) {
            listenerReg.close();
            listenerReg = null;
        }

        super.postStop();
    }

    @Override
    protected void handleReceive(final Object message) {
        unknownMessage(message);
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return new OneForOneStrategy(10, Duration.create("1 minute"), t -> {
            LOG.error("An exception happened actor will be resumed", t);
            return SupervisorStrategy.resume();
        });
    }
}
