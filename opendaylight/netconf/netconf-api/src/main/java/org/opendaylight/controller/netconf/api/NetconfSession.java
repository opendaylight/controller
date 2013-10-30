/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.api;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;

import java.io.IOException;

import org.opendaylight.protocol.framework.AbstractProtocolSession;
import org.opendaylight.protocol.framework.ProtocolMessageDecoder;
import org.opendaylight.protocol.framework.ProtocolMessageEncoder;
import org.opendaylight.protocol.framework.SessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class NetconfSession extends AbstractProtocolSession<NetconfMessage> {

    private HandlerManipulator originEncoder;
    private HandlerManipulator originDecoder;
    private HandlerManipulator newEncoder;
    private final  Channel channel;
    private final  SessionListener sessionListener;
    private final long sessionId;
    private boolean up = false;
    private boolean switchNew, switchOriginal = false;
    private static final Logger logger = LoggerFactory.getLogger(NetconfSession.class);
    private static final int T = 0;

    protected NetconfSession(SessionListener sessionListener, Channel channel, long sessionId) {
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
        logger.debug("handlign incomming message : ");
        sessionListener.onMessage(this, netconfMessage);
    }

    public void sendMessage(NetconfMessage netconfMessage) {
        channel.writeAndFlush(netconfMessage);
        if (switchNew) {
            channel.pipeline().replace(originEncoder.getHandler(), newEncoder.getHandlerName(),newEncoder.getHandler());
            switchNew = false;
        }

        if (switchOriginal) {
            channel.pipeline().replace(newEncoder.getHandler(), originEncoder.getHandlerName(),originEncoder.getHandler());
            switchOriginal = false;
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

    public <T extends ChannelHandler> T remove(Class<T> handlerType) {
        return channel.pipeline().remove(handlerType);
    }

    public <T extends ChannelHandler> T getHandler(Class<T> handlerType) {
        return channel.pipeline().get(handlerType);
    }

    public void addFirst(ChannelHandler handler, String name){
        channel.pipeline().addFirst(name, handler);
    }
    public void addLast(ChannelHandler handler, String name){
        channel.pipeline().addLast(name, handler);
    }

    public void replaceEncoder(ChannelHandler handler, String handlerName) {
        this.originEncoder = new HandlerManipulator("protocolMessageEncoder",channel.pipeline().get(ProtocolMessageEncoder.class));
        this.newEncoder = new HandlerManipulator(handlerName,handler);
        this.switchNew = true;
    }
    public void switchToOriginEncoder()
    {
        this.switchOriginal = true;
    }

    public void replaceDecoder(ChannelHandler handler, String handlerName) {
        this.originDecoder = new HandlerManipulator("protocolMessageDecoder",channel.pipeline().get(ProtocolMessageDecoder.class));
        channel.pipeline().replace(originDecoder.getHandler(), handlerName, handler);
    }

    public void switchToOriginDecoder(String handlerName)
    {
        channel.pipeline().replace(channel.pipeline().get(handlerName), originDecoder.getHandlerName(),originDecoder.getHandler());
    }

    /*
    public void removeAfterMessageSend(String name) {
        this.handlerNameToRemove = new ChannelManipulator(name, null, null,null);
    }
    */
}

