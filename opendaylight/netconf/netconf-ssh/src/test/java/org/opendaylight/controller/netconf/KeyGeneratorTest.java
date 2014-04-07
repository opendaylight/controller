/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.netconf.ssh.NetconfSSHServer;
import org.opendaylight.controller.netconf.ssh.authentication.AuthProvider;
import org.opendaylight.controller.netconf.ssh.authentication.PEMGenerator;
import org.opendaylight.controller.usermanager.IUserManager;
import org.opendaylight.controller.usermanager.UserConfig;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

// This test is intended to be verified using ssh
@Ignore
public class KeyGeneratorTest {

    @Mock
    private IUserManager iUserManager;
    File tempFile;

    @Before
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);
        doReturn(null).when(iUserManager).addLocalUser(any(UserConfig.class));
        tempFile = File.createTempFile("odltest", ".tmp");
        tempFile.deleteOnExit();
    }

    @After
    public void tearDown() {
        assertTrue(tempFile.delete());
    }

    @Test
    public void test() throws Exception {
        String pem = PEMGenerator.generateTo(tempFile);

        AuthProvider authProvider = new AuthProvider(iUserManager, pem);
        InetSocketAddress inetSocketAddress = new InetSocketAddress(Inet4Address.getLoopbackAddress().getHostAddress(), 8383);
        NetconfSSHServer server = NetconfSSHServer.start(1830, inetSocketAddress, authProvider);

        Thread serverThread = new  Thread(server,"netconf SSH server thread");
        serverThread.start();
        serverThread.join();
    }
}
