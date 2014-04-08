/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.client;

import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.Promise;
import org.opendaylight.controller.netconf.util.AbstractChannelInitializer;
import org.opendaylight.protocol.framework.SessionListenerFactory;

class TcpClientChannelInitializer extends AbstractChannelInitializer<NetconfClientSession> {

    private final NetconfClientSessionNegotiatorFactory negotiatorFactory;
    private final NetconfClientSessionListener sessionListener;

    TcpClientChannelInitializer(final NetconfClientSessionNegotiatorFactory negotiatorFactory,
                                final NetconfClientSessionListener sessionListener) {
        this.negotiatorFactory = negotiatorFactory;
        this.sessionListener = sessionListener;
    }

    @Override
    public void initialize(final SocketChannel ch, final Promise<NetconfClientSession> promise) {
        super.initialize(ch, promise);
    }

    @Override
    protected void initializeSessionNegotiator(final SocketChannel ch, final Promise<NetconfClientSession> promise) {
        ch.pipeline().addAfter(NETCONF_MESSAGE_DECODER, AbstractChannelInitializer.NETCONF_SESSION_NEGOTIATOR,
                negotiatorFactory.getSessionNegotiator(new SessionListenerFactory<NetconfClientSessionListener>() {
                    @Override
                    public NetconfClientSessionListener getSessionListener() {
                        return sessionListener;
                    }
                }, ch, promise));
    }
}
