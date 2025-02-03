/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;

import static java.util.Objects.requireNonNull;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.PoisonPill;
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

    private final String logName;
    private final DOMRpcProviderService rpcProvisionRegistry;
    private final RemoteOpsProviderConfig config;
    private final ActorSystem actorSystem;
    private final DOMRpcService rpcService;
    private final DOMActionProviderService actionProviderService;
    private final DOMActionService actionService;

    private ActorRef opsManager;

    public RemoteOpsProvider(final String logName, final ActorSystem actorSystem,
            final DOMRpcProviderService rpcProvisionRegistry, final DOMRpcService rpcService,
            final RemoteOpsProviderConfig config, final DOMActionProviderService actionProviderService,
            final DOMActionService actionService) {
        this.logName = requireNonNull(logName);
        this.actorSystem = requireNonNull(actorSystem);
        this.rpcProvisionRegistry = requireNonNull(rpcProvisionRegistry);
        this.rpcService = requireNonNull(rpcService);
        this.config = requireNonNull(config);
        this.actionProviderService = requireNonNull(actionProviderService);
        this.actionService = requireNonNull(actionService);
    }

    @Override
    public void close() {
        if (opsManager != null) {
            LOG.info("{}, Stopping Ops Manager at {}", logName, opsManager);
            opsManager.tell(PoisonPill.getInstance(), ActorRef.noSender());
            opsManager = null;
        }
    }

    public void start() {
        LOG.info("{}: Starting Remote Ops service...", logName);
        opsManager = actorSystem.actorOf(OpsManager.props(logName, rpcProvisionRegistry, rpcService, config,
                actionProviderService, actionService), config.getRpcManagerName());
        LOG.debug("Ops Manager started at {}", opsManager);
    }
}
