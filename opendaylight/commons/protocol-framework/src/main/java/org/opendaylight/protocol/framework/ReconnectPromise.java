/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import com.google.common.base.Preconditions;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import java.net.InetSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
final class ReconnectPromise<S extends ProtocolSession<?>, L extends SessionListener<?, ?, ?>> extends DefaultPromise<Void> {
    private static final Logger LOG = LoggerFactory.getLogger(ReconnectPromise.class);

    private final AbstractDispatcher<S, L> dispatcher;
    private final InetSocketAddress address;
    private final ReconnectStrategyFactory strategyFactory;
    private final Bootstrap b;
    private final AbstractDispatcher.PipelineInitializer<S> initializer;
    private Future<?> pending;

    public ReconnectPromise(final EventExecutor executor, final AbstractDispatcher<S, L> dispatcher, final InetSocketAddress address,
                            final ReconnectStrategyFactory connectStrategyFactory, final Bootstrap b, final AbstractDispatcher.PipelineInitializer<S> initializer) {
        super(executor);
        this.b = b;
        this.initializer = Preconditions.checkNotNull(initializer);
        this.dispatcher = Preconditions.checkNotNull(dispatcher);
        this.address = Preconditions.checkNotNull(address);
        this.strategyFactory = Preconditions.checkNotNull(connectStrategyFactory);
    }

    synchronized void connect() {
        final ReconnectStrategy cs = this.strategyFactory.createReconnectStrategy();

        // Set up a client with pre-configured bootstrap, but add a closed channel handler into the pipeline to support reconnect attempts
        pending = this.dispatcher.createClient(this.address, cs, b, (channel, promise) -> {
            initializer.initializeChannel(channel, promise);
            // add closed channel handler
            // This handler has to be added as last channel handler and the channel inactive event has to be caught by it
            // Handlers in front of it can react to channelInactive event, but have to forward the event or the reconnect will not work
            // This handler is last so all handlers in front of it can handle channel inactive (to e.g. resource cleanup) before a new connection is started
            channel.pipeline().addLast(new ClosedChannelHandler(ReconnectPromise.this));
        });

        pending.addListener((GenericFutureListener<Future<Object>>) future -> {
            if (!future.isSuccess()) {
                ReconnectPromise.this.setFailure(future.cause());
            }
        });
    }

    /**
     *
     * @return true if initial connection was established successfully, false if initial connection failed due to e.g. Connection refused, Negotiation failed
     */
    private boolean isInitialConnectFinished() {
        Preconditions.checkNotNull(pending);
        return pending.isDone() && pending.isSuccess();
    }

    @Override
    public synchronized boolean cancel(final boolean mayInterruptIfRunning) {
        if (super.cancel(mayInterruptIfRunning)) {
            Preconditions.checkNotNull(pending);
            this.pending.cancel(mayInterruptIfRunning);
            return true;
        }

        return false;
    }

    /**
     * Channel handler that responds to channelInactive event and reconnects the session.
     * Only if the promise was not canceled.
     */
    private static final class ClosedChannelHandler extends ChannelInboundHandlerAdapter {
        private final ReconnectPromise<?, ?> promise;

        public ClosedChannelHandler(final ReconnectPromise<?, ?> promise) {
            this.promise = promise;
        }

        @Override
        public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
            // This is the ultimate channel inactive handler, not forwarding
            if (promise.isCancelled()) {
                return;
            }

            if (promise.isInitialConnectFinished() == false) {
                LOG.debug("Connection to {} was dropped during negotiation, reattempting", promise.address);
            }

            LOG.debug("Reconnecting after connection to {} was dropped", promise.address);
            promise.connect();
        }
    }

}
