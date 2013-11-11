/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.util;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.api.NetconfSession;
import org.opendaylight.controller.netconf.util.handler.FramingMechanismHandlerFactory;
import org.opendaylight.controller.netconf.util.handler.NetconfMessageAggregator;
import org.opendaylight.controller.netconf.util.messages.FramingMechanism;
import org.opendaylight.controller.netconf.util.messages.NetconfMessageFactory;
import org.opendaylight.protocol.framework.ProtocolHandlerFactory;
import org.opendaylight.protocol.framework.ProtocolMessageDecoder;
import org.opendaylight.protocol.framework.ProtocolMessageEncoder;

import com.google.common.base.Optional;

import io.netty.channel.ChannelHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Promise;

public abstract class AbstractSslChannelInitializer extends AbstractChannelInitializer {

    private final Optional<SSLContext> maybeContext;
    private final NetconfHandlerFactory handlerFactory;

    public AbstractSslChannelInitializer(Optional<SSLContext> maybeContext) {
        this.maybeContext = maybeContext;
        this.handlerFactory = new NetconfHandlerFactory(new NetconfMessageFactory());
    }

    @Override
    public void initialize(SocketChannel ch, Promise<? extends NetconfSession> promise) {
        if (maybeContext.isPresent()) {
            initSsl(ch);
        }

        ch.pipeline().addLast("aggregator", new NetconfMessageAggregator(FramingMechanism.EOM));
        ch.pipeline().addLast(handlerFactory.getDecoders());
        initializeAfterDecoder(ch, promise);
        ch.pipeline().addLast("frameEncoder", FramingMechanismHandlerFactory.createHandler(FramingMechanism.EOM));
        ch.pipeline().addLast(handlerFactory.getEncoders());
    }

    private void initSsl(SocketChannel ch) {
        SSLEngine sslEngine = maybeContext.get().createSSLEngine();
        initSslEngine(sslEngine);
        final SslHandler handler = new SslHandler(sslEngine);
        ch.pipeline().addLast("ssl", handler);
    }

    protected abstract void initSslEngine(SSLEngine sslEngine);

    private static final class NetconfHandlerFactory extends ProtocolHandlerFactory<NetconfMessage> {

        public NetconfHandlerFactory(final NetconfMessageFactory msgFactory) {
            super(msgFactory);
        }

        @Override
        public ChannelHandler[] getEncoders() {
            return new ChannelHandler[] { new ProtocolMessageEncoder(this.msgFactory) };
        }

        @Override
        public ChannelHandler[] getDecoders() {
            return new ChannelHandler[] { new ProtocolMessageDecoder(this.msgFactory) };
        }
    }
}
