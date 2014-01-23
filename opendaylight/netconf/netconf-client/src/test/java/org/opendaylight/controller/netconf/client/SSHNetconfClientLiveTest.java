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
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.util.NetconfUtil;
import org.opendaylight.controller.netconf.util.handler.ssh.authentication.LoginPassword;

import java.net.InetSocketAddress;

@Ignore
public class SSHNetconfClientLiveTest {

    NioEventLoopGroup nettyThreadgroup;
    NetconfSshClientDispatcher netconfClientDispatcher;

    @Before
    public void setUp() {
        nettyThreadgroup = new NioEventLoopGroup();
        netconfClientDispatcher = new NetconfSshClientDispatcher(new LoginPassword(
                System.getProperty("username"), System.getProperty("password")),
                nettyThreadgroup, nettyThreadgroup, 5000);
    }

    @Test
    public void test() throws Exception {
        InetSocketAddress address = new InetSocketAddress(System.getProperty("host"), 830);
        int connectionAttempts = 10, attemptMsTimeout = 1000;

        NetconfClient netconfClient = new NetconfClient("client", address, connectionAttempts,
            attemptMsTimeout, netconfClientDispatcher);

        netconfClient.getCapabilities();

        NetconfMessage netconfMessage = NetconfUtil.createMessage(getClass().getResourceAsStream("/get_schema.xml"));
        NetconfMessage response = netconfClient.sendMessage(netconfMessage);
        NetconfUtil.checkIsMessageOk(response);
    }
}
