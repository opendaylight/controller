/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.it;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.auth.AuthProvider;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcher;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcherImpl;
import org.opendaylight.controller.netconf.client.SimpleNetconfClientSessionListener;
import org.opendaylight.controller.netconf.client.conf.NetconfClientConfiguration;
import org.opendaylight.controller.netconf.client.conf.NetconfClientConfigurationBuilder;
import org.opendaylight.controller.netconf.client.test.TestingNetconfClient;
import org.opendaylight.controller.netconf.confignetconfconnector.osgi.NetconfOperationServiceFactoryImpl;
import org.opendaylight.controller.netconf.confignetconfconnector.osgi.YangStoreException;
import org.opendaylight.controller.netconf.impl.DefaultCommitNotificationProducer;
import org.opendaylight.controller.netconf.impl.NetconfServerDispatcher;
import org.opendaylight.controller.netconf.impl.osgi.NetconfOperationServiceFactoryListenerImpl;
import org.opendaylight.controller.netconf.nettyutil.handler.ssh.authentication.AuthenticationHandler;
import org.opendaylight.controller.netconf.nettyutil.handler.ssh.authentication.LoginPassword;
import org.opendaylight.controller.netconf.ssh.NetconfSSHServer;
import org.opendaylight.controller.netconf.ssh.authentication.PEMGenerator;
import org.opendaylight.controller.netconf.util.messages.NetconfMessageUtil;
import org.opendaylight.controller.netconf.util.osgi.NetconfConfigUtil;
import org.opendaylight.controller.netconf.util.test.XmlFileLoader;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.protocol.framework.NeverReconnectStrategy;

import com.google.common.collect.Lists;

import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.GlobalEventExecutor;

public class NetconfITSecureTest extends AbstractNetconfConfigTest {

    private static final InetSocketAddress tlsAddress = new InetSocketAddress("127.0.0.1", 12024);

    private DefaultCommitNotificationProducer commitNot;
    private NetconfSSHServer sshServer;
    private NetconfMessage getConfig;

    @Before
    public void setUp() throws Exception {
        this.getConfig = XmlFileLoader.xmlFileToNetconfMessage("netconfMessages/getConfig.xml");

        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(mockedContext, getModuleFactories().toArray(
                new ModuleFactory[0])));

        final NetconfOperationServiceFactoryListenerImpl factoriesListener = new NetconfOperationServiceFactoryListenerImpl();
        factoriesListener.onAddNetconfOperationServiceFactory(new NetconfOperationServiceFactoryImpl(getYangStore()));

        commitNot = new DefaultCommitNotificationProducer(ManagementFactory.getPlatformMBeanServer());


        final NetconfServerDispatcher dispatchS = createDispatcher(factoriesListener);
        ChannelFuture s = dispatchS.createLocalServer(NetconfConfigUtil.getNetconfLocalAddress());
        s.await();
        EventLoopGroup bossGroup  = new NioEventLoopGroup();

        final char[] pem = PEMGenerator.generate().toCharArray();
        sshServer = NetconfSSHServer.start(tlsAddress.getPort(), NetconfConfigUtil.getNetconfLocalAddress(), bossGroup, pem);
        sshServer.setAuthProvider(getAuthProvider());
    }

    private NetconfServerDispatcher createDispatcher(final NetconfOperationServiceFactoryListenerImpl factoriesListener) {
        return super.createDispatcher(factoriesListener, NetconfITTest.getNetconfMonitoringListenerService(), commitNot);
    }

    @After
    public void tearDown() throws Exception {
        sshServer.close();
        commitNot.close();
        sshServer.join();
    }

    private HardcodedYangStoreService getYangStore() throws YangStoreException, IOException {
        final Collection<InputStream> yangDependencies = NetconfITTest.getBasicYangs();
        return new HardcodedYangStoreService(yangDependencies);
    }

    protected List<ModuleFactory> getModuleFactories() {
        return asList(NetconfITTest.FACTORIES);
    }

    @Test
    public void testSecure() throws Exception {
        final NetconfClientDispatcher dispatch = new NetconfClientDispatcherImpl(getNettyThreadgroup(), getNettyThreadgroup(), getHashedWheelTimer());
        try (TestingNetconfClient netconfClient = new TestingNetconfClient("testing-ssh-client", dispatch, getClientConfiguration())) {
            NetconfMessage response = netconfClient.sendMessage(getConfig);
            Assert.assertFalse("Unexpected error message " + XmlUtil.toString(response.getDocument()),
                    NetconfMessageUtil.isErrorMessage(response));

            final NetconfMessage gs = new NetconfMessage(XmlUtil.readXmlToDocument("<rpc message-id=\"2\"\n" +
                    "     xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n" +
                    "    <get-schema xmlns=\"urn:ietf:params:xml:ns:yang:ietf-netconf-monitoring\">\n" +
                    "        <identifier>config</identifier>\n" +
                    "    </get-schema>\n" +
                    "</rpc>\n"));

            response = netconfClient.sendMessage(gs);
            Assert.assertFalse("Unexpected error message " + XmlUtil.toString(response.getDocument()),
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
                final Future<NetconfMessage> netconfMessageFuture = netconfClient.sendRequest(getConfig);
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

            org.junit.Assert.assertEquals(requests, responseCounter.get());
        }
    }

    public NetconfClientConfiguration getClientConfiguration() throws IOException {
        final NetconfClientConfigurationBuilder b = NetconfClientConfigurationBuilder.create();
        b.withAddress(tlsAddress);
        b.withSessionListener(new SimpleNetconfClientSessionListener());
        b.withReconnectStrategy(new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE, 5000));
        b.withProtocol(NetconfClientConfiguration.NetconfClientProtocol.SSH);
        b.withConnectionTimeoutMillis(5000);
        b.withAuthHandler(getAuthHandler());
        return b.build();
    }

    public AuthProvider getAuthProvider() throws Exception {
        AuthProvider mock = mock(AuthProvider.class);
        doReturn(true).when(mock).authenticated(anyString(), anyString());
        return mock;
    }

    public AuthenticationHandler getAuthHandler() throws IOException {
        return new LoginPassword("user", "pwd");
    }
}
