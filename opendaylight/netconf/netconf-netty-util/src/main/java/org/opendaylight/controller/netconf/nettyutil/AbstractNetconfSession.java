/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.nettyutil;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import java.io.IOException;
import org.opendaylight.controller.netconf.api.NetconfExiSession;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.api.NetconfSession;
import org.opendaylight.controller.netconf.api.NetconfSessionListener;
import org.opendaylight.controller.netconf.api.NetconfTerminationReason;
import org.opendaylight.controller.netconf.nettyutil.handler.NetconfEXICodec;
import org.opendaylight.controller.netconf.nettyutil.handler.NetconfEXIToMessageDecoder;
import org.opendaylight.controller.netconf.nettyutil.handler.NetconfMessageToEXIEncoder;
import org.opendaylight.controller.netconf.nettyutil.handler.exi.EXIParameters;
import org.opendaylight.controller.netconf.util.xml.XmlElement;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.protocol.framework.AbstractProtocolSession;
import org.openexi.proc.common.EXIOptionsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractNetconfSession<S extends NetconfSession, L extends NetconfSessionListener<S>> extends AbstractProtocolSession<NetconfMessage> implements NetconfSession, NetconfExiSession {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractNetconfSession.class);
    private final L sessionListener;
    private final long sessionId;
    private boolean up = false;

    private ChannelHandler delayedEncoder;

    private final Channel channel;

    protected AbstractNetconfSession(final L sessionListener, final Channel channel, final long sessionId) {
        this.sessionListener = sessionListener;
        this.channel = channel;
        this.sessionId = sessionId;
        LOG.debug("Session {} created", sessionId);
    }

    protected abstract S thisInstance();

    @Override
    public void close() {
        channel.close();
        up = false;
        sessionListener.onSessionTerminated(thisInstance(), new NetconfTerminationReason("Session closed"));
    }

    @Override
    protected void handleMessage(final NetconfMessage netconfMessage) {
        LOG.debug("handling incoming message");
        sessionListener.onMessage(thisInstance(), netconfMessage);
    }

    @Override
    public ChannelFuture sendMessage(final NetconfMessage netconfMessage) {
        final ChannelFuture future = channel.writeAndFlush(netconfMessage);
        if (delayedEncoder != null) {
            replaceMessageEncoder(delayedEncoder);
            delayedEncoder = null;
        }

        return future;
    }

    @Override
    protected void endOfInput() {
        LOG.debug("Session {} end of input detected while session was in state {}", toString(), isUp() ? "up"
                : "initialized");
        if (isUp()) {
            this.sessionListener.onSessionDown(thisInstance(), new IOException("End of input detected. Close the session."));
        }
    }

    @Override
    protected void sessionUp() {
        LOG.debug("Session {} up", toString());
        sessionListener.onSessionUp(thisInstance());
        this.up = true;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer(getClass().getSimpleName() + "{");
        sb.append("sessionId=").append(sessionId);
        sb.append('}');
        return sb.toString();
    }

    protected final void replaceMessageDecoder(final ChannelHandler handler) {
        replaceChannelHandler(AbstractChannelInitializer.NETCONF_MESSAGE_DECODER, handler);
    }

    protected final void replaceMessageEncoder(final ChannelHandler handler) {
        replaceChannelHandler(AbstractChannelInitializer.NETCONF_MESSAGE_ENCODER, handler);
    }

    protected final void replaceMessageEncoderAfterNextMessage(final ChannelHandler handler) {
        this.delayedEncoder = handler;
    }

    protected final void replaceChannelHandler(final String handlerName, final ChannelHandler handler) {
        channel.pipeline().replace(handlerName, handlerName, handler);
    }

    @Override
    public final void startExiCommunication(final NetconfMessage startExiMessage) {
        final EXIParameters exiParams;
        try {
            exiParams = EXIParameters.fromXmlElement(XmlElement.fromDomDocument(startExiMessage.getDocument()));
        } catch (final EXIOptionsException e) {
            LOG.warn("Unable to parse EXI parameters from {} on session {}", XmlUtil.toString(startExiMessage.getDocument()), this, e);
            throw new IllegalArgumentException("Cannot parse options", e);
        }

        final NetconfEXICodec exiCodec = new NetconfEXICodec(exiParams.getOptions());
        final NetconfMessageToEXIEncoder exiEncoder;
        try {
            exiEncoder = NetconfMessageToEXIEncoder.create(exiCodec);
        } catch (EXIOptionsException e) {
            LOG.warn("Failed to instantiate EXI encoder for {} on session {}", exiCodec, this, e);
            throw new IllegalStateException("Cannot instantiate encoder for options", e);
        }

        final NetconfEXIToMessageDecoder exiDecoder;
        try {
            exiDecoder = NetconfEXIToMessageDecoder.create(exiCodec);
        } catch (EXIOptionsException e) {
            LOG.warn("Failed to instantiate EXI decodeer for {} on session {}", exiCodec, this, e);
            throw new IllegalStateException("Cannot instantiate encoder for options", e);
        }

        addExiHandlers(exiDecoder, exiEncoder);
        LOG.debug("Session {} EXI handlers added to pipeline", this);
    }

    /**
     * Add a set encoder/decoder tuple into the channel pipeline as appropriate.
     *
     * @param decoder EXI decoder
     * @param encoder EXI encoder
     */
    protected abstract void addExiHandlers(ByteToMessageDecoder decoder, MessageToByteEncoder<NetconfMessage> encoder);

    public final boolean isUp() {
        return up;
    }

    public final long getSessionId() {
        return sessionId;
    }
}
