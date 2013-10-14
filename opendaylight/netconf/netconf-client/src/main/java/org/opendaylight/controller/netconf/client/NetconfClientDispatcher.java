/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.client;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
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

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.net.InetSocketAddress;

public class NetconfClientDispatcher extends AbstractDispatcher<NetconfClientSession, NetconfClientSessionListener> {

    private final Optional<SSLContext> maybeContext;
    private final NetconfClientSessionNegotiatorFactory negotatorFactory;

    public NetconfClientDispatcher(final Optional<SSLContext> maybeContext) {
        this.maybeContext = Preconditions.checkNotNull(maybeContext);
        this.negotatorFactory = new NetconfClientSessionNegotiatorFactory(new HashedWheelTimer());
    }

    public Future<NetconfClientSession> createClient(InetSocketAddress address,
            final NetconfClientSessionListener sessionListener, ReconnectStrategy strat) {

        return super.createClient(address, strat, new PipelineInitializer<NetconfClientSession>() {

            @Override
            public void initializeChannel(final SocketChannel ch, final Promise<NetconfClientSession> promise) {
                initialize(ch, promise);
            }

            private void initialize(SocketChannel ch, Promise<NetconfClientSession> promise) {
                new ClientChannelInitializer(maybeContext, negotatorFactory, sessionListener).initialize(ch, promise);
            }
        });
    }

    private static class ClientChannelInitializer extends AbstractChannelInitializer {

        private final NetconfClientSessionNegotiatorFactory negotiatorFactory;
        private final NetconfClientSessionListener sessionListener;

        private ClientChannelInitializer(Optional<SSLContext> maybeContext,
                NetconfClientSessionNegotiatorFactory negotiatorFactory, NetconfClientSessionListener sessionListener) {
            super(maybeContext);
            this.negotiatorFactory = negotiatorFactory;
            this.sessionListener = sessionListener;
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

        @Override
        protected void initSslEngine(SSLEngine sslEngine) {
            sslEngine.setUseClientMode(true);
        }
    }

}
