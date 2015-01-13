/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.client;

import com.google.common.base.Preconditions;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.Timer;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import java.io.Closeable;
import org.opendaylight.controller.netconf.client.conf.NetconfClientConfiguration;
import org.opendaylight.controller.netconf.client.conf.NetconfReconnectingClientConfiguration;
import org.opendaylight.controller.netconf.client.conf.NetconfReversedClientConfiguration;
import org.opendaylight.protocol.framework.AbstractDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetconfClientDispatcherImpl extends AbstractDispatcher<NetconfClientSession, NetconfClientSessionListener>
        implements NetconfClientDispatcher, Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(NetconfClientDispatcherImpl.class);

    private final Timer timer;

    public NetconfClientDispatcherImpl(final EventLoopGroup bossGroup, final EventLoopGroup workerGroup, final Timer timer) {
        super(bossGroup, workerGroup);
        this.timer = timer;
    }

    @Override
    public Future<NetconfClientSession> createClient(final NetconfClientConfiguration clientConfiguration) {
        switch (clientConfiguration.getProtocol()) {
        case TCP:
            return createTcpClient(clientConfiguration);
        case SSH:
            return createSshClient(clientConfiguration);
        }
        throw new IllegalArgumentException("Unknown client protocol " + clientConfiguration.getProtocol());
    }

    @Override
    public Future<NetconfClientSession> createReversedClient(final NetconfReversedClientConfiguration clientConfiguration) {
        Preconditions.checkArgument(clientConfiguration.getProtocol() == NetconfClientConfiguration.NetconfClientProtocol.SSH, "Reversed client is only available with SSH");
        return createReversedSshClient(clientConfiguration);
    }

    @Override
    public Future<Void> createReconnectingClient(final NetconfReconnectingClientConfiguration clientConfiguration) {
        switch (clientConfiguration.getProtocol()) {
        case TCP:
            return createReconnectingTcpClient(clientConfiguration);
        case SSH:
            return createReconnectingSshClient(clientConfiguration);
        default:
            throw new IllegalArgumentException("Unknown client protocol " + clientConfiguration.getProtocol());
        }
    }

    private Future<NetconfClientSession> createTcpClient(final NetconfClientConfiguration currentConfiguration) {
        LOG.debug("Creating TCP client with configuration: {}", currentConfiguration);
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
        LOG.debug("Creating reconnecting TCP client with configuration: {}", currentConfiguration);
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
        LOG.debug("Creating SSH client with configuration: {}", currentConfiguration);
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

    private Future<NetconfClientSession> createReversedSshClient(final NetconfReversedClientConfiguration currentConfiguration) {
        LOG.debug("Creating reversed SSH client with configuration: {}", currentConfiguration);
        return super.createClient(currentConfiguration.getAddress(), currentConfiguration.getReconnectStrategy(),
                new PipelineInitializer<NetconfClientSession>() {

                    @Override
                    public void initializeChannel(final SocketChannel ch,
                                                  final Promise<NetconfClientSession> sessionPromise) {
                        new ReversedSshClientChannelInitializer(currentConfiguration.getAuthHandler(),
                                getNegotiatorFactory(currentConfiguration), currentConfiguration.getSessionListener(),
                                currentConfiguration.getTcpSession())
                                .initialize(ch, sessionPromise);
                    }

                });
    }

    private Future<Void> createReconnectingSshClient(final NetconfReconnectingClientConfiguration currentConfiguration) {
        LOG.debug("Creating reconnecting SSH client with configuration: {}", currentConfiguration);
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
}
