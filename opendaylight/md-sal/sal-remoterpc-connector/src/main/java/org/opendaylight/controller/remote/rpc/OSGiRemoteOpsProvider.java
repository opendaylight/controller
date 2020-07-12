/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import org.opendaylight.controller.cluster.ActorSystemProvider;
import org.opendaylight.mdsal.dom.api.DOMActionProviderService;
import org.opendaylight.mdsal.dom.api.DOMActionService;
import org.opendaylight.mdsal.dom.api.DOMRpcProviderService;
import org.opendaylight.mdsal.dom.api.DOMRpcService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, configurationPid = "org.opendaylight.controller.remoterpc")
@Designate(ocd = OSGiRemoteOpsProvider.Config.class)
public final class OSGiRemoteOpsProvider {
    @ObjectClassDefinition()
    public @interface Config {
        @AttributeDefinition(name = "enable-metric-capture")
        boolean metricCapture() default true;
        @AttributeDefinition(name = "bounded-mailbox-capacity")
        int boundedMailboxCapacity() default 1000;
    }

    private static final Logger LOG = LoggerFactory.getLogger(OSGiRemoteOpsProvider.class);

    @Reference
    ActorSystemProvider actorSystemProvider = null;
    @Reference
    DOMRpcProviderService rpcProviderService = null;
    @Reference
    DOMRpcService rpcService = null;
    @Reference
    DOMActionProviderService actionProviderService = null;
    @Reference
    DOMActionService actionService = null;

    private ActorRef opsManager;

    @Activate
    void activate(final Config config) {
        LOG.info("Remote Operations service starting");
        final ActorSystem actorSystem = actorSystemProvider.getActorSystem();
        final RemoteOpsProviderConfig opsConfig = RemoteOpsProviderConfig.newInstance(actorSystem.name(),
            config.metricCapture(), config.boundedMailboxCapacity());

        opsManager = actorSystem.actorOf(OpsManager.props(rpcProviderService, rpcService, opsConfig,
                actionProviderService, actionService), opsConfig.getRpcManagerName());
        LOG.debug("Ops Manager started at {}", opsManager);
        LOG.info("Remote Operations service started");
    }

    @Deactivate
    void deactivate() {
        LOG.info("Remote Operations service stopping");
        LOG.debug("Stopping Ops Manager at {}", opsManager);
        opsManager.tell(PoisonPill.getInstance(), ActorRef.noSender());
        opsManager = null;
        LOG.info("Remote Operations services stopped");
    }
}
