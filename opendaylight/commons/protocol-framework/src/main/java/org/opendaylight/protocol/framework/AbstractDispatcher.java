/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;

import java.io.Closeable;
import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * Dispatcher class for creating servers and clients. The idea is to first create servers and clients and the run the
 * start method that will handle sockets in different thread.
 */
public abstract class AbstractDispatcher<S extends ProtocolSession<?>, L extends SessionListener<?, ?, ?>> implements Closeable {

    protected interface PipelineInitializer<S extends ProtocolSession<?>> {
        /**
         * Initializes channel by specifying the handlers in its pipeline. Handlers are protocol specific, therefore this
         * method needs to be implemented in protocol specific Dispatchers.
         *
         * @param channel whose pipeline should be defined, also to be passed to {@link SessionNegotiatorFactory}
         * @param promise to be passed to {@link SessionNegotiatorFactory}
         */
        void initializeChannel(SocketChannel channel, Promise<S> promise);
    }


    private static final Logger LOG = LoggerFactory.getLogger(AbstractDispatcher.class);

    private final EventLoopGroup bossGroup;

    private final EventLoopGroup workerGroup;

    private final EventExecutor executor;

    protected AbstractDispatcher(final EventLoopGroup bossGroup, final EventLoopGroup workerGroup) {
        this(GlobalEventExecutor.INSTANCE, bossGroup, workerGroup);
    }

    protected AbstractDispatcher(final EventExecutor executor, final EventLoopGroup bossGroup, final EventLoopGroup workerGroup) {
        this.bossGroup = Preconditions.checkNotNull(bossGroup);
        this.workerGroup = Preconditions.checkNotNull(workerGroup);
        this.executor = Preconditions.checkNotNull(executor);
    }


    /**
     * Creates server. Each server needs factories to pass their instances to client sessions.
     *
     * @param address address to which the server should be bound
     * @param initializer instance of PipelineInitializer used to initialize the channel pipeline
     *
     * @return ChannelFuture representing the binding process
     */
    protected ChannelFuture createServer(final InetSocketAddress address, final PipelineInitializer<S> initializer) {
        final ServerBootstrap b = new ServerBootstrap();
        b.group(this.bossGroup, this.workerGroup);
        b.channel(NioServerSocketChannel.class);
        b.option(ChannelOption.SO_BACKLOG, 128);
        b.childHandler(new ChannelInitializer<SocketChannel>() {

            @Override
            protected void initChannel(final SocketChannel ch) {
                initializer.initializeChannel(ch, new DefaultPromise<S>(executor));
            }
        });
        b.childOption(ChannelOption.SO_KEEPALIVE, true);

        // Bind and start to accept incoming connections.
        final ChannelFuture f = b.bind(address);
        LOG.debug("Initiated server {} at {}.", f, address);
        return f;

    }

    /**
     * Creates a client.
     *
     * @param address remote address
     * @param connectStrategy Reconnection strategy to be used when initial connection fails
     *
     * @return Future representing the connection process. Its result represents the combined success of TCP connection
     *         as well as session negotiation.
     */
    protected Future<S> createClient(final InetSocketAddress address, final ReconnectStrategy strategy, final PipelineInitializer<S> initializer) {
        final Bootstrap b = new Bootstrap();
        final ProtocolSessionPromise<S> p = new ProtocolSessionPromise<S>(executor, address, strategy, b);
        b.group(this.workerGroup).channel(NioSocketChannel.class).option(ChannelOption.SO_KEEPALIVE, true).handler(
                new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(final SocketChannel ch) {
                        initializer.initializeChannel(ch, p);
                    }
                });
        p.connect();
        LOG.debug("Client created.");
        return p;
    }

    /**
     * Creates a client.
     *
     * @param address remote address
     * @param connectStrategyFactory Factory for creating reconnection strategy to be used when initial connection fails
     * @param reestablishStrategy Reconnection strategy to be used when the already-established session fails
     *
     * @return Future representing the reconnection task. It will report completion based on reestablishStrategy, e.g.
     *         success if it indicates no further attempts should be made and failure if it reports an error
     */
    protected Future<Void> createReconnectingClient(final InetSocketAddress address, final ReconnectStrategyFactory connectStrategyFactory,
            final ReconnectStrategy reestablishStrategy, final PipelineInitializer<S> initializer) {

        final ReconnectPromise<S, L> p = new ReconnectPromise<S, L>(GlobalEventExecutor.INSTANCE, this, address, connectStrategyFactory, reestablishStrategy, initializer);
        p.connect();

        return p;

    }

    /**
     * @deprecated Should only be used with {@link AbstractDispatcher#AbstractDispatcher()}
     */
    @Deprecated
    @Override
    public void close() {
        try {
            this.workerGroup.shutdownGracefully();
        } finally {
            this.bossGroup.shutdownGracefully();
        }
    }

}
