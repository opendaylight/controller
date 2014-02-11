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

import java.io.IOException;
import java.net.InetSocketAddress;

import org.opendaylight.controller.netconf.util.AbstractChannelInitializer;
import org.opendaylight.controller.netconf.util.handler.ssh.SshHandler;
import org.opendaylight.controller.netconf.util.handler.ssh.authentication.AuthenticationHandler;
import org.opendaylight.controller.netconf.util.handler.ssh.client.Invoker;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessageAdditionalHeader;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;
import org.opendaylight.protocol.framework.SessionListenerFactory;

import com.google.common.base.Optional;

public class NetconfSshClientDispatcher extends NetconfClientDispatcher {

    private final AuthenticationHandler authHandler;
    private final HashedWheelTimer timer;
    private final NetconfClientSessionNegotiatorFactory negotiatorFactory;

    public NetconfSshClientDispatcher(AuthenticationHandler authHandler, EventLoopGroup bossGroup,
            EventLoopGroup workerGroup, long connectionTimeoutMillis) {
        super(bossGroup, workerGroup, connectionTimeoutMillis);
        this.authHandler = authHandler;
        this.timer = new HashedWheelTimer();
        this.negotiatorFactory = new NetconfClientSessionNegotiatorFactory(timer,
                Optional.<NetconfHelloMessageAdditionalHeader> absent(), connectionTimeoutMillis);
    }

    public NetconfSshClientDispatcher(AuthenticationHandler authHandler, EventLoopGroup bossGroup,
            EventLoopGroup workerGroup, NetconfHelloMessageAdditionalHeader additionalHeader, long socketTimeoutMillis) {
        super(bossGroup, workerGroup, additionalHeader, socketTimeoutMillis);
        this.authHandler = authHandler;
        this.timer = new HashedWheelTimer();
        this.negotiatorFactory = new NetconfClientSessionNegotiatorFactory(timer, Optional.of(additionalHeader),
                socketTimeoutMillis);
    }

    @Override
    public Future<NetconfClientSession> createClient(InetSocketAddress address,
            final NetconfClientSessionListener sessionListener, ReconnectStrategy strat) {
        return super.createClient(address, strat, new PipelineInitializer<NetconfClientSession>() {

            @Override
            public void initializeChannel(SocketChannel arg0, Promise<NetconfClientSession> arg1) {
                new NetconfSshClientInitializer(authHandler, negotiatorFactory, sessionListener).initialize(arg0, arg1);
            }

        });
    }

    @Override
    public Future<Void> createReconnectingClient(final InetSocketAddress address,
            final NetconfClientSessionListener listener,
            final ReconnectStrategyFactory connectStrategyFactory, final ReconnectStrategy reestablishStrategy) {
        final NetconfSshClientInitializer init = new NetconfSshClientInitializer(authHandler, negotiatorFactory, listener);

        return super.createReconnectingClient(address, connectStrategyFactory, reestablishStrategy,
                new PipelineInitializer<NetconfClientSession>() {
            @Override
            public void initializeChannel(final SocketChannel ch, final Promise<NetconfClientSession> promise) {
                init.initialize(ch, promise);
            }
        });
    }

    private static final class NetconfSshClientInitializer extends AbstractChannelInitializer<NetconfClientSession> {

        private final AuthenticationHandler authenticationHandler;
        private final NetconfClientSessionNegotiatorFactory negotiatorFactory;
        private final NetconfClientSessionListener sessionListener;

        public NetconfSshClientInitializer(AuthenticationHandler authHandler,
                NetconfClientSessionNegotiatorFactory negotiatorFactory,
                final NetconfClientSessionListener sessionListener) {
            this.authenticationHandler = authHandler;
            this.negotiatorFactory = negotiatorFactory;
            this.sessionListener = sessionListener;
        }

        @Override
        public void initialize(SocketChannel ch, Promise<NetconfClientSession> promise) {
            try {
                Invoker invoker = Invoker.subsystem("netconf");
                ch.pipeline().addFirst(new SshHandler(authenticationHandler, invoker));
                super.initialize(ch,promise);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected void initializeSessionNegotiator(SocketChannel ch,
                Promise<NetconfClientSession> promise) {
            ch.pipeline().addAfter(NETCONF_MESSAGE_DECODER,  AbstractChannelInitializer.NETCONF_SESSION_NEGOTIATOR,
                    negotiatorFactory.getSessionNegotiator(new SessionListenerFactory<NetconfClientSessionListener>() {
                @Override
                public NetconfClientSessionListener getSessionListener() {
                    return sessionListener;
                }
            }, ch, promise));
        }
    }
}
