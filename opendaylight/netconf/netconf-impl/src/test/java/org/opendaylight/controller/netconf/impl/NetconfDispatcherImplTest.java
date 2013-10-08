/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl;

import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;

import javax.net.ssl.SSLContext;

import org.junit.Test;
import org.opendaylight.controller.netconf.impl.*;
import org.opendaylight.controller.netconf.impl.osgi.NetconfOperationServiceFactoryListener;
import org.opendaylight.controller.netconf.impl.osgi.NetconfOperationServiceFactoryListenerImpl;

import com.google.common.base.Optional;

import io.netty.channel.ChannelFuture;
import io.netty.util.HashedWheelTimer;

public class NetconfDispatcherImplTest {

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
        NetconfServerDispatcher dispatch = new NetconfServerDispatcher(Optional.<SSLContext> absent(),
                serverNegotiatorFactory, listenerFactory);

        InetSocketAddress addr = new InetSocketAddress("127.0.0.1", 8333);
        ChannelFuture s = dispatch.createServer(addr);

        commitNot.close();
        dispatch.close();
    }
}
