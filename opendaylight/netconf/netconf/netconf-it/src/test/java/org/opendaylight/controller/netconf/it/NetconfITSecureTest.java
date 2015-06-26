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
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.keyprovider.PEMGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.auth.AuthProvider;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcher;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcherImpl;
import org.opendaylight.controller.netconf.client.NetconfClientSessionListener;
import org.opendaylight.controller.netconf.client.SimpleNetconfClientSessionListener;
import org.opendaylight.controller.netconf.client.TestingNetconfClient;
import org.opendaylight.controller.netconf.client.conf.NetconfClientConfiguration;
import org.opendaylight.controller.netconf.client.conf.NetconfClientConfigurationBuilder;
import org.opendaylight.controller.netconf.nettyutil.handler.ssh.authentication.AuthenticationHandler;
import org.opendaylight.controller.netconf.nettyutil.handler.ssh.authentication.LoginPassword;
import org.opendaylight.controller.netconf.ssh.SshProxyServer;
import org.opendaylight.controller.netconf.ssh.SshProxyServerConfigurationBuilder;
import org.opendaylight.controller.netconf.util.messages.NetconfMessageUtil;
import org.opendaylight.controller.netconf.util.osgi.NetconfConfigUtil;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.controller.sal.connect.api.RemoteDevice;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfDeviceCommunicator;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfSessionPreferences;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.protocol.framework.NeverReconnectStrategy;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.xml.sax.SAXException;

public class NetconfITSecureTest extends AbstractNetconfConfigTest {

    public static final int PORT = 12024;
    private static final InetSocketAddress TLS_ADDRESS = new InetSocketAddress("127.0.0.1", PORT);

    public static final String USERNAME = "user";
    public static final String PASSWORD = "pwd";

    private SshProxyServer sshProxyServer;

    private ExecutorService nioExec;
    private EventLoopGroup clientGroup;
    private ScheduledExecutorService minaTimerEx;

    @Before
    public void setUp() throws Exception {
        nioExec = Executors.newFixedThreadPool(1);
        clientGroup = new NioEventLoopGroup();
        minaTimerEx = Executors.newScheduledThreadPool(1);
        sshProxyServer = new SshProxyServer(minaTimerEx, clientGroup, nioExec);
        sshProxyServer.bind(
                new SshProxyServerConfigurationBuilder()
                        .setBindingAddress(TLS_ADDRESS)
                        .setLocalAddress(NetconfConfigUtil.getNetconfLocalAddress())
                        .setAuthenticator(new PasswordAuthenticator() {
                            @Override
                            public boolean authenticate(final String username, final String password, final ServerSession session) {
                                return true;
                            }
                        }
                    )
                        .setKeyPairProvider(new PEMGeneratorHostKeyProvider(Files.createTempFile("prefix", "suffix").toAbsolutePath().toString()))
                        .setIdleTimeout(Integer.MAX_VALUE)
                        .createSshProxyServerConfiguration());
    }

    @After
    public void tearDown() throws Exception {
        sshProxyServer.close();
        clientGroup.shutdownGracefully();
        minaTimerEx.shutdownNow();
        nioExec.shutdownNow();
    }

