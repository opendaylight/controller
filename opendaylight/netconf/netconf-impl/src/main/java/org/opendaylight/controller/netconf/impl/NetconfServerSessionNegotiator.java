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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.api.NetconfServerSessionPreferences;
import org.opendaylight.controller.netconf.util.AbstractNetconfSessionNegotiator;
import org.opendaylight.protocol.framework.SessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public class NetconfServerSessionNegotiator extends
        AbstractNetconfSessionNegotiator<NetconfServerSessionPreferences, NetconfServerSession> {

    static final Logger logger = LoggerFactory.getLogger(NetconfServerSessionNegotiator.class);

    protected NetconfServerSessionNegotiator(NetconfServerSessionPreferences sessionPreferences,
            Promise<NetconfServerSession> promise, Channel channel, Timer timer, SessionListener sessionListener) {
        super(sessionPreferences, promise, channel, timer, sessionListener);
    }

    @Override
    protected NetconfServerSession getSession(SessionListener sessionListener, Channel channel, NetconfMessage message) {
        Optional<String> additionalHeader = message.getAdditionalHeader();

        AdditionalHeader parsedHeader;
        if (additionalHeader.isPresent()) {
            parsedHeader = new AdditionalHeader(additionalHeader.get());
        } else {
            parsedHeader = new AdditionalHeader("unknow", ((InetSocketAddress)channel.localAddress()).getHostString(),
                    "tcp", "client");
        }
        logger.debug("Additional header from hello parsed as {} from {}", parsedHeader, additionalHeader);

        return new NetconfServerSession(sessionListener, channel, sessionPreferences.getSessionId(), parsedHeader);
    }

    static class AdditionalHeader {

        private static final Pattern pattern = Pattern
                .compile("\\[(?<username>[^;]+);(?<address>[0-9\\.]+)[:/](?<port>[0-9]+);(?<transport>[a-z]+)[^\\]]+\\]");
        private static final Pattern customHeaderPattern = Pattern
                .compile("\\[(?<username>[^;]+);(?<address>[0-9\\.]+)[:/](?<port>[0-9]+);(?<transport>[a-z]+);(?<sessionType>[a-z]+)[^\\]]+\\]");
        private final String username;
        private final String address;
        private final String transport;
        private final String sessionType;

        public AdditionalHeader(String addHeaderAsString) {
            addHeaderAsString = addHeaderAsString.trim();
            Matcher matcher = pattern.matcher(addHeaderAsString);
            Matcher matcher2 = customHeaderPattern.matcher(addHeaderAsString);
            Preconditions.checkArgument(matcher.matches(), "Additional header in wrong format %s, expected %s",
                    addHeaderAsString, pattern);
            this.username = matcher.group("username");
            this.address = matcher.group("address");
            this.transport = matcher.group("transport");
            if(matcher2.matches()) {
                this.sessionType = matcher2.group("sessionType");
            } else {
                this.sessionType = "client";
            }
        }

        private AdditionalHeader(String userName, String hostAddress, String transport, String sessionType) {
            this.address = hostAddress;
            this.username = userName;
            this.transport = transport;
            this.sessionType = sessionType;
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
            return sessionType;
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
