/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl;

import java.util.Set;

import org.opendaylight.controller.netconf.api.NetconfServerSessionPreferences;
import org.opendaylight.controller.netconf.impl.mapping.CapabilityProvider;
import org.opendaylight.controller.netconf.impl.osgi.NetconfOperationServiceFactoryListener;
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
    private final NetconfOperationServiceFactoryListener factoriesListener;
    private final long connectionTimeoutMillis;

    public NetconfServerSessionNegotiatorFactory(Timer timer, NetconfOperationServiceFactoryListener factoriesListener,
            SessionIdProvider idProvider, long connectionTimeoutMillis) {
        this.timer = timer;
        this.factoriesListener = factoriesListener;
        this.idProvider = idProvider;
        this.connectionTimeoutMillis = connectionTimeoutMillis;
    }

    @Override
    public SessionNegotiator<NetconfServerSession> getSessionNegotiator(
            SessionListenerFactory<NetconfServerSessionListener> sessionListenerFactory, Channel channel,
            Promise<NetconfServerSession> promise) {
        long sessionId = idProvider.getNextSessionId();

        NetconfServerSessionPreferences proposal = new NetconfServerSessionPreferences(createHelloMessage(sessionId),
                sessionId);
        return new NetconfServerSessionNegotiator(proposal, promise, channel, timer,
                sessionListenerFactory.getSessionListener(), connectionTimeoutMillis);
    }

    private NetconfHelloMessage createHelloMessage(long sessionId) {
        CapabilityProvider capabilityProvider = new CapabilityProviderImpl(factoriesListener.getSnapshot(sessionId));
        return NetconfHelloMessage.createServerHello(Sets.union(capabilityProvider.getCapabilities(), DEFAULT_CAPABILITIES), sessionId);
    }
}
