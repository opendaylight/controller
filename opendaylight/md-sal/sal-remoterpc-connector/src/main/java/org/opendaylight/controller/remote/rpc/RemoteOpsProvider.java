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
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import org.opendaylight.mdsal.dom.api.DOMActionProviderService;
import org.opendaylight.mdsal.dom.api.DOMActionService;
import org.opendaylight.mdsal.dom.api.DOMRpcProviderService;
import org.opendaylight.mdsal.dom.api.DOMRpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the base class which initialize all the actors, listeners and
 * default RPc implementation so remote invocation of rpcs.
 */
public class RemoteOpsProvider implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteOpsProvider.class);

    private final DOMRpcProviderService rpcProvisionRegistry;
    private final RemoteOpsProviderConfig config;
    private final ActorSystem actorSystem;
    private final DOMRpcService rpcService;
    private final DOMActionProviderService actionProvisionRegistry;
    private final DOMActionService actionService;

    private ActorRef opsManager;

    public RemoteOpsProvider(final ActorSystem actorSystem, final DOMRpcProviderService rpcProvisionRegistry,
                             final DOMRpcService rpcService, final RemoteOpsProviderConfig config,
                             final DOMActionProviderService actionProviderService, DOMActionService actionService) {
        this.actorSystem = requireNonNull(actorSystem);
        this.rpcProvisionRegistry = requireNonNull(rpcProvisionRegistry);
        this.rpcService = requireNonNull(rpcService);
        this.config = requireNonNull(config);
        this.actionProvisionRegistry = requireNonNull(actionProviderService);
        this.actionService = requireNonNull(actionService);
    }

    @Override
    public void close() {
        if (opsManager != null) {
            LOG.info("Stopping Ops Manager at {}", opsManager);
            opsManager.tell(PoisonPill.getInstance(), ActorRef.noSender());
            opsManager = null;
        }
    }

    public void start() {
        LOG.info("Starting Remote Ops service...");
        opsManager = actorSystem.actorOf(OpsManager.props(rpcProvisionRegistry, rpcService, config,
                actionProvisionRegistry, actionService), config.getRpcManagerName());
        LOG.debug("Ops Manager started at {}", opsManager);
    }
}
