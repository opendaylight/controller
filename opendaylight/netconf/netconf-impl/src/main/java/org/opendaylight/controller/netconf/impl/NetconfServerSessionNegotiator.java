/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl;

import java.net.InetSocketAddress;

import org.opendaylight.controller.netconf.api.NetconfServerSessionPreferences;
import org.opendaylight.controller.netconf.util.AbstractNetconfSessionNegotiator;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessage;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessageAdditionalHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

import io.netty.channel.Channel;
import io.netty.util.Timer;
import io.netty.util.concurrent.Promise;

public class NetconfServerSessionNegotiator extends
        AbstractNetconfSessionNegotiator<NetconfServerSessionPreferences, NetconfServerSession, NetconfServerSessionListener> {

    static final Logger logger = LoggerFactory.getLogger(NetconfServerSessionNegotiator.class);

    protected NetconfServerSessionNegotiator(NetconfServerSessionPreferences sessionPreferences,
            Promise<NetconfServerSession> promise, Channel channel, Timer timer, NetconfServerSessionListener sessionListener,
            long connectionTimeoutMillis) {
        super(sessionPreferences, promise, channel, timer, sessionListener, connectionTimeoutMillis);
    }

    @Override
    protected NetconfServerSession getSession(NetconfServerSessionListener sessionListener, Channel channel, NetconfHelloMessage message) {
        Optional<NetconfHelloMessageAdditionalHeader> additionalHeader = message.getAdditionalHeader();

        NetconfHelloMessageAdditionalHeader parsedHeader;
        if (additionalHeader.isPresent()) {
            parsedHeader = additionalHeader.get();
        } else {
            InetSocketAddress inetSocketAddress = (InetSocketAddress) channel.localAddress();
            parsedHeader = new NetconfHelloMessageAdditionalHeader("unknown", inetSocketAddress.getHostString(), Integer.toString(inetSocketAddress.getPort()),
                    "tcp", "client");
        }

        logger.debug("Additional header from hello parsed as {} from {}", parsedHeader, additionalHeader);

        return new NetconfServerSession(sessionListener, channel, getSessionPreferences().getSessionId(), parsedHeader);
    }

   }
