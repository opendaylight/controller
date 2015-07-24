/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.client;

import com.google.common.base.Optional;
import java.net.InetSocketAddress;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.netconf.client.conf.NetconfClientConfiguration;
import org.opendaylight.controller.netconf.client.conf.NetconfReconnectingClientConfiguration;
import org.opendaylight.controller.netconf.client.conf.NetconfReconnectingClientConfigurationBuilder;
import org.opendaylight.controller.netconf.nettyutil.handler.ssh.authentication.AuthenticationHandler;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessageAdditionalHeader;
import org.opendaylight.protocol.framework.ReconnectStrategy;
import org.opendaylight.protocol.framework.ReconnectStrategyFactory;

public class NetconfReconnectingClientConfigurationTest {
    @Test
    public void testNetconfReconnectingClientConfiguration() throws Exception {
        Long timeout = 200L;
        NetconfHelloMessageAdditionalHeader header = new NetconfHelloMessageAdditionalHeader("a", "host", "port", "trans", "id");
        NetconfClientSessionListener listener = new SimpleNetconfClientSessionListener();
        InetSocketAddress address = InetSocketAddress.createUnresolved("host", 830);
        ReconnectStrategyFactory strategy = Mockito.mock(ReconnectStrategyFactory.class);
        AuthenticationHandler handler = Mockito.mock(AuthenticationHandler.class);
        ReconnectStrategy reconnect = Mockito.mock(ReconnectStrategy.class);

        NetconfReconnectingClientConfiguration cfg = NetconfReconnectingClientConfigurationBuilder.create().
                withProtocol(NetconfClientConfiguration.NetconfClientProtocol.SSH).
                withAddress(address).
                withConnectionTimeoutMillis(timeout).
                withReconnectStrategy(reconnect).
                withAdditionalHeader(header).
                withSessionListener(listener).
                withConnectStrategyFactory(strategy).
                withAuthHandler(handler).build();

        Assert.assertEquals(timeout, cfg.getConnectionTimeoutMillis());
        Assert.assertEquals(Optional.fromNullable(header), cfg.getAdditionalHeader());
        Assert.assertEquals(listener, cfg.getSessionListener());
        Assert.assertEquals(handler, cfg.getAuthHandler());
        Assert.assertEquals(strategy, cfg.getConnectStrategyFactory());
        Assert.assertEquals(NetconfClientConfiguration.NetconfClientProtocol.SSH, cfg.getProtocol());
        Assert.assertEquals(address, cfg.getAddress());
        Assert.assertEquals(reconnect, cfg.getReconnectStrategy());
    }
}
