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
import org.opendaylight.controller.netconf.util.handler.ssh.SshHandler;
import org.opendaylight.controller.netconf.util.handler.ssh.authentication.AuthenticationHandler;
import org.opendaylight.controller.netconf.util.handler.ssh.client.Invoker;
import org.opendaylight.protocol.framework.SessionListenerFactory;

import java.io.IOException;

final class SshClientChannelInitializer extends AbstractChannelInitializer<NetconfClientSession> {

    private final AuthenticationHandler authenticationHandler;
    private final NetconfClientSessionNegotiatorFactory negotiatorFactory;
    private final NetconfClientSessionListener sessionListener;

    public SshClientChannelInitializer(final AuthenticationHandler authHandler,
                                       final NetconfClientSessionNegotiatorFactory negotiatorFactory,
                                       final NetconfClientSessionListener sessionListener) {
        this.authenticationHandler = authHandler;
        this.negotiatorFactory = negotiatorFactory;
        this.sessionListener = sessionListener;
    }

    @Override
    public void initialize(final SocketChannel ch, final Promise<NetconfClientSession> promise) {
        try {
            final Invoker invoker = Invoker.subsystem("netconf");
            ch.pipeline().addFirst(new SshHandler(authenticationHandler, invoker));
            super.initialize(ch,promise);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void initializeSessionNegotiator(final SocketChannel ch,
                                               final Promise<NetconfClientSession> promise) {
        ch.pipeline().addAfter(NETCONF_MESSAGE_DECODER,  AbstractChannelInitializer.NETCONF_SESSION_NEGOTIATOR,
                negotiatorFactory.getSessionNegotiator(new SessionListenerFactory<NetconfClientSessionListener>() {
                    @Override
                    public NetconfClientSessionListener getSessionListener() {
                        return sessionListener;
                    }
                }, ch, promise));
    }
}
