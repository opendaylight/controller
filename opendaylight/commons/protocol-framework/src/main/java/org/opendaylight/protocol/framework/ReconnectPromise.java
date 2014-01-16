/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import io.netty.channel.ChannelFuture;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

import org.opendaylight.protocol.framework.AbstractDispatcher.PipelineInitializer;

import com.google.common.base.Preconditions;

final class ReconnectPromise<S extends ProtocolSession<?>, L extends SessionListener<?, ?, ?>> extends DefaultPromise<Void> {
    private final AbstractDispatcher<S, L> dispatcher;
    private final InetSocketAddress address;
    private final ReconnectStrategyFactory strategyFactory;
    private final ReconnectStrategy strategy;
    private final PipelineInitializer<S> initializer;
    private Future<?> pending;

    private final AtomicBoolean negotiationFinished = new AtomicBoolean(false);

    public ReconnectPromise(final EventExecutor executor, final AbstractDispatcher<S, L> dispatcher, final InetSocketAddress address,
            final ReconnectStrategyFactory connectStrategyFactory, final ReconnectStrategy reestablishStrategy,
            final PipelineInitializer<S> initializer) {
        super(executor);
        this.dispatcher = Preconditions.checkNotNull(dispatcher);
        this.address = Preconditions.checkNotNull(address);
        this.strategyFactory = Preconditions.checkNotNull(connectStrategyFactory);
        this.strategy = Preconditions.checkNotNull(reestablishStrategy);
        this.initializer = Preconditions.checkNotNull(initializer);
    }

    // FIXME: BUG-190: refactor

    synchronized void connect() {
        negotiationFinished.set(false);

        final ReconnectStrategy cs = this.strategyFactory.createReconnectStrategy();
        final ReconnectStrategy rs = new ReconnectStrategy() {
            @Override
            public Future<Void> scheduleReconnect(final Throwable cause) {
                return cs.scheduleReconnect(cause);
            }

            @Override
            public void reconnectSuccessful() {
                cs.reconnectSuccessful();
            }

            @Override
            public int getConnectTimeout() throws Exception {
                final int cst = cs.getConnectTimeout();
                final int rst = ReconnectPromise.this.strategy.getConnectTimeout();

                if (cst == 0) {
                    return rst;
                }
                if (rst == 0) {
                    return cst;
                }
                return Math.min(cst, rst);
            }
        };

        final Future<S> cf = this.dispatcher.createClient(this.address, rs, new PipelineInitializer<S>() {
            @Override
            public void initializeChannel(final SocketChannel channel, final Promise<S> promise) {
                addChannelClosedListener(channel.closeFuture());
                initializer.initializeChannel(channel, promise);
            }
        });

        final Object lock = this;
        this.pending = cf;

        cf.addListener(new FutureListener<S>() {

            @Override
            public void operationComplete(final Future<S> future) {
                synchronized (lock) {
                    if (!future.isSuccess()) {
                        final Future<Void> rf = ReconnectPromise.this.strategy.scheduleReconnect(cf.cause());

                        if(rf == null) {
                            // This should reflect: no more reconnecting strategies, enough
                            // Currently all reconnect strategies fail with exception, should return null
                            return;
                        }

                        ReconnectPromise.this.pending = rf;

                        rf.addListener(new FutureListener<Void>() {
                            @Override
                            public void operationComplete(final Future<Void> sf) {
                                synchronized (lock) {
                                    /*
                                     * The promise we gave out could have been cancelled,
                                     * which cascades to the reconnect attempt getting
                                     * cancelled, but there is a slight race window, where
                                     * the reconnect attempt is already enqueued, but the
                                     * listener has not yet been notified -- if cancellation
                                     * happens at that point, we need to catch it here.
                                     */
                                    if (!isCancelled()) {
                                        if (sf.isSuccess()) {
                                            connect();
                                        } else {
                                            setFailure(sf.cause());
                                        }
                                    }
                                }
                            }
                        });
                    } else {
                        /*
                         *  FIXME: BUG-190: we have a slight race window with cancellation
                         *         here. Analyze and define its semantics.
                         */
                        ReconnectPromise.this.strategy.reconnectSuccessful();
                        negotiationFinished.set(true);
                    }
                }
            }
        });
    }

    private final ClosedChannelListener closedChannelListener = new ClosedChannelListener();

    class ClosedChannelListener implements Closeable, FutureListener<Void> {

        private final AtomicBoolean stop = new AtomicBoolean(false);

        @Override
        public void operationComplete(final Future<Void> future) throws Exception {
            if (stop.get()) {
                return;
            }

            // Start reconnecting crashed session after negotiation was successful
            if (!negotiationFinished.get()) {
                return;
            }

            connect();
        }

        @Override
        public void close() {
            this.stop.set(true);
        }
    }

    private void addChannelClosedListener(final ChannelFuture channelFuture) {
        channelFuture.addListener(closedChannelListener);
    }

    @Override
    public synchronized boolean cancel(final boolean mayInterruptIfRunning) {
        closedChannelListener.close();

        if (super.cancel(mayInterruptIfRunning)) {
            this.pending.cancel(mayInterruptIfRunning);
            return true;
        }

        return false;
    }
}
