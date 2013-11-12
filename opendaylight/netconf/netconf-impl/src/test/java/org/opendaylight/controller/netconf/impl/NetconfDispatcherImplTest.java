/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl;

import com.google.common.base.Optional;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.netconf.impl.osgi.NetconfOperationServiceFactoryListener;
import org.opendaylight.controller.netconf.impl.osgi.NetconfOperationServiceFactoryListenerImpl;

import javax.net.ssl.SSLContext;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;

public class NetconfDispatcherImplTest {

    private EventLoopGroup nettyGroup;

    @Before
    public void setUp() throws Exception {
        nettyGroup = new NioEventLoopGroup();
    }

    @After
    public void tearDown() throws Exception {
        nettyGroup.shutdownGracefully();
    }

    @Test
    public void test() throws Exception {

        DefaultCommitNotificationProducer commitNot = new DefaultCommitNotificationProducer(
                ManagementFactory.getPlatformMBeanServer());
        NetconfOperationServiceFactoryListener factoriesListener = new NetconfOperationServiceFactoryListenerImpl();

        SessionIdProvider idProvider = new SessionIdProvider();
        NetconfServerSessionNegotiatorFactory serverNegotiatorFactory = new NetconfServerSessionNegotiatorFactory(
                new HashedWheelTimer(), factoriesListener, idProvider);

        NetconfServerSessionListenerFactory listenerFactory = new NetconfServerSessionListenerFactory(
                factoriesListener, commitNot, idProvider);
        NetconfServerDispatcher.ServerSslChannelInitializer serverChannelInitializer = new NetconfServerDispatcher.ServerSslChannelInitializer(Optional.<SSLContext>absent(), serverNegotiatorFactory, listenerFactory);


        NetconfServerDispatcher dispatch = new NetconfServerDispatcher(
                serverChannelInitializer, nettyGroup, nettyGroup);

        InetSocketAddress addr = new InetSocketAddress("127.0.0.1", 8333);
        ChannelFuture s = dispatch.createServer(addr);

        commitNot.close();
    }
}
