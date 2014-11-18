/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl;

import com.google.common.base.Optional;
import io.netty.channel.Channel;
import io.netty.channel.local.LocalAddress;
import io.netty.util.Timer;
import io.netty.util.concurrent.Promise;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.AbstractMap;
import java.util.Map;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfServerSessionPreferences;
import org.opendaylight.controller.netconf.nettyutil.AbstractNetconfSessionNegotiator;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessage;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessageAdditionalHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetconfServerSessionNegotiator
        extends
        AbstractNetconfSessionNegotiator<NetconfServerSessionPreferences, NetconfServerSession, NetconfServerSessionListener> {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfServerSessionNegotiator.class);

    private static final String UNKNOWN = "unknown";

    protected NetconfServerSessionNegotiator(
            NetconfServerSessionPreferences sessionPreferences,
            Promise<NetconfServerSession> promise, Channel channel,
            Timer timer, NetconfServerSessionListener sessionListener,
            long connectionTimeoutMillis) {
        super(sessionPreferences, promise, channel, timer, sessionListener,
                connectionTimeoutMillis);
    }

    @Override
    protected void handleMessage(NetconfHelloMessage netconfMessage)
            throws NetconfDocumentedException {
        NetconfServerSession session = getSessionForHelloMessage(netconfMessage);
        replaceHelloMessageInboundHandler(session);
        // Negotiation successful after all non hello messages were processed
        negotiationSuccessful(session);
    }

    @Override
    protected NetconfServerSession getSession(
            NetconfServerSessionListener sessionListener, Channel channel,
            NetconfHelloMessage message) {
        Optional<NetconfHelloMessageAdditionalHeader> additionalHeader = message
                .getAdditionalHeader();

        NetconfHelloMessageAdditionalHeader parsedHeader;
        if (additionalHeader.isPresent()) {
            parsedHeader = additionalHeader.get();
        } else {

            parsedHeader = new NetconfHelloMessageAdditionalHeader(UNKNOWN,
                    getHostName(channel.localAddress()).getValue(),
                    getHostName(channel.localAddress()).getKey(), "tcp",
                    "client");

        }

        LOG.debug("Additional header from hello parsed as {} from {}",
                parsedHeader, additionalHeader);

        return new NetconfServerSession(sessionListener, channel,
                getSessionPreferences().getSessionId(), parsedHeader);
    }

    /**
     * @param socketAddress
     *            type of socket address LocalAddress, or
     *            InetSocketAddress, for others returns unknown
     * @return Map<port, host > two values - port and host of socket address
     */
    protected static Map.Entry<String, String> getHostName(
            SocketAddress socketAddress) {

        if (socketAddress instanceof InetSocketAddress) {

            InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;

            return new AbstractMap.SimpleImmutableEntry<>(
                    Integer.toString(inetSocketAddress.getPort()),
                    inetSocketAddress.getHostString());

        } else if (socketAddress instanceof LocalAddress) {

            return new AbstractMap.SimpleImmutableEntry<>(UNKNOWN,
                    ((LocalAddress) socketAddress).id());

        }
        return new AbstractMap.SimpleImmutableEntry<>(UNKNOWN, UNKNOWN);

    }

}
