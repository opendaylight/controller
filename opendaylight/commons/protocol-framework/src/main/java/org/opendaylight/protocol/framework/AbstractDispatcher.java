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
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.local.LocalServerChannel;
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
import java.net.SocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dispatcher class for creating servers and clients. The idea is to first create servers and clients and the run the
 * start method that will handle sockets in different thread.
 */
@Deprecated
public abstract class AbstractDispatcher<S extends ProtocolSession<?>, L extends SessionListener<?, ?, ?>> implements Closeable {


    protected interface ChannelPipelineInitializer<CH extends Channel, S extends ProtocolSession<?>> {
        /**
         * Initializes channel by specifying the handlers in its pipeline. Handlers are protocol specific, therefore this
         * method needs to be implemented in protocol specific Dispatchers.
         *
         * @param channel whose pipeline should be defined, also to be passed to {@link SessionNegotiatorFactory}
         * @param promise to be passed to {@link SessionNegotiatorFactory}
         */
        void initializeChannel(CH channel, Promise<S> promise);
    }

    protected interface PipelineInitializer<S extends ProtocolSession<?>> extends ChannelPipelineInitializer<SocketChannel, S> {

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
        return createServer(address, NioServerSocketChannel.class, initializer);
    }

    /**
     * Creates server. Each server needs factories to pass their instances to client sessions.
     *
     * @param address address to which the server should be bound
     * @param channelClass The {@link Class} which is used to create {@link Channel} instances from.
     * @param initializer instance of PipelineInitializer used to initialize the channel pipeline
     *
     * @return ChannelFuture representing the binding process
     */
    protected <CH extends Channel> ChannelFuture createServer(final SocketAddress address, final Class<? extends ServerChannel> channelClass,
            final ChannelPipelineInitializer<CH, S> initializer) {
        final ServerBootstrap b = new ServerBootstrap();
        b.childHandler(new ChannelInitializer<CH>() {

            @Override
            protected void initChannel(final CH ch) {
                initializer.initializeChannel(ch, new DefaultPromise<>(executor));
            }
        });

        b.option(ChannelOption.SO_BACKLOG, 128);
        if (LocalServerChannel.class.equals(channelClass) == false) {
            // makes no sense for LocalServer and produces warning
            b.childOption(ChannelOption.SO_KEEPALIVE, true);
            b.childOption(ChannelOption.TCP_NODELAY , true);
        }
        b.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        customizeBootstrap(b);

        if (b.group() == null) {
            b.group(bossGroup, workerGroup);
        }
        try {
            b.channel(channelClass);
        } catch (final IllegalStateException e) {
            // FIXME: if this is ok, document why
            LOG.trace("Not overriding channelFactory on bootstrap {}", b, e);
        }

        // Bind and start to accept incoming connections.
        final ChannelFuture f = b.bind(address);
        LOG.debug("Initiated server {} at {}.", f, address);
        return f;
    }

    /**
     * Customize a server bootstrap before the server is created. This allows
     * subclasses to assign non-default server options before the server is
     * created.
     *
     * @param b Server bootstrap
     */
    protected void customizeBootstrap(final ServerBootstrap b) {
        // The default is a no-op
    }

    /**
     * Creates a client.
     *
     * @param address remote address
     * @param strategy Reconnection strategy to be used when initial connection fails
     *
     * @return Future representing the connection process. Its result represents the combined success of TCP connection
     *         as well as session negotiation.
     */
    protected Future<S> createClient(final InetSocketAddress address, final ReconnectStrategy strategy, final PipelineInitializer<S> initializer) {
        final Bootstrap b = new Bootstrap();
        final ProtocolSessionPromise<S> p = new ProtocolSessionPromise<>(executor, address, strategy, b);
        b.option(ChannelOption.SO_KEEPALIVE, true).handler(
                new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(final SocketChannel ch) {
                        initializer.initializeChannel(ch, p);
                    }
                });

        customizeBootstrap(b);
        setWorkerGroup(b);
        setChannelFactory(b);

        p.connect();
        LOG.debug("Client created.");
        return p;
    }

    private void setWorkerGroup(final Bootstrap b) {
        if (b.group() == null) {
            b.group(workerGroup);
        }
    }

    /**
     * Create a client but use a pre-configured bootstrap.
     * This method however replaces the ChannelInitializer in the bootstrap. All other configuration is preserved.
     *
     * @param address remote address
     */
    protected Future<S> createClient(final InetSocketAddress address, final ReconnectStrategy strategy, final Bootstrap bootstrap, final PipelineInitializer<S> initializer) {
        final ProtocolSessionPromise<S> p = new ProtocolSessionPromise<>(executor, address, strategy, bootstrap);

        bootstrap.handler(
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
     * Customize a client bootstrap before the connection is attempted. This
     * allows subclasses to assign non-default options before the client is
     * created.
     *
     * @param b Client bootstrap
     */
    protected void customizeBootstrap(final Bootstrap b) {
        // The default is a no-op
    }

    /**
     *
     * @deprecated use {@link org.opendaylight.protocol.framework.AbstractDispatcher#createReconnectingClient(java.net.InetSocketAddress, ReconnectStrategyFactory, org.opendaylight.protocol.framework.AbstractDispatcher.PipelineInitializer)} with only one reconnectStrategyFactory instead.
     *
     * Creates a client.
     *
     * @param address remote address
     * @param connectStrategyFactory Factory for creating reconnection strategy to be used when initial connection fails
     * @param reestablishStrategy Reconnection strategy to be used when the already-established session fails
     *
     * @return Future representing the reconnection task. It will report completion based on reestablishStrategy, e.g.
     *         success if it indicates no further attempts should be made and failure if it reports an error
     */
    @Deprecated
    protected Future<Void> createReconnectingClient(final InetSocketAddress address, final ReconnectStrategyFactory connectStrategyFactory,
            final ReconnectStrategy reestablishStrategy, final PipelineInitializer<S> initializer) {
        return createReconnectingClient(address, connectStrategyFactory, initializer);
    }

    /**
     * Creates a reconnecting client.
     *
     * @param address remote address
     * @param connectStrategyFactory Factory for creating reconnection strategy for every reconnect attempt
     *
     * @return Future representing the reconnection task. It will report completion based on reestablishStrategy, e.g.
     *         success is never reported, only failure when it runs out of reconnection attempts.
     */
    protected Future<Void> createReconnectingClient(final InetSocketAddress address, final ReconnectStrategyFactory connectStrategyFactory,
            final PipelineInitializer<S> initializer) {
        final Bootstrap b = new Bootstrap();

        final ReconnectPromise<S, L> p = new ReconnectPromise<>(GlobalEventExecutor.INSTANCE, this, address, connectStrategyFactory, b, initializer);

        b.option(ChannelOption.SO_KEEPALIVE, true);

        customizeBootstrap(b);
        setWorkerGroup(b);
        setChannelFactory(b);

        p.connect();
        return p;
    }

    private void setChannelFactory(final Bootstrap b) {
        // There is no way to detect if this was already set by
        // customizeBootstrap()
        try {
            b.channel(NioSocketChannel.class);
        } catch (final IllegalStateException e) {
            LOG.trace("Not overriding channelFactory on bootstrap {}", b, e);
        }
    }

    /**
     * @deprecated Should only be used with AbstractDispatcher#AbstractDispatcher()
     */
    @Deprecated
    @Override
    public void close() {
    }
}
