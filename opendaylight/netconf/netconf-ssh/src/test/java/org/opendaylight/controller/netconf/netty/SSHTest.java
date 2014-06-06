/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.netty;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.netty.channel.nio.NioEventLoopGroup;
import org.junit.Test;
import org.opendaylight.controller.netconf.ssh.NetconfSSHServer;
import org.opendaylight.controller.netconf.ssh.authentication.AuthProvider;
import org.opendaylight.controller.netconf.ssh.authentication.PEMGenerator;
import org.opendaylight.controller.netconf.util.osgi.NetconfConfigUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SSHTest {
    public static final Logger logger = LoggerFactory.getLogger(SSHTest.class);

    @Test
    public void test() throws Exception {
        new Thread(new EchoServer(), "EchoServer").start();
        AuthProvider authProvider = mock(AuthProvider.class);
        doReturn(PEMGenerator.generate().toCharArray()).when(authProvider).getPEMAsCharArray();
        doReturn(true).when(authProvider).authenticated(anyString(), anyString());
        NetconfSSHServer thread = NetconfSSHServer.start(1831, NetconfConfigUtil.getNetconfLocalAddress(), authProvider, new NioEventLoopGroup());
        Thread.sleep(2000);
        logger.info("Closing socket");
        thread.close();
        thread.join();
    }
}
