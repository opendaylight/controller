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
import java.net.InetSocketAddress;
import java.nio.file.Files;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.keyprovider.PEMGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.netconf.ssh.SshProxyServer;
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
    private static final Logger logger = LoggerFactory.getLogger(SSHServerTest.class);

    private SshProxyServer server;

    @Mock
    private BundleContext mockedContext;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doReturn(null).when(mockedContext).createFilter(anyString());
        doNothing().when(mockedContext).addServiceListener(any(ServiceListener.class), anyString());
        doReturn(new ServiceReference[0]).when(mockedContext).getServiceReferences(anyString(), anyString());

        logger.info("Creating SSH server");

        final InetSocketAddress addr = InetSocketAddress.createUnresolved(HOST, PORT);
        server = new SshProxyServer(addr, NetconfConfigUtil.getNetconfLocalAddress(),
                new PasswordAuthenticator() {
                    @Override
                    public boolean authenticate(final String username, final String password, final ServerSession session) {
                        return true;
                    }
                },  new PEMGeneratorHostKeyProvider(Files.createTempFile("prefix", "suffix").toAbsolutePath().toString()));
        logger.info("SSH server on " + PORT);
    }

    @Test
    public void connect() {
        try {
            final Connection conn = new Connection(HOST, PORT);
            org.junit.Assert.assertNotNull(conn);
            logger.info("connecting to SSH server");
            conn.connect();
            logger.info("authenticating ...");
            final boolean isAuthenticated = conn.authenticateWithPassword(USER, PASSWORD);
            org.junit.Assert.assertTrue(isAuthenticated);
        } catch (final Exception e) {
            logger.error("Error while starting SSH server.", e);
        } finally {
            server.close();
        }

    }

}
