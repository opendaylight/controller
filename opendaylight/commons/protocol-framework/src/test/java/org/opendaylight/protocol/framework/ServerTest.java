/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.protocol.framework;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.SucceededFuture;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ServerTest {
    SimpleDispatcher clientDispatcher, dispatcher;

    SimpleSession session = null;

    ChannelFuture server = null;

    InetSocketAddress serverAddress;
    private NioEventLoopGroup eventLoopGroup;
    // Dedicated loop group for server, needed for testing reconnection client
    // With dedicated server group we can simulate session drop by shutting only the server group down
    private NioEventLoopGroup serverLoopGroup;

    @Before
    public void setUp() {
        final int port = 10000 + (int)(10000 * Math.random());
        serverAddress = new InetSocketAddress("127.0.0.1", port);
        eventLoopGroup = new NioEventLoopGroup();
        serverLoopGroup = new NioEventLoopGroup();
    }

    @After
    public void tearDown() throws IOException, InterruptedException, ExecutionException {
        if(server != null) {
            this.server.channel().close();
        }
        this.eventLoopGroup.shutdownGracefully().get();
        this.serverLoopGroup.shutdownGracefully().get();
        try {
            Thread.sleep(500);
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testConnectionRefused() throws Exception {
        this.clientDispatcher = getClientDispatcher();

        final ReconnectStrategy mockReconnectStrategy = getMockedReconnectStrategy();

        this.clientDispatcher.createClient(this.serverAddress, mockReconnectStrategy, SimpleSessionListener::new);

        Mockito.verify(mockReconnectStrategy, timeout(5000).atLeast(2)).scheduleReconnect(any(Throwable.class));
    }

    @Test
    public void testConnectionReestablishInitial() throws Exception {
        this.clientDispatcher = getClientDispatcher();

        final ReconnectStrategy mockReconnectStrategy = getMockedReconnectStrategy();

        this.clientDispatcher.createClient(this.serverAddress, mockReconnectStrategy, SimpleSessionListener::new);

        Mockito.verify(mockReconnectStrategy, timeout(5000).atLeast(2)).scheduleReconnect(any(Throwable.class));

        final Promise<Boolean> p = new DefaultPromise<>(GlobalEventExecutor.INSTANCE);
        this.dispatcher = getServerDispatcher(p);

        this.server = this.dispatcher.createServer(this.serverAddress, SimpleSessionListener::new);

        this.server.get();

        assertEquals(true, p.get(3, TimeUnit.SECONDS));
    }

    @Test
    public void testConnectionDrop() throws Exception {
        final Promise<Boolean> p = new DefaultPromise<>(GlobalEventExecutor.INSTANCE);

        this.dispatcher = getServerDispatcher(p);

        this.server = this.dispatcher.createServer(this.serverAddress, SimpleSessionListener::new);

        this.server.get();

        this.clientDispatcher = getClientDispatcher();

        final ReconnectStrategy reconnectStrategy = getMockedReconnectStrategy();
        this.session = this.clientDispatcher.createClient(this.serverAddress,
                reconnectStrategy, SimpleSessionListener::new).get(6, TimeUnit.SECONDS);

        assertEquals(true, p.get(3, TimeUnit.SECONDS));

        shutdownServer();

        // No reconnect should be scheduled after server drops connection with not-reconnecting client
        verify(reconnectStrategy, times(0)).scheduleReconnect(any(Throwable.class));
    }

    @Test
    public void testConnectionReestablishAfterDrop() throws Exception {
        final Promise<Boolean> p = new DefaultPromise<>(GlobalEventExecutor.INSTANCE);

        this.dispatcher = getServerDispatcher(p);

        this.server = this.dispatcher.createServer(this.serverAddress, SimpleSessionListener::new);

        this.server.get();

        this.clientDispatcher = getClientDispatcher();

        final ReconnectStrategyFactory reconnectStrategyFactory = mock(ReconnectStrategyFactory.class);
        final ReconnectStrategy reconnectStrategy = getMockedReconnectStrategy();
        doReturn(reconnectStrategy).when(reconnectStrategyFactory).createReconnectStrategy();

        this.clientDispatcher.createReconnectingClient(this.serverAddress,
                reconnectStrategyFactory, SimpleSessionListener::new);

        assertEquals(true, p.get(3, TimeUnit.SECONDS));
        shutdownServer();

        verify(reconnectStrategyFactory, timeout(20000).atLeast(2)).createReconnectStrategy();
    }

    @Test
    public void testConnectionEstablished() throws Exception {
        final Promise<Boolean> p = new DefaultPromise<>(GlobalEventExecutor.INSTANCE);

        this.dispatcher = getServerDispatcher(p);

        this.server = this.dispatcher.createServer(this.serverAddress, SimpleSessionListener::new);

        this.server.get();

        this.clientDispatcher = getClientDispatcher();

        this.session = this.clientDispatcher.createClient(this.serverAddress,
                new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE, 5000), SimpleSessionListener::new).get(6,
                TimeUnit.SECONDS);

        assertEquals(true, p.get(3, TimeUnit.SECONDS));
    }

    @Test
    public void testConnectionFailed() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        final Promise<Boolean> p = new DefaultPromise<>(GlobalEventExecutor.INSTANCE);

        this.dispatcher = getServerDispatcher(p);

        this.server = this.dispatcher.createServer(this.serverAddress, SimpleSessionListener::new);

        this.server.get();

        this.clientDispatcher = getClientDispatcher();

        this.session = this.clientDispatcher.createClient(this.serverAddress,
                new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE, 5000), SimpleSessionListener::new).get(6,
                TimeUnit.SECONDS);

        final Future<?> session = this.clientDispatcher.createClient(this.serverAddress,
                new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE, 5000), SimpleSessionListener::new);
        assertFalse(session.isSuccess());
    }

    @Test
    public void testNegotiationFailedReconnect() throws Exception {
        final Promise<Boolean> p = new DefaultPromise<>(GlobalEventExecutor.INSTANCE);

        this.dispatcher = getServerDispatcher(p);

        this.server = this.dispatcher.createServer(this.serverAddress, SimpleSessionListener::new);

        this.server.get();

        this.clientDispatcher = new SimpleDispatcher(
                (factory, channel, promise) -> new SimpleSessionNegotiator(promise, channel) {
                    @Override
                    protected void startNegotiation() throws Exception {
                        negotiationFailed(new IllegalStateException("Negotiation failed"));
                    }
                }, new DefaultPromise<>(GlobalEventExecutor.INSTANCE), eventLoopGroup);

        final ReconnectStrategyFactory reconnectStrategyFactory = mock(ReconnectStrategyFactory.class);
        final ReconnectStrategy reconnectStrategy = getMockedReconnectStrategy();
        doReturn(reconnectStrategy).when(reconnectStrategyFactory).createReconnectStrategy();

        this.clientDispatcher.createReconnectingClient(this.serverAddress,
                reconnectStrategyFactory, SimpleSessionListener::new);


        // Reconnect strategy should be consulted at least twice, for initial connect and reconnect attempts after drop
        verify(reconnectStrategyFactory, timeout((int) TimeUnit.MINUTES.toMillis(3)).atLeast(2)).createReconnectStrategy();
    }

    private SimpleDispatcher getClientDispatcher() {
        return new SimpleDispatcher((factory, channel, promise) -> new SimpleSessionNegotiator(promise, channel), new DefaultPromise<>(GlobalEventExecutor.INSTANCE), eventLoopGroup);
    }

    private ReconnectStrategy getMockedReconnectStrategy() throws Exception {
        final ReconnectStrategy mockReconnectStrategy = mock(ReconnectStrategy.class);
        final Future<Void> future = new SucceededFuture<>(GlobalEventExecutor.INSTANCE, null);
        doReturn(future).when(mockReconnectStrategy).scheduleReconnect(any(Throwable.class));
        doReturn(5000).when(mockReconnectStrategy).getConnectTimeout();
        doNothing().when(mockReconnectStrategy).reconnectSuccessful();
        return mockReconnectStrategy;
    }


    private void shutdownServer() throws InterruptedException, ExecutionException {
        // Shutdown server
        server.channel().close().get();
        // Closing server channel does not close established connections, eventLoop has to be closed as well to simulate dropped session
        serverLoopGroup.shutdownGracefully().get();
    }

    private SimpleDispatcher getServerDispatcher(final Promise<Boolean> p) {
        return new SimpleDispatcher((factory, channel, promise) -> {
            p.setSuccess(true);
            return new SimpleSessionNegotiator(promise, channel);
        }, null, serverLoopGroup);
    }

}
