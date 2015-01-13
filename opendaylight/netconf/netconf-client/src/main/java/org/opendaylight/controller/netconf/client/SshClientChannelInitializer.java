/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.util.concurrent.Promise;
import java.io.IOException;
import org.opendaylight.controller.netconf.nettyutil.AbstractChannelInitializer;
import org.opendaylight.controller.netconf.nettyutil.handler.ssh.authentication.AuthenticationHandler;
import org.opendaylight.controller.netconf.nettyutil.handler.ssh.client.AsyncSshHandler;
import org.opendaylight.protocol.framework.SessionListenerFactory;

class SshClientChannelInitializer extends AbstractChannelInitializer<NetconfClientSession> {

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
    public void initialize(final Channel ch, final Promise<NetconfClientSession> promise) {
        try {
            // ssh handler has to be the first handler in pipeline
            ch.pipeline().addFirst(getSshHandler());
            super.initialize(ch,promise);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected ChannelHandler getSshHandler() throws IOException {
        return AsyncSshHandler.createForNetconfSubsystem(getAuthenticationHandler());
    }

    protected AuthenticationHandler getAuthenticationHandler() {
        return authenticationHandler;
    }

    @Override
    protected void initializeSessionNegotiator(final Channel ch,
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
