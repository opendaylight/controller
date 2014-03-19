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
import io.netty.channel.ChannelHandler;
import org.opendaylight.protocol.framework.AbstractProtocolSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public abstract class AbstractNetconfSession<S extends NetconfSession, L extends NetconfSessionListener<S>> extends AbstractProtocolSession<NetconfMessage> implements NetconfSession {
    private static final Logger logger = LoggerFactory.getLogger(AbstractNetconfSession.class);
    private final L sessionListener;
    private final long sessionId;
    private boolean up = false;
    private String removeAfterMessageSentname;
    private ChannelHandler exiEncoder;
    private String exiEncoderName;

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
        ChannelFuture future = channel.writeAndFlush(netconfMessage);
        if (exiEncoder!=null){
            if (channel.pipeline().get(exiEncoderName)== null){
                channel.pipeline().addBefore("netconfMessageEncoder",exiEncoderName,exiEncoder);
                logger.debug(exiEncoderName+" upstreamed netconfMessageEncoder");
                exiEncoder = null;
            }
        }
        if (removeAfterMessageSentname!=null){
            channel.pipeline().remove(removeAfterMessageSentname);
            removeAfterMessageSentname = null;
        }
        return future;
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

    public <T extends ChannelHandler> T remove(Class<T> handlerType) {
        return this.channel.pipeline().remove(handlerType);
    }

    public void removeAfterMessageSent(String handlerName) {
        this.removeAfterMessageSentname = handlerName;
    }

    public void addExiDecoder(String name, ChannelHandler handler) {
        channel.pipeline().addAfter("netconfMessageDecoder",name,handler);
        logger.debug(name+" included in stream after netconfMessageDecoder ");
    }

    public void addExiEncoderAfterMessageSent(String name, ChannelHandler handler) {
        this.exiEncoder = handler;
        this.exiEncoderName = name;
    }

    public final boolean isUp() {
        return up;
    }

    public final long getSessionId() {
        return sessionId;
    }
}

