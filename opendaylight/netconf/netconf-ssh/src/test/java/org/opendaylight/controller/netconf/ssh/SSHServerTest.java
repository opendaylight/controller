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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendaylight.controller.netconf.ssh.authentication.AuthProvider;
import org.opendaylight.controller.sal.authorization.UserLevel;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.usermanager.AuthenticatedUser;
import org.opendaylight.controller.usermanager.AuthorizationConfig;
import org.opendaylight.controller.usermanager.IAAAProvider;
import org.opendaylight.controller.usermanager.IUserManager;
import org.opendaylight.controller.usermanager.ServerConfig;
import org.opendaylight.controller.usermanager.UserConfig;
import org.opendaylight.controller.usermanager.internal.UserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SSHServerTest {

    private static final String USER = "netconf";
    private static final String PASSWORD  = "netconf";
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 1830;
    private static final InetSocketAddress tcpAddress = new InetSocketAddress("127.0.0.1", 8383);
    private static final Logger logger =  LoggerFactory.getLogger(SSHServerTest.class);
    private static UserManager um;
    private Thread sshServerThread;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

        IUserManager userManager = (IUserManager) ServiceHelper
                .getGlobalInstance(IUserManager.class, new Object());
        if (userManager instanceof UserManager) {
            um = (UserManager) userManager;
        } else {
            um = new UserManager();
            um.setAuthProviders(new ConcurrentHashMap<String, IAAAProvider>());

            // mock up a remote server list with a dummy server
            um.setRemoteServerConfigList(new ConcurrentHashMap<String, ServerConfig>() {
                static final long serialVersionUID = 1L;
                {
                    put("dummyServerConfig", new ServerConfig() {
                        // Server config can't be empty
                        static final long serialVersionUID = 8645L;

                        @Override
                        public String getAddress() {
                            return "1.1.1.1";
                        }

                        @Override
                        public String getSecret() {
                            return "secret";
                        }

                        @Override
                        public String getProtocol() {
                            return "IPv4";
                        }
                    });
                }
            });

            // mock up a localUserConfigList with an admin user
            um.setLocalUserConfigList(new ConcurrentHashMap<String, UserConfig>() {
                static final long serialVersionUID = 2L;
                {
                    List<String> roles = new ArrayList<String>(1);
                    roles.add(UserLevel.SYSTEMADMIN.toString());
                    put(USER, new UserConfig(USER,PASSWORD,roles));
                }
            });

            um.setAuthorizationConfList(new ConcurrentHashMap<String, AuthorizationConfig>() {
                static final long serialVersionUID = 2L;
                {
                    List<String> roles = new ArrayList<String>(3);
                    roles.add(UserLevel.NETWORKOPERATOR.toString());
                    roles.add("Container1-Admin");
                    roles.add("Application2-User");

                    put(USER, new AuthorizationConfig(PASSWORD, roles));
                }
            });
            // instantiate an empty activeUser collection
            um.setActiveUsers(new ConcurrentHashMap<String, AuthenticatedUser>());
        }
    }

    public void startSSHServer() throws Exception{
        logger.info("Creating SSH server");
        AuthProvider ap = new AuthProvider(um);
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
        } catch (Exception e) {
            e.printStackTrace();
        }

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


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
