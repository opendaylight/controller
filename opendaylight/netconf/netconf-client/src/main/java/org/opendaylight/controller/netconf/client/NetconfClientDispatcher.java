/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.client;

import com.google.common.base.Optional;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.api.NetconfSession;
import org.opendaylight.controller.netconf.api.NetconfTerminationReason;
import org.opendaylight.controller.netconf.util.AbstractChannelInitializer;
import org.opendaylight.protocol.framework.AbstractDispatcher;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.SessionListener;
import org.opendaylight.protocol.framework.SessionListenerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.net.InetSocketAddress;

public class NetconfClientDispatcher extends AbstractDispatcher<NetconfClientSession, NetconfClientSessionListener> implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(NetconfClient.class);

    private final NetconfClientSessionNegotiatorFactory negotatorFactory;
    private final HashedWheelTimer timer;

    public NetconfClientDispatcher(EventLoopGroup bossGroup, EventLoopGroup workerGroup, long clientConnectionTimeoutMillis) {
        super(bossGroup, workerGroup);
        timer = new HashedWheelTimer();
        this.negotatorFactory = new NetconfClientSessionNegotiatorFactory(timer, Optional.<String>absent(), clientConnectionTimeoutMillis);
    }

    public NetconfClientDispatcher(EventLoopGroup bossGroup, EventLoopGroup workerGroup, String additionalHeader, long connectionTimeoutMillis) {
        super(bossGroup, workerGroup);
        timer = new HashedWheelTimer();
        this.negotatorFactory = new NetconfClientSessionNegotiatorFactory(timer, Optional.of(additionalHeader), connectionTimeoutMillis);
    }

    public Future<NetconfClientSession> createClient(InetSocketAddress address,
            final NetconfClientSessionListener sessionListener, ReconnectStrategy strat) {

        return super.createClient(address, strat, new PipelineInitializer<NetconfClientSession>() {

            @Override
            public void initializeChannel(final SocketChannel ch, final Promise<NetconfClientSession> promise) {
                initialize(ch, promise);
            }

            private void initialize(SocketChannel ch, Promise<NetconfClientSession> promise) {
                new ClientChannelInitializer( negotatorFactory, sessionListener).initialize(ch, promise);
            }
        });
    }

    private static class ClientChannelInitializer extends AbstractChannelInitializer {

        private final NetconfClientSessionNegotiatorFactory negotiatorFactory;
        private final NetconfClientSessionListener sessionListener;

        private ClientChannelInitializer(NetconfClientSessionNegotiatorFactory negotiatorFactory,
                                            NetconfClientSessionListener sessionListener) {
            this.negotiatorFactory = negotiatorFactory;
            this.sessionListener = sessionListener;
        }

        @Override
        public void initialize(SocketChannel ch, Promise<? extends NetconfSession> promise) {
                super.initialize(ch,promise);
        }

        @Override
        protected void initializeAfterDecoder(SocketChannel ch, Promise<? extends NetconfSession> promise) {
            ch.pipeline().addLast("negotiator", negotiatorFactory.getSessionNegotiator(new SessionListenerFactory() {
                @Override
                public SessionListener<NetconfMessage, NetconfClientSession, NetconfTerminationReason> getSessionListener() {
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
