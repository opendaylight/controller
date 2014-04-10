/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.client;

import java.io.Closeable;

import org.opendaylight.controller.netconf.client.conf.NetconfClientConfiguration;
import org.opendaylight.controller.netconf.client.conf.NetconfReconnectingClientConfiguration;
import org.opendaylight.protocol.framework.AbstractDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

public class NetconfClientDispatcherImpl extends AbstractDispatcher<NetconfClientSession, NetconfClientSessionListener>
        implements NetconfClientDispatcher, Closeable {

    private static final Logger logger = LoggerFactory.getLogger(NetconfClientDispatcherImpl.class);

    private final HashedWheelTimer timer;

    public NetconfClientDispatcherImpl(final EventLoopGroup bossGroup, final EventLoopGroup workerGroup) {
        super(bossGroup, workerGroup);
        timer = new HashedWheelTimer();
    }

    @Override
    public Future<NetconfClientSession> createClient(final NetconfClientConfiguration clientConfiguration) {
        switch (clientConfiguration.getProtocol()) {
            case TCP: return createTcpClient(clientConfiguration);
            case SSH: return createSshClient(clientConfiguration);
            default: throw new IllegalArgumentException("Unknown client protocol " + clientConfiguration.getProtocol());
        }
    }

    @Override
    public Future<Void> createReconnectingClient(final NetconfReconnectingClientConfiguration clientConfiguration) {
        switch (clientConfiguration.getProtocol()) {
            case TCP: return createReconnectingTcpClient(clientConfiguration);
            case SSH: return createReconnectingSshClient(clientConfiguration);
            default: throw new IllegalArgumentException("Unknown client protocol " + clientConfiguration.getProtocol());
        }
    }

    private Future<NetconfClientSession> createTcpClient(final NetconfClientConfiguration currentConfiguration) {
        logger.debug("Creating TCP client with configuration: {}", currentConfiguration);
        return super.createClient(currentConfiguration.getAddress(), currentConfiguration.getReconnectStrategy(),
                new PipelineInitializer<NetconfClientSession>() {

                    @Override
                    public void initializeChannel(final SocketChannel ch, final Promise<NetconfClientSession> promise) {
                        initialize(ch, promise);
                    }

                    private void initialize(final SocketChannel ch, final Promise<NetconfClientSession> promise) {
                        new TcpClientChannelInitializer(getNegotiatorFactory(currentConfiguration), currentConfiguration
                                .getSessionListener()).initialize(ch, promise);
                    }
                });
    }

    private Future<Void> createReconnectingTcpClient(final NetconfReconnectingClientConfiguration currentConfiguration) {
        logger.debug("Creating reconnecting TCP client with configuration: {}", currentConfiguration);
        final TcpClientChannelInitializer init = new TcpClientChannelInitializer(getNegotiatorFactory(currentConfiguration),
                currentConfiguration.getSessionListener());

        return super.createReconnectingClient(currentConfiguration.getAddress(), currentConfiguration.getConnectStrategyFactory(),
                currentConfiguration.getReconnectStrategy(), new PipelineInitializer<NetconfClientSession>() {
                    @Override
                    public void initializeChannel(final SocketChannel ch, final Promise<NetconfClientSession> promise) {
                        init.initialize(ch, promise);
                    }
                });
    }

    private Future<NetconfClientSession> createSshClient(final NetconfClientConfiguration currentConfiguration) {
        logger.debug("Creating SSH client with configuration: {}", currentConfiguration);
        return super.createClient(currentConfiguration.getAddress(), currentConfiguration.getReconnectStrategy(),
                new PipelineInitializer<NetconfClientSession>() {

                    @Override
                    public void initializeChannel(final SocketChannel ch,
                                                  final Promise<NetconfClientSession> sessionPromise) {
                        new SshClientChannelInitializer(currentConfiguration.getAuthHandler(),
                                getNegotiatorFactory(currentConfiguration), currentConfiguration.getSessionListener())
                                .initialize(ch, sessionPromise);
                    }

                });
    }

    private Future<Void> createReconnectingSshClient(final NetconfReconnectingClientConfiguration currentConfiguration) {
        logger.debug("Creating reconnecting SSH client with configuration: {}", currentConfiguration);
        final SshClientChannelInitializer init = new SshClientChannelInitializer(currentConfiguration.getAuthHandler(),
                getNegotiatorFactory(currentConfiguration), currentConfiguration.getSessionListener());

        return super.createReconnectingClient(currentConfiguration.getAddress(), currentConfiguration.getConnectStrategyFactory(), currentConfiguration.getReconnectStrategy(),
                new PipelineInitializer<NetconfClientSession>() {
                    @Override
                    public void initializeChannel(final SocketChannel ch, final Promise<NetconfClientSession> promise) {
                        init.initialize(ch, promise);
                    }
                });
    }

    protected NetconfClientSessionNegotiatorFactory getNegotiatorFactory(final NetconfClientConfiguration cfg) {
        return new NetconfClientSessionNegotiatorFactory(timer, cfg.getAdditionalHeader(),
                cfg.getConnectionTimeoutMillis());
    }

    @Override
    public void close() {
        try {
            timer.stop();
        } catch (final Exception e) {
            logger.debug("Ignoring exception while closing {}", timer, e);
        }
    }
}