    @Test(timeout = 2*60*1000)
    public void testSecure() throws Exception {
        final NetconfClientDispatcher dispatch = new NetconfClientDispatcherImpl(getNettyThreadgroup(), getNettyThreadgroup(), getHashedWheelTimer());
        try (TestingNetconfClient netconfClient = new TestingNetconfClient("testing-ssh-client", dispatch, getClientConfiguration(new SimpleNetconfClientSessionListener(), TLS_ADDRESS))) {
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
    @Test(timeout = 6*60*1000)
    public void testSecureStress() throws Exception {
        final int requests = 4000;

        final NetconfClientDispatcher dispatch = new NetconfClientDispatcherImpl(getNettyThreadgroup(), getNettyThreadgroup(), getHashedWheelTimer());
        final NetconfDeviceCommunicator sessionListener = getSessionListener();
        try (TestingNetconfClient netconfClient = new TestingNetconfClient("testing-ssh-client", dispatch, getClientConfiguration(sessionListener, TLS_ADDRESS))) {

            final AtomicInteger responseCounter = new AtomicInteger(0);
            final List<ListenableFuture<RpcResult<NetconfMessage>>> futures = Lists.newArrayList();

            for (int i = 0; i < requests; i++) {
                NetconfMessage getConfig = getGetConfig();
                getConfig = changeMessageId(getConfig, i);
                final ListenableFuture<RpcResult<NetconfMessage>> netconfMessageFuture = sessionListener.sendRequest(getConfig, QName.create("namespace", "2012-12-12", "get"));
                futures.add(netconfMessageFuture);
                Futures.addCallback(netconfMessageFuture, new FutureCallback<RpcResult<NetconfMessage>>() {
                    @Override
                    public void onSuccess(final RpcResult<NetconfMessage> result) {
                        responseCounter.incrementAndGet();
                    }

                    @Override
                    public void onFailure(final Throwable t) {
                        throw new RuntimeException(t);
                    }
                });
            }

            // Wait for every future
            for (final ListenableFuture<RpcResult<NetconfMessage>> future : futures) {
                try {
                    future.get(3, TimeUnit.MINUTES);
                } catch (final TimeoutException e) {
                    fail("Request " + futures.indexOf(future) + " is not responding");
                }
            }

            // Give future listeners some time to finish counter incrementation
            Thread.sleep(5000);

            assertEquals(requests, responseCounter.get());
        }
    }

    public static NetconfMessage changeMessageId(final NetconfMessage getConfig, final int i) throws IOException, SAXException {
        String s = XmlUtil.toString(getConfig.getDocument(), false);
        s = s.replace("101", Integer.toString(i));
        return new NetconfMessage(XmlUtil.readXmlToDocument(s));
    }

    static NetconfClientConfiguration getClientConfiguration(final NetconfClientSessionListener sessionListener,final InetSocketAddress tlsAddress) throws IOException {
        final NetconfClientConfigurationBuilder b = NetconfClientConfigurationBuilder.create();
        b.withAddress(tlsAddress);
        // Using session listener from sal-netconf-connector since stress test cannot be performed with simple listener
        b.withSessionListener(sessionListener);
        b.withReconnectStrategy(new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE, 5000));
        b.withProtocol(NetconfClientConfiguration.NetconfClientProtocol.SSH);
        b.withConnectionTimeoutMillis(5000);
        b.withAuthHandler(getAuthHandler());
        return b.build();
    }

    static NetconfDeviceCommunicator getSessionListener() {
        RemoteDevice<NetconfSessionPreferences, NetconfMessage, NetconfDeviceCommunicator> mockedRemoteDevice = mock(RemoteDevice.class);
        doNothing().when(mockedRemoteDevice).onRemoteSessionUp(any(NetconfSessionPreferences.class), any(NetconfDeviceCommunicator.class));
        doNothing().when(mockedRemoteDevice).onRemoteSessionDown();
        return new NetconfDeviceCommunicator(new RemoteDeviceId("secure-test"), mockedRemoteDevice);
    }

    public AuthProvider getAuthProvider() throws Exception {
        final AuthProvider mockAuth = mock(AuthProvider.class);
        doReturn("mockedAuth").when(mockAuth).toString();
        doReturn(true).when(mockAuth).authenticated(anyString(), anyString());
        return mockAuth;
    }

    public static AuthenticationHandler getAuthHandler() throws IOException {
        return new LoginPassword(USERNAME, PASSWORD);
    }

    @Override
    protected LocalAddress getTcpServerAddress() {
        return NetconfConfigUtil.getNetconfLocalAddress();
    }
}
