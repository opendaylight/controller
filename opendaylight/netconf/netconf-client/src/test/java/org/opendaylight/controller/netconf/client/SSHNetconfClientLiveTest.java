/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.client;

import io.netty.channel.nio.NioEventLoopGroup;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.controller.netconf.util.handler.ssh.authentication.LoginPassword;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Ignore
public class SSHNetconfClientLiveTest {
    private static final Logger logger = LoggerFactory.getLogger(SSHNetconfClientLiveTest.class);

    NioEventLoopGroup nettyThreadgroup;
    NetconfSshClientDispatcher netconfClientDispatcher;
    InetSocketAddress address;
    final int connectionAttempts = 10, attemptMsTimeout = 1000;
    final int connectionTimeoutMillis = 20000;

    @Before
    public void setUp() {
        nettyThreadgroup = new NioEventLoopGroup();

        netconfClientDispatcher = new NetconfSshClientDispatcher(new LoginPassword(
                System.getProperty("username"), System.getProperty("password")),
                nettyThreadgroup, nettyThreadgroup, connectionTimeoutMillis);

        address = new InetSocketAddress(System.getProperty("host"), Integer.parseInt(System.getProperty("port")));
    }

    @Ignore
    @Test
    public void test() throws Exception {
        //runnable.run();
    }

    @Test
    public void testInExecutor() throws Exception {
        int threads = 4;
        ExecutorService executorService = Executors.newFixedThreadPool(threads);
        try {
            for (int i= 0;i< threads;i++) {
                InetSocketAddress address = new InetSocketAddress(System.getProperty("host"),
                        Integer.parseInt(System.getProperty("port")));
                NetconfRunnable runnable = new NetconfRunnable(address);
                executorService.execute(runnable);
            }
            executorService.shutdown();
            executorService.awaitTermination(1, TimeUnit.MINUTES);


        } finally {
            executorService.shutdownNow();
        }
    }

    class NetconfRunnable implements Runnable {
        private final InetSocketAddress address;

        NetconfRunnable(InetSocketAddress address) {
            this.address = address;
        }

        @Override
        public void run() {
            try (NetconfClient netconfClient = new NetconfClient(address.toString(), address, connectionAttempts,
                    attemptMsTimeout, netconfClientDispatcher);) {
                logger.info("OK {}", address);
            } catch (InterruptedException | IOException e) {
                logger.error("Failed {}", address, e);
            }
        }
    };
}
