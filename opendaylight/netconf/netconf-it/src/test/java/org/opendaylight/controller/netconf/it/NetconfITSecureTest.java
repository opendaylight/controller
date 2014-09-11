/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.collect.Lists;
import io.netty.channel.local.LocalAddress;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.auth.AuthProvider;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcher;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcherImpl;
import org.opendaylight.controller.netconf.client.SimpleNetconfClientSessionListener;
import org.opendaylight.controller.netconf.client.conf.NetconfClientConfiguration;
import org.opendaylight.controller.netconf.client.conf.NetconfClientConfigurationBuilder;
import org.opendaylight.controller.netconf.client.TestingNetconfClient;
import org.opendaylight.controller.netconf.nettyutil.handler.ssh.authentication.AuthenticationHandler;
import org.opendaylight.controller.netconf.nettyutil.handler.ssh.authentication.LoginPassword;
import org.opendaylight.controller.netconf.ssh.NetconfSSHServer;
import org.opendaylight.controller.netconf.ssh.authentication.PEMGenerator;
import org.opendaylight.controller.netconf.util.messages.NetconfMessageUtil;
import org.opendaylight.controller.netconf.util.osgi.NetconfConfigUtil;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.protocol.framework.NeverReconnectStrategy;

public class NetconfITSecureTest extends AbstractNetconfConfigTest {

    public static final int PORT = 12024;
    private static final InetSocketAddress TLS_ADDRESS = new InetSocketAddress("127.0.0.1", PORT);

    public static final String USERNAME = "user";
    public static final String PASSWORD = "pwd";

    private NetconfSSHServer sshServer;

    @Before
    public void setUp() throws Exception {
        final char[] pem = PEMGenerator.generate().toCharArray();
        sshServer = NetconfSSHServer.start(TLS_ADDRESS.getPort(), NetconfConfigUtil.getNetconfLocalAddress(), getNettyThreadgroup(), pem);
        sshServer.setAuthProvider(getAuthProvider());
    }

    @After
    public void tearDown() throws Exception {
        sshServer.close();
        sshServer.join();
    }

    @Test
    public void testSecure() throws Exception {
        final NetconfClientDispatcher dispatch = new NetconfClientDispatcherImpl(getNettyThreadgroup(), getNettyThreadgroup(), getHashedWheelTimer());
        try (TestingNetconfClient netconfClient = new TestingNetconfClient("testing-ssh-client", dispatch, getClientConfiguration())) {
            NetconfMessage response = netconfClient.sendMessage(getGetConfig());
            assertFalse("Unexpected error message " + XmlUtil.toString(response.getDocument()),
                    NetconfMessageUtil.isErrorMessage(response));

            final NetconfMessage gs = new NetconfMessage(XmlUtil.readXmlToDocument("<rpc message-id=\"2\"\n" +
                    "     xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n" +
                    "    <get-schema xmlns=\"urn:ietf:params:xml:ns:yang:ietf-netconf-monitoring\">\n" +
                    "        <identifier>config</identifier>\n" +
                    "    </get-schema>\n" +
                    "</rpc>\n"));

            response = netconfClient.sendMessage(gs);
            assertFalse("Unexpected error message " + XmlUtil.toString(response.getDocument()),
                    NetconfMessageUtil.isErrorMessage(response));
        }
    }

    /**
     * Test all requests are handled properly and no mismatch occurs in listener
     */
    @Test(timeout = 3*60*1000)
    public void testSecureStress() throws Exception {
        final NetconfClientDispatcher dispatch = new NetconfClientDispatcherImpl(getNettyThreadgroup(), getNettyThreadgroup(), getHashedWheelTimer());
        try (TestingNetconfClient netconfClient = new TestingNetconfClient("testing-ssh-client", dispatch, getClientConfiguration())) {

            final AtomicInteger responseCounter = new AtomicInteger(0);
            final List<Future<?>> futures = Lists.newArrayList();

            final int requests = 1000;
            for (int i = 0; i < requests; i++) {
                final Future<NetconfMessage> netconfMessageFuture = netconfClient.sendRequest(getGetConfig());
                futures.add(netconfMessageFuture);
                netconfMessageFuture.addListener(new GenericFutureListener<Future<? super NetconfMessage>>() {
                    @Override
                    public void operationComplete(final Future<? super NetconfMessage> future) throws Exception {
                        assertTrue("Request unsuccessful " + future.cause(), future.isSuccess());
                        responseCounter.incrementAndGet();
                    }
                });
            }

            for (final Future<?> future : futures) {
                future.await();
            }

            // Give future listeners some time to finish counter incrementation
            Thread.sleep(5000);

            assertEquals(requests, responseCounter.get());
        }
    }

    public NetconfClientConfiguration getClientConfiguration() throws IOException {
        final NetconfClientConfigurationBuilder b = NetconfClientConfigurationBuilder.create();
        b.withAddress(TLS_ADDRESS);
        b.withSessionListener(new SimpleNetconfClientSessionListener());
        b.withReconnectStrategy(new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE, 5000));
        b.withProtocol(NetconfClientConfiguration.NetconfClientProtocol.SSH);
        b.withConnectionTimeoutMillis(5000);
        b.withAuthHandler(getAuthHandler());
        return b.build();
    }

    public AuthProvider getAuthProvider() throws Exception {
        final AuthProvider mockAuth = mock(AuthProvider.class);
        doReturn("mockedAuth").when(mockAuth).toString();
        doReturn(true).when(mockAuth).authenticated(anyString(), anyString());
        return mockAuth;
    }

    public AuthenticationHandler getAuthHandler() throws IOException {
        return new LoginPassword(USERNAME, PASSWORD);
    }

    @Override
    protected LocalAddress getTcpServerAddress() {
        return NetconfConfigUtil.getNetconfLocalAddress();
    }
}
