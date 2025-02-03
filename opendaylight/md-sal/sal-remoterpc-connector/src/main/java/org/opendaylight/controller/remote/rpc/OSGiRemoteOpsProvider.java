/*
 * Copyright (c) 2020 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.remote.rpc;

import java.util.concurrent.atomic.AtomicLong;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.PoisonPill;
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

@Component(configurationPid = "org.opendaylight.controller.remoterpc")
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
    private static final AtomicLong INCARNATION = new AtomicLong();

    private ActorRef opsManager;

    @Activate
    public OSGiRemoteOpsProvider(@Reference final ActorSystemProvider actorSystemProvider,
            @Reference final DOMRpcProviderService rpcProviderService, @Reference final DOMRpcService rpcService,
            @Reference final DOMActionProviderService actionProviderService,
            @Reference final DOMActionService actionService, final Config config) {
        LOG.info("Remote Operations service starting");
        final var actorSystem = actorSystemProvider.getActorSystem();
        final var opsConfig = RemoteOpsProviderConfig.newInstance(actorSystem.name(),
            config.metricCapture(), config.boundedMailboxCapacity());

        opsManager = actorSystem.actorOf(
            OpsManager.props(Long.toUnsignedString(INCARNATION.incrementAndGet(), 16), rpcProviderService, rpcService,
                opsConfig, actionProviderService, actionService), opsConfig.getRpcManagerName());
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
