/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl;

import static org.opendaylight.controller.netconf.mapping.api.NetconfOperationProvider.NetconfOperationProviderUtil.getNetconfSessionIdForReporting;

import java.util.Set;

import org.opendaylight.controller.netconf.api.NetconfServerSessionPreferences;
import org.opendaylight.controller.netconf.impl.mapping.CapabilityProvider;
import org.opendaylight.controller.netconf.impl.osgi.SessionMonitoringService;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationProvider;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceSnapshot;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessage;
import org.opendaylight.controller.netconf.util.xml.XmlNetconfConstants;
import org.opendaylight.protocol.framework.SessionListenerFactory;
import org.opendaylight.protocol.framework.SessionNegotiator;
import org.opendaylight.protocol.framework.SessionNegotiatorFactory;

import com.google.common.collect.Sets;

import io.netty.channel.Channel;
import io.netty.util.Timer;
import io.netty.util.concurrent.Promise;

public class NetconfServerSessionNegotiatorFactory implements SessionNegotiatorFactory<NetconfHelloMessage, NetconfServerSession, NetconfServerSessionListener> {

    private static final Set<String> DEFAULT_CAPABILITIES = Sets.newHashSet(
            XmlNetconfConstants.URN_IETF_PARAMS_XML_NS_NETCONF_BASE_1_0,
            XmlNetconfConstants.URN_IETF_PARAMS_NETCONF_CAPABILITY_EXI_1_0);

    private final Timer timer;

    private final SessionIdProvider idProvider;
    private final NetconfOperationProvider netconfOperationProvider;
    private final long connectionTimeoutMillis;
    private final DefaultCommitNotificationProducer commitNotificationProducer;
    private final SessionMonitoringService monitoringService;

    public NetconfServerSessionNegotiatorFactory(Timer timer, NetconfOperationProvider netconfOperationProvider,
                                                 SessionIdProvider idProvider, long connectionTimeoutMillis,
                                                 DefaultCommitNotificationProducer commitNot, SessionMonitoringService monitoringService) {
        this.timer = timer;
        this.netconfOperationProvider = netconfOperationProvider;
        this.idProvider = idProvider;
        this.connectionTimeoutMillis = connectionTimeoutMillis;
        this.commitNotificationProducer = commitNot;
        this.monitoringService = monitoringService;
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

        NetconfServerSessionPreferences proposal = new NetconfServerSessionPreferences(
                createHelloMessage(sessionId, capabilityProvider), sessionId);

        NetconfServerSessionListenerFactory sessionListenerFactory = new NetconfServerSessionListenerFactory(
                commitNotificationProducer, monitoringService,
                netconfOperationServiceSnapshot, capabilityProvider);

        return new NetconfServerSessionNegotiator(proposal, promise, channel, timer,
                sessionListenerFactory.getSessionListener(), connectionTimeoutMillis);
    }

    private NetconfHelloMessage createHelloMessage(long sessionId, CapabilityProvider capabilityProvider) {
        return NetconfHelloMessage.createServerHello(Sets.union(capabilityProvider.getCapabilities(), DEFAULT_CAPABILITIES), sessionId);
    }

}
