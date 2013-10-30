/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;

import java.io.IOException;

import org.opendaylight.controller.netconf.api.ChannelManipulator;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.api.NetconfSession;
import org.opendaylight.controller.netconf.api.NetconfTerminationReason;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.protocol.framework.SessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetconfServerSession extends NetconfSession {

    private final SessionListener sessionListener;
    private final Channel channel;

    private static final Logger logger = LoggerFactory.getLogger(NetconfServerSession.class);
    private final long sessionId;
    private boolean up = false;

    private ChannelManipulator handlerToAddAfterMessageSent;
    private ChannelManipulator handlerNameToRemove;

    public NetconfServerSession(SessionListener sessionListener, Channel channel, long sessionId) {
        this.sessionListener = sessionListener;
        this.channel = channel;
        this.sessionId = sessionId;
        logger.debug("Session {} created", toString());
    }

    @Override
    public void close() {
        channel.close();
        sessionListener.onSessionTerminated(this, new NetconfTerminationReason("Session closed"));
    }

    @Override
    protected void handleMessage(NetconfMessage netconfMessage) {
        logger.debug("Session {} received message {}", toString(), XmlUtil.toString(netconfMessage.getDocument()));
        sessionListener.onMessage(this, netconfMessage);
    }

    public void sendMessage(NetconfMessage netconfMessage) {
        channel.writeAndFlush(netconfMessage);
        if (this.handlerToAddAfterMessageSent != null) {
            if (channel.pipeline().get(this.handlerToAddAfterMessageSent.getName()) == null){
                channel.pipeline().addLast(
                        this.handlerToAddAfterMessageSent.getName(),
                        this.handlerToAddAfterMessageSent.getHandler());
            }
        }
        if (this.handlerNameToRemove != null
                && this.handlerNameToRemove.getName()!=null) {
            if (channel.pipeline().get(this.handlerToAddAfterMessageSent.getName()) != null)
                channel.pipeline().remove(this.handlerNameToRemove.getName());
        }
    }

    @Override
    protected void endOfInput() {
        logger.debug("Session {} end of input detected while session was in state {}", toString(), isUp() ? "up"
                : "initialized");
        if (isUp()) {
            this.sessionListener.onSessionDown(this, new IOException("End of input detected. Close the session."));
        }
    }

    @Override
    protected void sessionUp() {
        logger.debug("Session {} up", toString());
        sessionListener.onSessionUp(this);
        this.up = true;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("ServerNetconfSession{");
        sb.append("sessionId=").append(sessionId);
        sb.append('}');
        return sb.toString();
    }

    public boolean isUp() {
        return up;
    }

    public long getSessionId() {
        return sessionId;
    }



    @Override
    public <T extends ChannelHandler> T remove(Class<T> handlerType) {
        return channel.pipeline().remove(handlerType);
    }

    @Override
    public void addFirst(String name, ChannelHandler handler) {
        channel.pipeline().addFirst(name, handler);
    }

    @Override
    public void addLast(ChannelHandler handler) {
        channel.pipeline().addLast(handler);
    }

    @Override
    public void addAfter(String baseName, String name, ChannelHandler handler) {
        channel.pipeline().addAfter(baseName, name, handler);
    }

    @Override
    public void addAfterMessageSend(String name, String baseName,
            ChannelHandler handler) {
        this.handlerToAddAfterMessageSent = new ChannelManipulator(name,
                baseName,
                handler);
    }

    @Override
    public void removeAfterMessageSend(String name) {
        this.handlerNameToRemove = new ChannelManipulator(name, null, null);
    }

}
