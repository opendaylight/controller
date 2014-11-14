/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.client;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import io.netty.util.concurrent.GenericFutureListener;
import java.net.InetSocketAddress;
import java.util.concurrent.Future;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.netconf.client.conf.NetconfClientConfiguration;
import org.opendaylight.controller.netconf.client.conf.NetconfReconnectingClientConfiguration;
import org.opendaylight.controller.netconf.client.conf.NetconfReconnectingClientConfigurationBuilder;
import org.opendaylight.controller.netconf.nettyutil.handler.ssh.authentication.AuthenticationHandler;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessageAdditionalHeader;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;

public class NetconfClientDispatcherImplTest {
    @Test
    public void testNetconfClientDispatcherImpl() throws Exception {
        EventLoopGroup bossGroup = Mockito.mock(EventLoopGroup.class);
        EventLoopGroup workerGroup = Mockito.mock(EventLoopGroup.class);
        Timer timer = new HashedWheelTimer();

        ChannelFuture chf = Mockito.mock(ChannelFuture.class);
        Channel ch = Mockito.mock(Channel.class);
        doReturn(ch).when(chf).channel();
        Throwable thr = Mockito.mock(Throwable.class);
        doReturn(chf).when(workerGroup).register(any(Channel.class));

        ChannelPromise promise = Mockito.mock(ChannelPromise.class);
        doReturn(promise).when(chf).addListener(any(GenericFutureListener.class));
        doReturn(thr).when(chf).cause();

        Long timeout = 200L;
        NetconfHelloMessageAdditionalHeader header = new NetconfHelloMessageAdditionalHeader("a", "host", "port", "trans", "id");
        NetconfClientSessionListener listener = new SimpleNetconfClientSessionListener();
        InetSocketAddress address = InetSocketAddress.createUnresolved("host", 830);
        ReconnectStrategyFactory reconnectStrategyFactory = Mockito.mock(ReconnectStrategyFactory.class);
        AuthenticationHandler handler = Mockito.mock(AuthenticationHandler.class);
        ReconnectStrategy reconnect = Mockito.mock(ReconnectStrategy.class);

        doReturn(5).when(reconnect).getConnectTimeout();
        doReturn("").when(reconnect).toString();
        doReturn("").when(handler).toString();
        doReturn("").when(reconnectStrategyFactory).toString();
        doReturn(reconnect).when(reconnectStrategyFactory).createReconnectStrategy();

        NetconfReconnectingClientConfiguration cfg = NetconfReconnectingClientConfigurationBuilder.create().
                withProtocol(NetconfClientConfiguration.NetconfClientProtocol.SSH).
                withAddress(address).
                withConnectionTimeoutMillis(timeout).
                withReconnectStrategy(reconnect).
                withAdditionalHeader(header).
                withSessionListener(listener).
                withConnectStrategyFactory(reconnectStrategyFactory).
                withAuthHandler(handler).build();

        NetconfReconnectingClientConfiguration cfg2 = NetconfReconnectingClientConfigurationBuilder.create().
                withProtocol(NetconfClientConfiguration.NetconfClientProtocol.TCP).
                withAddress(address).
                withConnectionTimeoutMillis(timeout).
                withReconnectStrategy(reconnect).
                withAdditionalHeader(header).
                withSessionListener(listener).
                withConnectStrategyFactory(reconnectStrategyFactory).
                withAuthHandler(handler).build();

        NetconfClientDispatcherImpl dispatcher = new NetconfClientDispatcherImpl(bossGroup, workerGroup, timer);
        Future<NetconfClientSession> sshSession = dispatcher.createClient(cfg);
        Future<NetconfClientSession> tcpSession = dispatcher.createClient(cfg2);

        Future<Void> sshReconn = dispatcher.createReconnectingClient(cfg);
        Future<Void> tcpReconn = dispatcher.createReconnectingClient(cfg2);

        assertNotNull(sshSession);
        assertNotNull(tcpSession);
        assertNotNull(sshReconn);
        assertNotNull(tcpReconn);

    }
}
