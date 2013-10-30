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
import java.util.Map;

import org.opendaylight.protocol.framework.AbstractProtocolSession;
import org.opendaylight.protocol.framework.ProtocolMessageDecoder;
import org.opendaylight.protocol.framework.ProtocolMessageEncoder;
import org.opendaylight.protocol.framework.SessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class NetconfSession extends AbstractProtocolSession<NetconfMessage> {

    private ChannelHandler exiEncoder;
    private String exiEncoderName;
    private String removeAfterMessageSentname;
    private String pmeName,pmdName;
    private final  Channel channel;
    private final  SessionListener sessionListener;
    private final long sessionId;
    private boolean up = false;
    private static final Logger logger = LoggerFactory.getLogger(NetconfSession.class);
    private static final int T = 0;

    protected NetconfSession(SessionListener sessionListener, Channel channel, long sessionId) {
        this.sessionListener = sessionListener;
        this.channel = channel;
        this.sessionId = sessionId;
        logger.debug("Session {} created", toString());

        ChannelHandler pmd = channel.pipeline().get(ProtocolMessageDecoder.class);
        ChannelHandler pme = channel.pipeline().get(ProtocolMessageEncoder.class);

        for (Map.Entry<String, ChannelHandler> entry:channel.pipeline().toMap().entrySet()){
            if (entry.getValue().equals(pmd)){
                pmdName = entry.getKey();
            }
            if (entry.getValue().equals(pme)){
                pmeName = entry.getKey();
            }
        }
    }
    @Override
    public void close() {
        channel.close();
        sessionListener.onSessionTerminated(this, new NetconfTerminationReason("Session closed"));
    }

    @Override
    protected void handleMessage(NetconfMessage netconfMessage) {
        logger.debug("handlign incomming message");
        sessionListener.onMessage(this, netconfMessage);
    }

    public void sendMessage(NetconfMessage netconfMessage) {
        channel.writeAndFlush(netconfMessage);
        if (exiEncoder!=null){
            if (channel.pipeline().get(exiEncoderName)== null){
                channel.pipeline().addBefore(pmeName, exiEncoderName, exiEncoder);
            }
        }
        if (removeAfterMessageSentname!=null){
            channel.pipeline().remove(removeAfterMessageSentname);
            removeAfterMessageSentname = null;
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

    public void addExiDecoder(String name,ChannelHandler handler){
        if (channel.pipeline().get(name)== null){
            channel.pipeline().addBefore(pmdName, name, handler);
        }
    }
    public void addExiEncoderAfterMessageSent(String name, ChannelHandler handler){
        this.exiEncoder = handler;
        this.exiEncoderName = name;
    }

    public void addExiEncoder(String name, ChannelHandler handler){
        channel.pipeline().addBefore(pmeName, name, handler);
    }

    public void removeAfterMessageSent(String handlerName){
        this.removeAfterMessageSentname = handlerName;
    }

}

