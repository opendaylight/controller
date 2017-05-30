/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class SimpleDispatcher extends AbstractDispatcher<SimpleSession, SimpleSessionListener> {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleDispatcher.class);

    private final SessionNegotiatorFactory<SimpleMessage, SimpleSession, SimpleSessionListener> negotiatorFactory;
    private final ChannelOutboundHandler encoder = new SimpleMessageToByteEncoder();

    private final class SimplePipelineInitializer implements PipelineInitializer<SimpleSession> {
        final SessionListenerFactory<SimpleSessionListener> listenerFactory;

        SimplePipelineInitializer(final SessionListenerFactory<SimpleSessionListener> listenerFactory) {
            this.listenerFactory = Preconditions.checkNotNull(listenerFactory);
        }

        @Override
        public void initializeChannel(final SocketChannel channel, final Promise<SimpleSession> promise) {
            channel.pipeline().addLast(new SimpleByteToMessageDecoder());
            channel.pipeline().addLast("negotiator", negotiatorFactory.getSessionNegotiator(listenerFactory, channel, promise));
            channel.pipeline().addLast(encoder);
            LOG.debug("initialization completed for channel {}", channel);
        }

    }

    public SimpleDispatcher(final SessionNegotiatorFactory<SimpleMessage, SimpleSession, SimpleSessionListener> negotiatorFactory,
            final Promise<SimpleSession> promise, final EventLoopGroup eventLoopGroup) {
        super(eventLoopGroup, eventLoopGroup);
        this.negotiatorFactory = Preconditions.checkNotNull(negotiatorFactory);
    }

    public Future<SimpleSession> createClient(final InetSocketAddress address, final ReconnectStrategy strategy, final SessionListenerFactory<SimpleSessionListener> listenerFactory) {
        return super.createClient(address, strategy, new SimplePipelineInitializer(listenerFactory));
    }

    public Future<Void> createReconnectingClient(final InetSocketAddress address, final ReconnectStrategyFactory strategy, final SessionListenerFactory<SimpleSessionListener> listenerFactory) {
        return super.createReconnectingClient(address, strategy, new SimplePipelineInitializer(listenerFactory));
    }

    public ChannelFuture createServer(final InetSocketAddress address, final SessionListenerFactory<SimpleSessionListener> listenerFactory) {
        return super.createServer(address, new SimplePipelineInitializer(listenerFactory));
    }

    @Override
    public void close() {
    }
}
