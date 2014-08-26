/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.ssh.authentication;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

import ch.ethz.ssh2.Connection;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import java.io.InputStream;
import java.net.InetSocketAddress;
import junit.framework.Assert;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.netconf.auth.AuthProvider;
import org.opendaylight.controller.netconf.ssh.NetconfSSHServer;
import org.opendaylight.controller.netconf.util.osgi.NetconfConfigUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SSHServerTest {

    private static final String USER = "netconf";
    private static final String PASSWORD = "netconf";
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 1830;
    private static final InetSocketAddress tcpAddress = new InetSocketAddress("127.0.0.1", 8383);
    private static final Logger logger = LoggerFactory.getLogger(SSHServerTest.class);
    private Thread sshServerThread;

    @Mock
    private BundleContext mockedContext;


    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doReturn(null).when(mockedContext).createFilter(anyString());
        doNothing().when(mockedContext).addServiceListener(any(ServiceListener.class), anyString());
        doReturn(new ServiceReference[0]).when(mockedContext).getServiceReferences(anyString(), anyString());

        logger.info("Creating SSH server");
        String pem;
        try (InputStream is = getClass().getResourceAsStream("/RSA.pk")) {
            pem = IOUtils.toString(is);
        }


        EventLoopGroup bossGroup = new NioEventLoopGroup();
        NetconfSSHServer server = NetconfSSHServer.start(PORT, NetconfConfigUtil.getNetconfLocalAddress(),
                bossGroup, pem.toCharArray());
        server.setAuthProvider(new AuthProvider() {
            @Override
            public boolean authenticated(final String username, final String password) {
                return true;
            }
        });

        sshServerThread = new Thread(server);
        sshServerThread.setDaemon(true);
        sshServerThread.start();
        logger.info("SSH server on " + PORT);
    }

    @Test
    public void connect() {
        try {
            Connection conn = new Connection(HOST, PORT);
            Assert.assertNotNull(conn);
            logger.info("connecting to SSH server");
            conn.connect();
            logger.info("authenticating ...");
            boolean isAuthenticated = conn.authenticateWithPassword(USER, PASSWORD);
            Assert.assertTrue(isAuthenticated);
        } catch (Exception e) {
            logger.error("Error while starting SSH server.", e);
        }

    }

}
