/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.ssh;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import java.io.IOException;
import java.net.InetSocketAddress;
import junit.framework.Assert;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SSHServerTest {

    private static final String USER = "netconf";
    private static final String PASSWORD  = "netconf";
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 1830;
    private static final InetSocketAddress tcpAddress = new InetSocketAddress("127.0.0.1", 8383);
    private static final Logger logger =  LoggerFactory.getLogger(SSHServerTest.class);

//    @Before
    public void startSSHServer() throws Exception{
            logger.info("Creating SSH server");
            NetconfSSHServer server = NetconfSSHServer.start(PORT,tcpAddress);
            Thread sshServerThread = new Thread(server);
            sshServerThread.setDaemon(true);
            sshServerThread.start();
            logger.info("SSH server on");
    }

    @Test
    public void connect(){
        Connection conn = new Connection(HOST,PORT);
        Assert.assertNotNull(conn);
        try {
            logger.info("connecting to SSH server");
            conn.connect();
            logger.info("authenticating ...");
            boolean isAuthenticated = conn.authenticateWithPassword(USER,PASSWORD);
            Assert.assertTrue(isAuthenticated);
            logger.info("opening session");
            Session sess = conn.openSession();
            logger.info("subsystem netconf");
            sess.startSubSystem("netconf");
            sess.getStdin().write("<?xml version=\"1.0\" encoding=\"UTF-8\"?><hello xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\"><capabilities><capability>urn:ietf:params:netconf:base:1.1</capability></capabilities></hello>]]>]]>".getBytes());
            IOUtils.copy(sess.getStdout(), System.out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
