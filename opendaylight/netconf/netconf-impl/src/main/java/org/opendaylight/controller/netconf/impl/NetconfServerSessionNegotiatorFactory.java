/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl;

import static org.opendaylight.controller.netconf.mapping.api.NetconfOperationProvider.NetconfOperationProviderUtil.getNetconfSessionIdForReporting;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import io.netty.channel.Channel;
import io.netty.util.Timer;
import io.netty.util.concurrent.Promise;
import java.util.Set;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfServerSessionPreferences;
import org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.impl.mapping.CapabilityProvider;
import org.opendaylight.controller.netconf.impl.osgi.SessionMonitoringService;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationProvider;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceSnapshot;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessage;
import org.opendaylight.protocol.framework.SessionListenerFactory;
import org.opendaylight.protocol.framework.SessionNegotiator;
import org.opendaylight.protocol.framework.SessionNegotiatorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetconfServerSessionNegotiatorFactory implements SessionNegotiatorFactory<NetconfHelloMessage, NetconfServerSession, NetconfServerSessionListener> {

    public static final Set<String> DEFAULT_BASE_CAPABILITIES = ImmutableSet.of(
            XmlNetconfConstants.URN_IETF_PARAMS_NETCONF_BASE_1_0,
            XmlNetconfConstants.URN_IETF_PARAMS_NETCONF_BASE_1_1,
            XmlNetconfConstants.URN_IETF_PARAMS_NETCONF_CAPABILITY_EXI_1_0
    );

    private final Timer timer;

    private final SessionIdProvider idProvider;
    private final NetconfOperationProvider netconfOperationProvider;
    private final long connectionTimeoutMillis;
    private final CommitNotifier commitNotificationProducer;
    private final SessionMonitoringService monitoringService;
    private static final Logger LOG = LoggerFactory.getLogger(NetconfServerSessionNegotiatorFactory.class);
    private final Set<String> baseCapabilities;

    // TODO too many params, refactor
    public NetconfServerSessionNegotiatorFactory(Timer timer, NetconfOperationProvider netconfOperationProvider,
                                                 SessionIdProvider idProvider, long connectionTimeoutMillis,
                                                 CommitNotifier commitNot,
                                                 SessionMonitoringService monitoringService) {
        this(timer, netconfOperationProvider, idProvider, connectionTimeoutMillis, commitNot, monitoringService, DEFAULT_BASE_CAPABILITIES);
    }

    // TODO too many params, refactor
    public NetconfServerSessionNegotiatorFactory(Timer timer, NetconfOperationProvider netconfOperationProvider,
                                                 SessionIdProvider idProvider, long connectionTimeoutMillis,
                                                 CommitNotifier commitNot,
                                                 SessionMonitoringService monitoringService, Set<String> baseCapabilities) {
        this.timer = timer;
        this.netconfOperationProvider = netconfOperationProvider;
        this.idProvider = idProvider;
        this.connectionTimeoutMillis = connectionTimeoutMillis;
        this.commitNotificationProducer = commitNot;
        this.monitoringService = monitoringService;
        this.baseCapabilities = validateBaseCapabilities(baseCapabilities);
    }

    private ImmutableSet<String> validateBaseCapabilities(final Set<String> baseCapabilities) {
        // Check base capabilities to be supported by the server
        Sets.SetView<String> unknownBaseCaps = Sets.difference(baseCapabilities, DEFAULT_BASE_CAPABILITIES);
        Preconditions.checkArgument(unknownBaseCaps.isEmpty(),
                "Base capabilities that will be supported by netconf server have to be subset of %s, unknown base capabilities: %s",
                DEFAULT_BASE_CAPABILITIES, unknownBaseCaps);

        ImmutableSet.Builder<String> b = ImmutableSet.builder();
        b.addAll(baseCapabilities);
        // Base 1.0 capability is supported by default
        b.add(XmlNetconfConstants.URN_IETF_PARAMS_NETCONF_BASE_1_0);
        return b.build();
    }

    /**
     *
     * @param defunctSessionListenerFactory will not be taken into account as session listener factory can
     *                                      only be created after snapshot is opened, thus this method constructs
     *                                      proper session listener factory.
     * @param channel Underlying channel
     * @param promise Promise to be notified
     * @return session negotiator
     */
    @Override
    public SessionNegotiator<NetconfServerSession> getSessionNegotiator(SessionListenerFactory<NetconfServerSessionListener> defunctSessionListenerFactory,
                                                                        Channel channel, Promise<NetconfServerSession> promise) {
        long sessionId = idProvider.getNextSessionId();
        NetconfOperationServiceSnapshot netconfOperationServiceSnapshot = netconfOperationProvider.openSnapshot(
                getNetconfSessionIdForReporting(sessionId));
        CapabilityProvider capabilityProvider = new CapabilityProviderImpl(netconfOperationServiceSnapshot);

        NetconfServerSessionPreferences proposal = null;
        try {
            proposal = new NetconfServerSessionPreferences(
                    createHelloMessage(sessionId, capabilityProvider), sessionId);
        } catch (NetconfDocumentedException e) {
            LOG.error("Unable to create hello mesage for session {} with capability provider {}", sessionId,capabilityProvider);
            throw new IllegalStateException(e);
        }

        NetconfServerSessionListenerFactory sessionListenerFactory = new NetconfServerSessionListenerFactory(
                commitNotificationProducer, monitoringService,
                netconfOperationServiceSnapshot, capabilityProvider);

        return new NetconfServerSessionNegotiator(proposal, promise, channel, timer,
                sessionListenerFactory.getSessionListener(), connectionTimeoutMillis);
    }

    private NetconfHelloMessage createHelloMessage(long sessionId, CapabilityProvider capabilityProvider) throws NetconfDocumentedException {
        return NetconfHelloMessage.createServerHello(Sets.union(capabilityProvider.getCapabilities(), baseCapabilities), sessionId);
    }

}
