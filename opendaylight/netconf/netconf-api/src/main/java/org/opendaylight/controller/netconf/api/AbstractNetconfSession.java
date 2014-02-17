/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.api;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

import java.io.IOException;

import org.opendaylight.protocol.framework.AbstractProtocolSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractNetconfSession<S extends NetconfSession, L extends NetconfSessionListener<S>> extends AbstractProtocolSession<NetconfMessage> implements NetconfSession {
    private static final Logger logger = LoggerFactory.getLogger(AbstractNetconfSession.class);
    private final L sessionListener;
    private final long sessionId;
    private boolean up = false;

    private final Channel channel;

    protected AbstractNetconfSession(L sessionListener, Channel channel, long sessionId) {
        this.sessionListener = sessionListener;
        this.channel = channel;
        this.sessionId = sessionId;
        logger.debug("Session {} created", sessionId);
    }

    protected abstract S thisInstance();

    @Override
    public void close() {
        channel.close();
        up = false;
        sessionListener.onSessionTerminated(thisInstance(), new NetconfTerminationReason("Session closed"));
    }

    @Override
    protected void handleMessage(NetconfMessage netconfMessage) {
        logger.debug("handling incoming message");
        sessionListener.onMessage(thisInstance(), netconfMessage);
    }

    @Override
    public ChannelFuture sendMessage(NetconfMessage netconfMessage) {
        return channel.writeAndFlush(netconfMessage);
    }

    @Override
    protected void endOfInput() {
        logger.debug("Session {} end of input detected while session was in state {}", toString(), isUp() ? "up"
                : "initialized");
        if (isUp()) {
            this.sessionListener.onSessionDown(thisInstance(), new IOException("End of input detected. Close the session."));
        }
    }

    @Override
    protected void sessionUp() {
        logger.debug("Session {} up", toString());
        sessionListener.onSessionUp(thisInstance());
        this.up = true;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("ServerNetconfSession{");
        sb.append("sessionId=").append(sessionId);
        sb.append('}');
        return sb.toString();
    }

    public final boolean isUp() {
        return up;
    }

    public final long getSessionId() {
        return sessionId;
    }
}

