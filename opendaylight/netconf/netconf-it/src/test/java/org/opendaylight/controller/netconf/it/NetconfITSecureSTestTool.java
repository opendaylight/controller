/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.html
 */

package org.opendaylight.controller.netconf.it;

import static java.lang.Thread.sleep;
import static org.junit.Assert.assertEquals;
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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.auth.AuthProvider;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcher;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcherImpl;
import org.opendaylight.controller.netconf.client.NetconfClientSessionListener;
import org.opendaylight.controller.netconf.client.TestingNetconfClient;
import org.opendaylight.controller.netconf.client.conf.NetconfClientConfiguration;
import org.opendaylight.controller.netconf.client.conf.NetconfClientConfigurationBuilder;
import org.opendaylight.controller.netconf.nettyutil.handler.ssh.authentication.AuthenticationHandler;
import org.opendaylight.controller.netconf.nettyutil.handler.ssh.authentication.LoginPassword;
import org.opendaylight.controller.netconf.ssh.SshProxyServer;
import org.opendaylight.controller.netconf.test.tool.Main.Params;
import org.opendaylight.controller.netconf.test.tool.NetconfDeviceSimulator;
import org.opendaylight.controller.netconf.util.osgi.NetconfConfigUtil;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.controller.sal.connect.api.RemoteDevice;
import org.opendaylight.controller.sal.connect.api.RemoteDeviceCommunicator;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfDeviceCommunicator;
import org.opendaylight.controller.sal.connect.netconf.listener.NetconfSessionCapabilities;
import org.opendaylight.controller.sal.connect.util.RemoteDeviceId;
import org.opendaylight.protocol.framework.NeverReconnectStrategy;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.xml.sax.SAXException;

public class NetconfITSecureSTestTool extends AbstractNetconfConfigTest {

    //set up port both for testool device and test
    public static final int PORT = 17833;
    private static final InetSocketAddress TLS_ADDRESS = new InetSocketAddress("127.0.0.1", PORT);

    public static final String USERNAME = "user";
    public static final String PASSWORD = "pwd";

    private SshProxyServer sshProxyServer;

    private ExecutorService nioExec;
    private EventLoopGroup clientGroup;
    private ScheduledExecutorService minaTimerEx;

    private ExecutorService msgExec = Executors.newFixedThreadPool(8);

    final NetconfDeviceSimulator netconfDeviceSimulator = new NetconfDeviceSimulator();

    @Before
    public void setUp() throws Exception {
        nioExec = Executors.newFixedThreadPool(1);
        clientGroup = new NioEventLoopGroup();
        minaTimerEx = Executors.newScheduledThreadPool(1);
        sshProxyServer = new SshProxyServer(minaTimerEx, clientGroup, nioExec);

        //Set up parameters for testtool device
        Params params = new Params();
        params.debug = true;
        params.deviceCount = 1;
        params.startingPort = PORT;
        params.ssh = true;
        params.exi = true;

        final List<Integer> openDevices = netconfDeviceSimulator.start(params);
    }

    @After
    public void tearDown() throws Exception {
        sshProxyServer.close();
        clientGroup.shutdownGracefully().await();
        minaTimerEx.shutdownNow();
        nioExec.shutdownNow();

        //closing testtool
        netconfDeviceSimulator.close();

    }

    /**
     * Test all requests are handled properly and no mismatch occurs in listener
     */
    @Test(timeout = 6*60*1000)
    public void testSecureStress() throws Exception {
        final int requests = 4000;

        final NetconfClientDispatcher dispatch = new NetconfClientDispatcherImpl(getNettyThreadgroup(), getNettyThreadgroup(), getHashedWheelTimer());

        final NetconfDeviceCommunicator sessionListener = getSessionListener();

        try (TestingNetconfClient netconfClient = new TestingNetconfClient("testing-ssh-client", dispatch, getClientConfiguration(sessionListener));)
        {

            final AtomicInteger responseCounter = new AtomicInteger(0);
            final List<ListenableFuture<RpcResult<NetconfMessage>>> futures = Lists.newArrayList();

            class MyRunnable implements Runnable {

                private NetconfMessage getConfig;
                private int it;

                public MyRunnable(NetconfMessage getConfig, int it){
                    this.getConfig = getConfig;
                    this.it = it;
                }

                @Override
                public void run(){

                    ListenableFuture<RpcResult<NetconfMessage>> netconfMessageFuture;

                    netconfMessageFuture = sessionListener.sendRequest(getConfig, QName.create("namespace", "2012-12-12", "get"));

                    futures.add(netconfMessageFuture);
                    Futures.addCallback(netconfMessageFuture, new FutureCallback<RpcResult<NetconfMessage>>() {

                            @Override
                            public void onSuccess(final RpcResult<NetconfMessage> result) {

                                if(result.isSuccessful()&result.getErrors().isEmpty()) {
                                    responseCounter.incrementAndGet();
                                } else {
                                    for (RpcError rpcError : result.getErrors()) {

                                        rpcError.getCause().getMessage();

                                    }
                                    throw new RuntimeException("Failed result");
                                }
                            }

                            @Override
                            public void onFailure(final Throwable t) {
                                throw new RuntimeException(t);
                            }
                        }
                    );
                }
            }

            for (int i = 0; i < requests; i++) {

                NetconfMessage getConfig = getGetConfig();

                getConfig = changeMessageId(getConfig,i);

                Runnable worker = new MyRunnable(getConfig,i);
                msgExec.execute(worker);

            }

            msgExec.shutdown();

            while(!msgExec.isTerminated()){

            }
            // Wait for every future
            for (final ListenableFuture<RpcResult<NetconfMessage>> future : futures) {
                try {
                    future.get(3, TimeUnit.MINUTES);
                } catch (final TimeoutException e) {
                    fail("Request " + futures.indexOf(future) + " is not responding");
                }
            }

            sleep(5000);

            assertEquals(requests, responseCounter.get());

        }
    }

    private NetconfMessage changeMessageId(final NetconfMessage getConfig, final int i) throws IOException, SAXException {
        String s = XmlUtil.toString(getConfig.getDocument(), false);
        s = s.replace("101", Integer.toString(i));
        return new NetconfMessage(XmlUtil.readXmlToDocument(s));
    }

    public NetconfClientConfiguration getClientConfiguration(final NetconfClientSessionListener sessionListener) throws IOException {
        final NetconfClientConfigurationBuilder b = NetconfClientConfigurationBuilder.create();
        b.withAddress(TLS_ADDRESS);
        // Using session listener from sal-netconf-connector since stress test cannot be performed with simple listener
        b.withSessionListener(sessionListener);
        b.withReconnectStrategy(new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE, 5000));
        b.withProtocol(NetconfClientConfiguration.NetconfClientProtocol.SSH);
        b.withConnectionTimeoutMillis(5000);
        b.withAuthHandler(getAuthHandler());
        return b.build();
    }

    @Mock
    private RemoteDevice<NetconfSessionCapabilities, NetconfMessage> mockedRemoteDevice;

    private NetconfDeviceCommunicator getSessionListener() {
        MockitoAnnotations.initMocks(this);
        doNothing().when(mockedRemoteDevice).onRemoteSessionUp(any(NetconfSessionCapabilities.class), any(RemoteDeviceCommunicator.class));
        doNothing().when(mockedRemoteDevice).onRemoteSessionDown();
        return new NetconfDeviceCommunicator(new RemoteDeviceId("secure-test"), mockedRemoteDevice);
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
