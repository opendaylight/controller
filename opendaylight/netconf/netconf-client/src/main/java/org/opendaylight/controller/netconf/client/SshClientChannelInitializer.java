/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import java.io.IOException;
import org.opendaylight.controller.netconf.nettyutil.AbstractChannelInitializer;
import org.opendaylight.controller.netconf.nettyutil.handler.ssh.authentication.AuthenticationHandler;
import org.opendaylight.controller.netconf.nettyutil.handler.ssh.client.AsyncSshHandler;
import org.opendaylight.protocol.framework.SessionListenerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SshClientChannelInitializer extends AbstractChannelInitializer<NetconfClientSession> {

    private static final Logger LOG = LoggerFactory.getLogger(SshClientChannelInitializer.class);

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
            LOG.info("initialize channel {} - promise {}", ch, promise);
            ch.pipeline().addFirst(AsyncSshHandler.createForNetconfSubsystem(authenticationHandler));
            ch.closeFuture().addListener(new GenericFutureListener<ChannelFuture>() {
                @Override
                public void operationComplete(final ChannelFuture future) throws Exception {
                    if(future.isSuccess()) {
                        LOG.debug("Channel {} closed: success", future.channel());
                        if (promise != null && !promise.isDone()) {
//                            LOG.info("cancel promise {}", promise);
//                            promise.cancel(true);
                            future.channel().eventLoop().shutdownGracefully();
                        }
                    } else {
                        LOG.warn("Channel {} closed: fail", future.channel());
                    }
                }
            });
            super.initialize(ch,promise);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void initializeSessionNegotiator(final Channel ch,
                                               final Promise<NetconfClientSession> promise) {
        LOG.info("initializeSessionNegotiator channel {} - promise {}", ch, promise);

        ch.pipeline().addAfter(NETCONF_MESSAGE_DECODER,  AbstractChannelInitializer.NETCONF_SESSION_NEGOTIATOR,
                negotiatorFactory.getSessionNegotiator(new SessionListenerFactory<NetconfClientSessionListener>() {
                    @Override
                    public NetconfClientSessionListener getSessionListener() {
                        return sessionListener;
                    }
                }, ch, promise));
        ch.closeFuture().addListener(new GenericFutureListener<ChannelFuture>() {
            @Override
            public void operationComplete(final ChannelFuture future) throws Exception {
                if(future.isSuccess()) {
                    LOG.debug("Channel {} initializeSessionNegotiator closed: success", future.channel());
                    if (promise != null && !promise.isDone()) {
//                        LOG.info("cancel promise {}", promise);
//                        promise.cancel(true);
                        future.channel().eventLoop().shutdownGracefully();
                    }
                } else {
                    LOG.warn("Channel {} initializeSessionNegotiator closed: fail", future.channel());
                }
            }
        });
    }
}
