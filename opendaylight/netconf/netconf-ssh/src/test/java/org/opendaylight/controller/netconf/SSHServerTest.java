/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf;

import ch.ethz.ssh2.Connection;
import java.io.InputStream;
import java.net.InetSocketAddress;
import junit.framework.Assert;
import org.junit.Test;
import org.opendaylight.controller.netconf.ssh.NetconfSSHServer;
import org.opendaylight.controller.netconf.ssh.authentication.AuthProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SSHServerTest {

    private static final String USER = "netconf";
    private static final String PASSWORD  = "netconf";
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 1830;
    private static final InetSocketAddress tcpAddress = new InetSocketAddress("127.0.0.1", 8383);
    private static final Logger logger =  LoggerFactory.getLogger(SSHServerTest.class);
    private Thread sshServerThread;




    public void startSSHServer() throws Exception{
        logger.info("Creating SSH server");
        StubUserManager um = new StubUserManager(USER,PASSWORD);
        InputStream is = getClass().getResourceAsStream("/RSA.pk");
        AuthProvider ap = new AuthProvider(um, is);
        NetconfSSHServer server = NetconfSSHServer.start(PORT,tcpAddress,ap);
        sshServerThread = new Thread(server);
        sshServerThread.setDaemon(true);
        sshServerThread.start();
        logger.info("SSH server on");
    }

    @Test
    public void connect(){
        try {
            this.startSSHServer();
            Connection conn = new Connection(HOST,PORT);
            Assert.assertNotNull(conn);
            logger.info("connecting to SSH server");
            conn.connect();
            logger.info("authenticating ...");
            boolean isAuthenticated = conn.authenticateWithPassword(USER,PASSWORD);
            Assert.assertTrue(isAuthenticated);
        } catch (Exception e) {
            logger.error("Error while starting SSH server.", e);
        }

    }

}
