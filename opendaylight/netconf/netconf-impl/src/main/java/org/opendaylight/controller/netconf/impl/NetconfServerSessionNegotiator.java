/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl;

import io.netty.channel.Channel;
import io.netty.util.Timer;
import io.netty.util.concurrent.Promise;

import java.net.InetSocketAddress;

import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.api.NetconfServerSessionPreferences;
import org.opendaylight.controller.netconf.impl.util.AdditionalHeaderUtil;
import org.opendaylight.controller.netconf.util.AbstractNetconfSessionNegotiator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class NetconfServerSessionNegotiator extends
        AbstractNetconfSessionNegotiator<NetconfServerSessionPreferences, NetconfServerSession, NetconfServerSessionListener> {

    static final Logger logger = LoggerFactory.getLogger(NetconfServerSessionNegotiator.class);

    protected NetconfServerSessionNegotiator(NetconfServerSessionPreferences sessionPreferences,
            Promise<NetconfServerSession> promise, Channel channel, Timer timer, NetconfServerSessionListener sessionListener,
            long connectionTimeoutMillis) {
        super(sessionPreferences, promise, channel, timer, sessionListener, connectionTimeoutMillis);
    }

    @Override
    protected NetconfServerSession getSession(NetconfServerSessionListener sessionListener, Channel channel, NetconfMessage message) {
        Optional<String> additionalHeader = message.getAdditionalHeader();

        AdditionalHeader parsedHeader;
        if (additionalHeader.isPresent()) {
            parsedHeader = AdditionalHeaderUtil.fromString(additionalHeader.get());
        } else {
            parsedHeader = new AdditionalHeader("unknown", ((InetSocketAddress)channel.localAddress()).getHostString(),
                    "tcp", "client");
        }
        logger.debug("Additional header from hello parsed as {} from {}", parsedHeader, additionalHeader);

        return new NetconfServerSession(sessionListener, channel, sessionPreferences.getSessionId(), parsedHeader);
    }

    public static class AdditionalHeader {

        private final String username;
        private final String address;
        private final String transport;
        private final String sessionIdentifier;

        public AdditionalHeader(String userName, String hostAddress, String transport, String sessionIdentifier) {
            this.address = hostAddress;
            this.username = userName;
            this.transport = transport;
            this.sessionIdentifier = sessionIdentifier;
        }

        String getUsername() {
            return username;
        }

        String getAddress() {
            return address;
        }

        String getTransport() {
            return transport;
        }

        String getSessionType() {
            return sessionIdentifier;
        }

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer("AdditionalHeader{");
            sb.append("username='").append(username).append('\'');
            sb.append(", address='").append(address).append('\'');
            sb.append(", transport='").append(transport).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }

}
