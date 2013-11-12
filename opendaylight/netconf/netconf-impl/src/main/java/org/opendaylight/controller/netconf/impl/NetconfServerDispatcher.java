/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl;

import com.google.common.base.Optional;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.Promise;
import org.opendaylight.controller.netconf.api.NetconfSession;
import org.opendaylight.controller.netconf.impl.util.DeserializerExceptionHandler;
import org.opendaylight.controller.netconf.util.AbstractSslChannelInitializer;
import org.opendaylight.protocol.framework.AbstractDispatcher;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.net.InetSocketAddress;

public class NetconfServerDispatcher extends AbstractDispatcher<NetconfSession, NetconfServerSessionListener> {

    private final ServerSslChannelInitializer initializer;

    public NetconfServerDispatcher(ServerSslChannelInitializer serverChannelInitializer, EventLoopGroup bossGroup,
            EventLoopGroup workerGroup) {
        super(bossGroup, workerGroup);
        this.initializer = serverChannelInitializer;
    }

    // TODO test create server with same address twice
    public ChannelFuture createServer(InetSocketAddress address) {

        return super.createServer(address, new PipelineInitializer<NetconfSession>() {
            @Override
            public void initializeChannel(final SocketChannel ch, final Promise<NetconfSession> promise) {
                initializer.initialize(ch, promise);
            }
        });
    }

    public static class ServerSslChannelInitializer extends AbstractSslChannelInitializer {

        private final NetconfServerSessionNegotiatorFactory negotiatorFactory;
        private final NetconfServerSessionListenerFactory listenerFactory;

        public ServerSslChannelInitializer(Optional<SSLContext> maybeContext,
                                            NetconfServerSessionNegotiatorFactory negotiatorFactory,
                                            NetconfServerSessionListenerFactory listenerFactory) {
            super(maybeContext);
            this.negotiatorFactory = negotiatorFactory;
            this.listenerFactory = listenerFactory;
        }

        @Override
        protected void initializeAfterDecoder(SocketChannel ch, Promise<? extends NetconfSession> promise) {
            ch.pipeline().addLast("deserializerExHandler", new DeserializerExceptionHandler());
            ch.pipeline().addLast("negotiator", negotiatorFactory.getSessionNegotiator(listenerFactory, ch, promise));
        }

        @Override
        protected void initSslEngine(SSLEngine sslEngine) {
            sslEngine.setUseClientMode(false);
        }
    }

}
