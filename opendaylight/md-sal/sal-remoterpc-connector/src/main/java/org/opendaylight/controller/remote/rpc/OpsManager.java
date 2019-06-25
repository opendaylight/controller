/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc;

import static java.util.Objects.requireNonNull;

import akka.actor.ActorRef;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.common.actor.AbstractUntypedActor;
import org.opendaylight.controller.remote.rpc.registry.RpcRegistry;
import org.opendaylight.mdsal.dom.api.DOMActionProviderService;
import org.opendaylight.mdsal.dom.api.DOMActionService;
import org.opendaylight.mdsal.dom.api.DOMRpcProviderService;
import org.opendaylight.mdsal.dom.api.DOMRpcService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import scala.concurrent.duration.FiniteDuration;

/**
 * This class acts as a supervisor, creates all the actors, resumes them, if an exception is thrown. It also registers
 * {@link OpsListener} with the local {@link DOMRpcService}.
 */
public class OpsManager extends AbstractUntypedActor {
    private final DOMRpcProviderService rpcProvisionRegistry;
    private final RemoteOpsProviderConfig config;
    private final DOMRpcService rpcServices;
    private DOMActionProviderService actionProvisionRegistry;
    private DOMActionService actionService;

    private ListenerRegistration<OpsListener> listenerReg;
    private ActorRef opsInvoker;
    private ActorRef actionRegistry;
    private ActorRef rpcRegistry;
    private ActorRef opsRegistrar;

    OpsManager(final DOMRpcProviderService rpcProvisionRegistry, final DOMRpcService rpcServices,
               final RemoteOpsProviderConfig config, final DOMActionProviderService actionProviderService,
               final DOMActionService actionService) {
        this.rpcProvisionRegistry = requireNonNull(rpcProvisionRegistry);
        this.rpcServices = requireNonNull(rpcServices);
        this.config = requireNonNull(config);
        this.actionProvisionRegistry = requireNonNull(actionProviderService);
        this.actionService = requireNonNull(actionService);
    }

    public static Props props(final DOMRpcProviderService rpcProvisionRegistry, final DOMRpcService rpcServices,
                              final RemoteOpsProviderConfig config,
                              final DOMActionProviderService actionProviderService,
                              final DOMActionService actionService) {
        requireNonNull(rpcProvisionRegistry, "RpcProviderService can not be null!");
        requireNonNull(rpcServices, "RpcService can not be null!");
        requireNonNull(config, "RemoteOpsProviderConfig can not be null!");
        requireNonNull(actionProviderService, "ActionProviderService can not be null!");
        requireNonNull(actionService, "ActionService can not be null!");
        return Props.create(OpsManager.class, rpcProvisionRegistry, rpcServices, config,
                actionProviderService, actionService);
    }

    @Override
    public void preStart() throws Exception {
        super.preStart();

        opsInvoker = getContext().actorOf(OpsInvoker.props(rpcServices, actionService)
                .withMailbox(config.getMailBoxName()), config.getRpcBrokerName());
        LOG.debug("Listening for RPC invocation requests with {}", opsInvoker);

        opsRegistrar = getContext().actorOf(OpsRegistrar.props(config, rpcProvisionRegistry, actionProvisionRegistry)
                .withMailbox(config.getMailBoxName()), config.getRpcRegistrarName());
        LOG.debug("Registering remote RPCs with {}", opsRegistrar);

        rpcRegistry = getContext().actorOf(RpcRegistry.props(config, opsInvoker, opsRegistrar)
                .withMailbox(config.getMailBoxName()), config.getRpcRegistryName());
        LOG.debug("Propagating RPC information with {}", rpcRegistry);

        actionRegistry = getContext().actorOf(RpcRegistry.props(config, opsInvoker, opsRegistrar)
                .withMailbox(config.getMailBoxName()), config.getActionRegistryName());
        LOG.debug("Propagating RPC information with {}", actionRegistry);

        final OpsListener opsListener = new OpsListener(rpcRegistry, actionRegistry);
        LOG.debug("Registering local availability listener {}", opsListener);
        listenerReg = rpcServices.registerRpcListener(opsListener);
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
        return new OneForOneStrategy(10, FiniteDuration.create(1, TimeUnit.MINUTES), t -> {
            LOG.error("An exception happened actor will be resumed", t);
            return SupervisorStrategy.resume();
        });
    }
}
