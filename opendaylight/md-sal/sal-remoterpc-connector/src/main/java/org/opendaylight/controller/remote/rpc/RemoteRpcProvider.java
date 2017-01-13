/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.remote.rpc;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcProviderService;
import org.opendaylight.controller.md.sal.dom.api.DOMRpcService;
import org.opendaylight.controller.sal.core.api.Broker;
import org.opendaylight.controller.sal.core.api.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the base class which initialize all the actors, listeners and
 * default RPc implementation so remote invocation of rpcs.
 */
public class RemoteRpcProvider implements AutoCloseable, Provider {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteRpcProvider.class);

    private final DOMRpcProviderService rpcProvisionRegistry;
    private final RemoteRpcProviderConfig config;
    private final ActorSystem actorSystem;

    private DOMRpcService rpcService;
    private ActorRef rpcManager;

    public RemoteRpcProvider(final ActorSystem actorSystem, final DOMRpcProviderService rpcProvisionRegistry,
            final RemoteRpcProviderConfig config) {
        this.actorSystem = actorSystem;
        this.rpcProvisionRegistry = rpcProvisionRegistry;
        this.config = Preconditions.checkNotNull(config);
    }

    public void setRpcService(final DOMRpcService rpcService) {
        this.rpcService = rpcService;
    }

    @Override
    public void close() {
        if (rpcManager != null) {
            LOG.info("Stopping RPC Manager at {}", rpcManager);
            rpcManager.tell(PoisonPill.getInstance(), ActorRef.noSender());
            rpcManager = null;
        }
    }

    @Override
    public void onSessionInitiated(final Broker.ProviderSession session) {
        rpcService = session.getService(DOMRpcService.class);
        start();
    }

    @Override
    public Collection<ProviderFunctionality> getProviderFunctionality() {
        return ImmutableSet.of();
    }

    public void start() {
        LOG.info("Starting Remote RPC service...");
        rpcManager = actorSystem.actorOf(RpcManager.props(rpcProvisionRegistry, rpcService, config),
                config.getRpcManagerName());
        LOG.debug("RPC Manager started at {}", rpcManager);
    }
}
