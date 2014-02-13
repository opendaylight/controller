/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.client;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

import java.io.Closeable;
import java.net.InetSocketAddress;

import org.opendaylight.controller.netconf.util.AbstractChannelInitializer;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessageAdditionalHeader;
import org.opendaylight.protocol.framework.AbstractDispatcher;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.opendaylight.protocol.framework.SessionListenerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class NetconfClientDispatcher extends AbstractDispatcher<NetconfClientSession, NetconfClientSessionListener> implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(NetconfClientDispatcher.class);

    private final NetconfClientSessionNegotiatorFactory negotiatorFactory;
    private final HashedWheelTimer timer;

    public NetconfClientDispatcher(EventLoopGroup bossGroup, EventLoopGroup workerGroup,
            long clientConnectionTimeoutMillis) {
        super(bossGroup, workerGroup);
        timer = new HashedWheelTimer();
        this.negotiatorFactory = new NetconfClientSessionNegotiatorFactory(timer,
                Optional.<NetconfHelloMessageAdditionalHeader> absent(), clientConnectionTimeoutMillis);
    }

    public NetconfClientDispatcher(EventLoopGroup bossGroup, EventLoopGroup workerGroup,
            NetconfHelloMessageAdditionalHeader additionalHeader, long connectionTimeoutMillis) {
        super(bossGroup, workerGroup);
        timer = new HashedWheelTimer();
        this.negotiatorFactory = new NetconfClientSessionNegotiatorFactory(timer, Optional.of(additionalHeader),
                connectionTimeoutMillis);
    }

    public Future<NetconfClientSession> createClient(InetSocketAddress address,
            final NetconfClientSessionListener sessionListener, ReconnectStrategy strat) {

        return super.createClient(address, strat, new PipelineInitializer<NetconfClientSession>() {

            @Override
            public void initializeChannel(final SocketChannel ch, final Promise<NetconfClientSession> promise) {
                initialize(ch, promise);
            }

            private void initialize(SocketChannel ch, Promise<NetconfClientSession> promise) {
                new ClientChannelInitializer(negotiatorFactory, sessionListener).initialize(ch, promise);
            }
        });
    }

    public Future<Void> createReconnectingClient(final InetSocketAddress address,
            final NetconfClientSessionListener listener,
            final ReconnectStrategyFactory connectStrategyFactory, final ReconnectStrategy reestablishStrategy) {
        final ClientChannelInitializer init = new ClientChannelInitializer(negotiatorFactory, listener);

        return super.createReconnectingClient(address, connectStrategyFactory, reestablishStrategy,
                new PipelineInitializer<NetconfClientSession>() {
            @Override
            public void initializeChannel(final SocketChannel ch, final Promise<NetconfClientSession> promise) {
                init.initialize(ch, promise);
            }
        });
    }

    private static class ClientChannelInitializer extends AbstractChannelInitializer<NetconfClientSession> {

        private final NetconfClientSessionNegotiatorFactory negotiatorFactory;
        private final NetconfClientSessionListener sessionListener;

        private ClientChannelInitializer(NetconfClientSessionNegotiatorFactory negotiatorFactory,
                NetconfClientSessionListener sessionListener) {
            this.negotiatorFactory = negotiatorFactory;
            this.sessionListener = sessionListener;
        }

        @Override
        public void initialize(SocketChannel ch, Promise<NetconfClientSession> promise) {
                super.initialize(ch,promise);
        }

        @Override
        protected void initializeSessionNegotiator(SocketChannel ch, Promise<NetconfClientSession> promise) {
            ch.pipeline().addAfter(NETCONF_MESSAGE_DECODER,  AbstractChannelInitializer.NETCONF_SESSION_NEGOTIATOR,
                    negotiatorFactory.getSessionNegotiator(
                            new SessionListenerFactory<NetconfClientSessionListener>() {
                                @Override
                                public NetconfClientSessionListener getSessionListener() {
                                    return sessionListener;
                                }
                            }, ch, promise));
        }
    }

    @Override
    public void close() {
        try {
            timer.stop();
        } catch (Exception e) {
            logger.debug("Ignoring exception while closing {}", timer, e);
        }
    }
}
