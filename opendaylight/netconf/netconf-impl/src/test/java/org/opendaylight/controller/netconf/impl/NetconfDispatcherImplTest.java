/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl;

import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.netconf.impl.osgi.NetconfOperationServiceFactoryListenerImpl;

import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;

public class NetconfDispatcherImplTest {

    private EventLoopGroup nettyGroup;
    private NetconfServerDispatcher dispatch;
    private DefaultCommitNotificationProducer commitNot;
    private HashedWheelTimer hashedWheelTimer;


    @Before
    public void setUp() throws Exception {
        nettyGroup = new NioEventLoopGroup();

        commitNot = new DefaultCommitNotificationProducer(
                ManagementFactory.getPlatformMBeanServer());
        NetconfOperationServiceFactoryListenerImpl factoriesListener = new NetconfOperationServiceFactoryListenerImpl();

        SessionIdProvider idProvider = new SessionIdProvider();
        hashedWheelTimer = new HashedWheelTimer();
        NetconfServerSessionNegotiatorFactory serverNegotiatorFactory = new NetconfServerSessionNegotiatorFactory(
                hashedWheelTimer, factoriesListener, idProvider, 5000, commitNot, ConcurrentClientsTest.createMockedMonitoringService());

        NetconfServerDispatcher.ServerChannelInitializer serverChannelInitializer = new NetconfServerDispatcher.ServerChannelInitializer(serverNegotiatorFactory);

        dispatch = new NetconfServerDispatcher(
                serverChannelInitializer, nettyGroup, nettyGroup);
    }

    @After
    public void tearDown() throws Exception {
        hashedWheelTimer.stop();
        commitNot.close();
        nettyGroup.shutdownGracefully();
    }

    @Test
    public void test() throws Exception {
        InetSocketAddress addr = new InetSocketAddress("127.0.0.1", 8333);
        ChannelFuture s = dispatch.createServer(addr);
        s.get();
    }
}
