/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.it;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import org.junit.After;
import org.junit.Before;
import org.opendaylight.controller.config.manager.impl.AbstractConfigTest;
import org.opendaylight.controller.netconf.impl.DefaultCommitNotificationProducer;
import org.opendaylight.controller.netconf.impl.NetconfServerDispatcher;
import org.opendaylight.controller.netconf.impl.NetconfServerSessionListenerFactory;
import org.opendaylight.controller.netconf.impl.NetconfServerSessionNegotiatorFactory;
import org.opendaylight.controller.netconf.impl.SessionIdProvider;
import org.opendaylight.controller.netconf.impl.osgi.NetconfOperationServiceFactoryListenerImpl;
import org.opendaylight.controller.netconf.impl.osgi.SessionMonitoringService;

public class AbstractNetconfConfigTest extends AbstractConfigTest {

    protected EventLoopGroup nettyThreadgroup;
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
                hashedWheelTimer, factoriesListener, idProvider, 5000);

        NetconfServerSessionListenerFactory listenerFactory = new NetconfServerSessionListenerFactory(
                factoriesListener, commitNotifier, idProvider,
                sessionMonitoringService);

        NetconfServerDispatcher.ServerChannelInitializer serverChannelInitializer = new NetconfServerDispatcher.ServerChannelInitializer(
                serverNegotiatorFactory, listenerFactory);
        return new NetconfServerDispatcher(serverChannelInitializer, nettyThreadgroup, nettyThreadgroup);
    }


    @After
    public void cleanUpTimer() {
        hashedWheelTimer.stop();
        nettyThreadgroup.shutdownGracefully();
    }

}
