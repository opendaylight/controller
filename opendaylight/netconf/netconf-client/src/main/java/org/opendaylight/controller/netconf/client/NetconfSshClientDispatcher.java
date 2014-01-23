/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.client;

import com.google.common.base.Optional;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.api.NetconfSession;
import org.opendaylight.controller.netconf.api.NetconfTerminationReason;
import org.opendaylight.controller.netconf.util.AbstractChannelInitializer;
import org.opendaylight.controller.netconf.util.handler.ssh.SshHandler;
import org.opendaylight.controller.netconf.util.handler.ssh.authentication.AuthenticationHandler;
import org.opendaylight.controller.netconf.util.handler.ssh.client.Invoker;
import org.opendaylight.controller.netconf.util.messages.NetconfMessageFactory;
import org.opendaylight.protocol.framework.ProtocolHandlerFactory;
import org.opendaylight.protocol.framework.ProtocolMessageDecoder;
import org.opendaylight.protocol.framework.ProtocolMessageEncoder;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.SessionListener;
import org.opendaylight.protocol.framework.SessionListenerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;

public class NetconfSshClientDispatcher extends NetconfClientDispatcher {

    private AuthenticationHandler authHandler;
    private HashedWheelTimer timer;
    private NetconfClientSessionNegotiatorFactory negotatorFactory;

    public NetconfSshClientDispatcher(AuthenticationHandler authHandler, EventLoopGroup bossGroup,
            EventLoopGroup workerGroup, long connectionTimeoutMillis) {
        super(bossGroup, workerGroup, connectionTimeoutMillis);
        this.authHandler = authHandler;
        this.timer = new HashedWheelTimer();
        this.negotatorFactory = new NetconfClientSessionNegotiatorFactory(timer, Optional.<String>absent(), connectionTimeoutMillis);
    }

    public NetconfSshClientDispatcher(AuthenticationHandler authHandler, EventLoopGroup bossGroup,
            EventLoopGroup workerGroup, String additionalHeader, long socketTimeoutMillis) {
        super(bossGroup, workerGroup, additionalHeader, socketTimeoutMillis);
        this.authHandler = authHandler;
        this.timer = new HashedWheelTimer();
        this.negotatorFactory = new NetconfClientSessionNegotiatorFactory(timer, Optional.of(additionalHeader), socketTimeoutMillis);
    }

    public Future<NetconfClientSession> createClient(InetSocketAddress address,
            final NetconfClientSessionListener sessionListener, ReconnectStrategy strat) {
        return super.createClient(address, strat, new PipelineInitializer<NetconfClientSession>() {

            @Override
            public void initializeChannel(SocketChannel arg0, Promise<NetconfClientSession> arg1) {
                new NetconfSshClientInitializer(authHandler, negotatorFactory, sessionListener).initialize(arg0, arg1);
            }

        });
    }

    private static final class NetconfSshClientInitializer extends AbstractChannelInitializer {

        private final NetconfHandlerFactory handlerFactory;
        private final AuthenticationHandler authenticationHandler;
        private final NetconfClientSessionNegotiatorFactory negotiatorFactory;
        private final NetconfClientSessionListener sessionListener;

        public NetconfSshClientInitializer(AuthenticationHandler authHandler,
                NetconfClientSessionNegotiatorFactory negotiatorFactory,
                final NetconfClientSessionListener sessionListener) {
            this.handlerFactory = new NetconfHandlerFactory(new NetconfMessageFactory());
            this.authenticationHandler = authHandler;
            this.negotiatorFactory = negotiatorFactory;
            this.sessionListener = sessionListener;
        }

        @Override
        public void initialize(SocketChannel ch, Promise<? extends NetconfSession> promise) {
            try {
                Invoker invoker = Invoker.subsystem("netconf");
                ch.pipeline().addFirst(new SshHandler(authenticationHandler, invoker));
                super.initialize(ch,promise);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
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
