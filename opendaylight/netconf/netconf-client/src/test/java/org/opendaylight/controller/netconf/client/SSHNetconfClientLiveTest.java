/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.controller.netconf.client.conf.NetconfClientConfiguration;
import org.opendaylight.controller.netconf.client.conf.NetconfClientConfigurationBuilder;
import org.opendaylight.controller.netconf.util.handler.ssh.authentication.LoginPassword;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.TimedReconnectStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

@Ignore
public class SSHNetconfClientLiveTest {
    private static final Logger logger = LoggerFactory.getLogger(SSHNetconfClientLiveTest.class);

    NioEventLoopGroup nettyThreadgroup;
    NetconfClientDispatcher netconfClientDispatcher;
    InetSocketAddress address;
    final int connectionAttempts = 10, attemptMsTimeout = 1000;
    final int connectionTimeoutMillis = 20000;

    @Before
    public void setUp() {
        nettyThreadgroup = new NioEventLoopGroup();

        netconfClientDispatcher = new NetconfClientDispatcherImpl(nettyThreadgroup, nettyThreadgroup);

        address = new InetSocketAddress(System.getProperty("host"), Integer.parseInt(System.getProperty("port")));
    }

    @Ignore
    @Test
    public void test() throws Exception {
        //runnable.run();
    }

    @Test
    public void testInExecutor() throws Exception {
        final int threads = 4;
        final ExecutorService executorService = Executors.newFixedThreadPool(threads);
        try {
            for (int i= 0;i< threads;i++) {
                final InetSocketAddress address = new InetSocketAddress(System.getProperty("host"),
                        Integer.parseInt(System.getProperty("port")));
                final NetconfRunnable runnable = new NetconfRunnable(address);
                executorService.execute(runnable);
            }
            executorService.shutdown();
            executorService.awaitTermination(1, TimeUnit.MINUTES);


        } finally {
            executorService.shutdownNow();
        }
    }

    final class NetconfRunnable implements Runnable {
        private final InetSocketAddress address;

        NetconfRunnable(final InetSocketAddress address) {
            this.address = address;
        }

        @Override
        public void run() {
            try (NetconfClient netconfClient = new NetconfClient(address.toString(), netconfClientDispatcher, getClientConfig());) {
                logger.info("OK {}", address);
            } catch (InterruptedException | IOException e) {
                logger.error("Failed {}", address, e);
            }
        }

        public NetconfClientConfiguration getClientConfig() {
            final NetconfClientConfigurationBuilder b = NetconfClientConfigurationBuilder.create();
            b.withAddress(address);
            b.withConnectionTimeoutMillis(connectionTimeoutMillis);
            b.withAuthHandler(new LoginPassword(System.getProperty("username"), System.getProperty("password")));
            b.withSessionListener(new SimpleNetconfClientSessionListener());
            b.withProtocol(NetconfClientConfiguration.NetconfClientProtocol.SSH);
            b.withReconnectStrategy(getReconnectStrategy(connectionAttempts, attemptMsTimeout));
            return b.build();
        }

        private ReconnectStrategy getReconnectStrategy(final int connectionAttempts, final int attemptMsTimeout) {
            return new TimedReconnectStrategy(GlobalEventExecutor.INSTANCE, attemptMsTimeout, 1000, 1.0, null,
                    (long) connectionAttempts, null);
        }
    }
}
