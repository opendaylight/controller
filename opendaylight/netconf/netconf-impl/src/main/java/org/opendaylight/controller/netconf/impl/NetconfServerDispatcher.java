/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl;

import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.Promise;

import java.net.InetSocketAddress;

import org.opendaylight.controller.netconf.impl.util.DeserializerExceptionHandler;
import org.opendaylight.controller.netconf.util.AbstractChannelInitializer;
import org.opendaylight.protocol.framework.AbstractDispatcher;

public class NetconfServerDispatcher extends AbstractDispatcher<NetconfServerSession, NetconfServerSessionListener> {

    private final ServerChannelInitializer initializer;

    public NetconfServerDispatcher(ServerChannelInitializer serverChannelInitializer, EventLoopGroup bossGroup,
            EventLoopGroup workerGroup) {
        super(bossGroup, workerGroup);
        this.initializer = serverChannelInitializer;
    }

    public ChannelFuture createServer(InetSocketAddress address) {

        return super.createServer(address, new PipelineInitializer<NetconfServerSession>() {
            @Override
            public void initializeChannel(final SocketChannel ch, final Promise<NetconfServerSession> promise) {
                initializer.initialize(ch, promise);
            }
        });
    }

    public static class ServerChannelInitializer extends AbstractChannelInitializer<NetconfServerSession> {

        public static final String DESERIALIZER_EX_HANDLER_KEY = "deserializerExHandler";

        private final NetconfServerSessionNegotiatorFactory negotiatorFactory;
        private final NetconfServerSessionListenerFactory listenerFactory;

        public ServerChannelInitializer(NetconfServerSessionNegotiatorFactory negotiatorFactory,
                                            NetconfServerSessionListenerFactory listenerFactory) {
            this.negotiatorFactory = negotiatorFactory;
            this.listenerFactory = listenerFactory;
        }

        @Override
        protected void initializeMessageDecoder(SocketChannel ch) {
            super.initializeMessageDecoder(ch);
            ch.pipeline().addLast(DESERIALIZER_EX_HANDLER_KEY, new DeserializerExceptionHandler());
        }

        @Override
        protected void initializeSessionNegotiator(SocketChannel ch, Promise<NetconfServerSession> promise) {
            ch.pipeline().addAfter(DESERIALIZER_EX_HANDLER_KEY, AbstractChannelInitializer.NETCONF_SESSION_NEGOTIATOR, negotiatorFactory.getSessionNegotiator(listenerFactory, ch, promise));
        }
    }

}
