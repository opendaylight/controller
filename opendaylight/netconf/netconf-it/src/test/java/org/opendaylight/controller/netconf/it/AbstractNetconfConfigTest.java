/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.it;

import java.net.InetSocketAddress;

import org.junit.After;
import org.junit.Before;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.netconf.client.SimpleNetconfClientSessionListener;
import org.opendaylight.controller.netconf.client.conf.NetconfClientConfiguration;
import org.opendaylight.controller.netconf.client.conf.NetconfClientConfigurationBuilder;
import org.opendaylight.controller.netconf.impl.DefaultCommitNotificationProducer;
import org.opendaylight.controller.netconf.impl.NetconfServerDispatcher;
import org.opendaylight.controller.netconf.impl.NetconfServerSessionNegotiatorFactory;
import org.opendaylight.controller.netconf.impl.SessionIdProvider;
import org.opendaylight.controller.netconf.impl.osgi.NetconfOperationServiceFactoryListenerImpl;
import org.opendaylight.controller.netconf.impl.osgi.SessionMonitoringService;
import org.opendaylight.protocol.framework.NeverReconnectStrategy;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.GlobalEventExecutor;

public class AbstractNetconfConfigTest extends AbstractConfigTest {

    private EventLoopGroup nettyThreadgroup;
    private HashedWheelTimer hashedWheelTimer;

    @Before
    public void setUpAbstractNetconfConfigTest() {
        nettyThreadgroup = new NioEventLoopGroup();
        hashedWheelTimer = new HashedWheelTimer();
    }

    protected NetconfServerDispatcher createDispatcher(
            NetconfOperationServiceFactoryListenerImpl factoriesListener, SessionMonitoringService sessionMonitoringService,
            DefaultCommitNotificationProducer commitNotifier) {
        SessionIdProvider idProvider = new SessionIdProvider();

        NetconfServerSessionNegotiatorFactory serverNegotiatorFactory = new NetconfServerSessionNegotiatorFactory(
                hashedWheelTimer, factoriesListener, idProvider, 5000, commitNotifier, sessionMonitoringService);

        NetconfServerDispatcher.ServerChannelInitializer serverChannelInitializer = new NetconfServerDispatcher.ServerChannelInitializer(
                serverNegotiatorFactory);
        return new NetconfServerDispatcher(serverChannelInitializer, nettyThreadgroup, nettyThreadgroup);
    }

    protected HashedWheelTimer getHashedWheelTimer() {
        return hashedWheelTimer;
    }

    protected EventLoopGroup getNettyThreadgroup() {
        return nettyThreadgroup;
    }

    @After
    public void cleanUpTimer() {
        hashedWheelTimer.stop();
        nettyThreadgroup.shutdownGracefully();
    }

    public NetconfClientConfiguration getClientConfiguration(final InetSocketAddress tcpAddress, final int timeout) {
        final NetconfClientConfigurationBuilder b = NetconfClientConfigurationBuilder.create();
        b.withAddress(tcpAddress);
        b.withSessionListener(new SimpleNetconfClientSessionListener());
        b.withReconnectStrategy(new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE,
                timeout));
        b.withConnectionTimeoutMillis(timeout);
        return b.build();
    }
}
