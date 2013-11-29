/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import io.netty.channel.Channel;
import io.netty.util.Timer;
import io.netty.util.concurrent.Promise;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.api.NetconfServerSessionPreferences;
import org.opendaylight.controller.netconf.util.AbstractNetconfSessionNegotiator;
import org.opendaylight.protocol.framework.SessionListener;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetconfServerSessionNegotiator extends
        AbstractNetconfSessionNegotiator<NetconfServerSessionPreferences, NetconfServerSession> {

    private static final AdditionalHeader DEFAULT_HEADER = new AdditionalHeader();

    protected NetconfServerSessionNegotiator(NetconfServerSessionPreferences sessionPreferences,
            Promise<NetconfServerSession> promise, Channel channel, Timer timer, SessionListener sessionListener) {
        super(sessionPreferences, promise, channel, timer, sessionListener);
    }

    @Override
    protected NetconfServerSession getSession(SessionListener sessionListener, Channel channel, NetconfMessage message) {
        Optional<String> additionalHeader = message.getAdditionalHeader();

        AdditionalHeader header;
        if (additionalHeader.isPresent()) {
            header = new AdditionalHeader(additionalHeader.get());
        } else {
            header = DEFAULT_HEADER;
        }
        return new NetconfServerSession(sessionListener, channel, sessionPreferences.getSessionId(), header);
    }

    static class AdditionalHeader {

        private static final Pattern pattern = Pattern
                .compile("\\[(?<username>[^;]+);(?<address>[0-9\\.]+):(?<port>[0-9]+);(?<transport>[a-z]+)[^\\]]*\\]");
        private final String username;
        private final String address;
        private final String transport;

        public AdditionalHeader(String addHeaderAsString) {
            Matcher matcher = pattern.matcher(addHeaderAsString);
            Preconditions.checkArgument(matcher.matches(), "Additional header in wrong format %s, expected %s",
                    addHeaderAsString, pattern);
            this.username = matcher.group("username");
            this.address = matcher.group("address");
            this.transport = matcher.group("transport");
        }

        private AdditionalHeader() {
            this.username = this.address = "unknown";
            this.transport = "ssh";
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

    }
}
