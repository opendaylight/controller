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

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.sshd.ClientSession;
import org.apache.sshd.SshClient;
import org.apache.sshd.client.future.AuthFuture;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.keyprovider.PEMGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.netconf.ssh.SshProxyServer;
import org.opendaylight.controller.netconf.ssh.SshProxyServerConfigurationBuilder;
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
    private static final Logger LOG = LoggerFactory.getLogger(SSHServerTest.class);

    private SshProxyServer server;

    @Mock
    private BundleContext mockedContext;
    private final ExecutorService nioExec = Executors.newFixedThreadPool(1);
    private final EventLoopGroup clientGroup = new NioEventLoopGroup();
    private final ScheduledExecutorService minaTimerEx = Executors.newScheduledThreadPool(1);

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doReturn(null).when(mockedContext).createFilter(anyString());
        doNothing().when(mockedContext).addServiceListener(any(ServiceListener.class), anyString());
        doReturn(new ServiceReference[0]).when(mockedContext).getServiceReferences(anyString(), anyString());

        LOG.info("Creating SSH server");

        final InetSocketAddress addr = InetSocketAddress.createUnresolved(HOST, PORT);
        server = new SshProxyServer(minaTimerEx, clientGroup, nioExec);
        server.bind(
                new SshProxyServerConfigurationBuilder().setBindingAddress(addr).setLocalAddress(NetconfConfigUtil.getNetconfLocalAddress()).setAuthenticator(new PasswordAuthenticator() {
                    @Override
                    public boolean authenticate(final String username, final String password, final ServerSession session) {
                        return true;
                    }
                }).setKeyPairProvider(new PEMGeneratorHostKeyProvider(Files.createTempFile("prefix", "suffix").toAbsolutePath().toString())).setIdleTimeout(Integer.MAX_VALUE).createSshProxyServerConfiguration());
        LOG.info("SSH server started on {}", PORT);
    }

    @Test
    public void connect() throws Exception {
        final SshClient sshClient = SshClient.setUpDefaultClient();
        sshClient.start();
        try {
            final ConnectFuture connect = sshClient.connect(USER, HOST, PORT);
            connect.await(30, TimeUnit.SECONDS);
            org.junit.Assert.assertTrue(connect.isConnected());
            final ClientSession session = connect.getSession();
            session.addPasswordIdentity(PASSWORD);
            final AuthFuture auth = session.auth();
            auth.await(30, TimeUnit.SECONDS);
            org.junit.Assert.assertTrue(auth.isSuccess());
        } finally {
            sshClient.close(true);
            server.close();
            clientGroup.shutdownGracefully().await();
            minaTimerEx.shutdownNow();
            nioExec.shutdownNow();
        }
    }

}
