/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.it;

import static java.util.Arrays.asList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import ch.ethz.ssh2.Connection;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.controller.netconf.api.NetconfMessage;
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
import org.opendaylight.controller.netconf.ssh.NetconfSSHServer;
import org.opendaylight.controller.netconf.ssh.authentication.AuthProvider;
import org.opendaylight.controller.netconf.ssh.authentication.PEMGenerator;
import org.opendaylight.controller.netconf.util.messages.NetconfMessageUtil;
import org.opendaylight.controller.netconf.util.osgi.NetconfConfigUtil;
import org.opendaylight.controller.netconf.util.test.XmlFileLoader;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.protocol.framework.NeverReconnectStrategy;

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

        NetconfOperationServiceFactoryListenerImpl factoriesListener = new NetconfOperationServiceFactoryListenerImpl();
        factoriesListener.onAddNetconfOperationServiceFactory(new NetconfOperationServiceFactoryImpl(getYangStore()));

        commitNot = new DefaultCommitNotificationProducer(ManagementFactory.getPlatformMBeanServer());


        final NetconfServerDispatcher dispatchS = createDispatcher(factoriesListener);
        ChannelFuture s = dispatchS.createLocalServer(NetconfConfigUtil.getNetconfLocalAddress());
        s.await();
        EventLoopGroup bossGroup  = new NioEventLoopGroup();
        sshServer = NetconfSSHServer.start(tlsAddress.getPort(), NetconfConfigUtil.getNetconfLocalAddress(), getAuthProvider(), bossGroup);
    }

    private NetconfServerDispatcher createDispatcher(NetconfOperationServiceFactoryListenerImpl factoriesListener) {
        return super.createDispatcher(factoriesListener, NetconfITTest.getNetconfMonitoringListenerService(), commitNot);
    }

    @After
    public void tearDown() throws Exception {
        sshServer.stop();
        commitNot.close();
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
        NetconfClientDispatcher dispatch = new NetconfClientDispatcherImpl(getNettyThreadgroup(), getNettyThreadgroup(), getHashedWheelTimer());
        try (TestingNetconfClient netconfClient = new TestingNetconfClient("testing-ssh-client", dispatch, getClientConfiguration())) {
            NetconfMessage response = netconfClient.sendMessage(getConfig);
            Assert.assertFalse("Unexpected error message " + XmlUtil.toString(response.getDocument()),
                    NetconfMessageUtil.isErrorMessage(response));

            NetconfMessage gs = new NetconfMessage(XmlUtil.readXmlToDocument("<rpc message-id=\"2\"\n" +
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
        doReturn(PEMGenerator.generate().toCharArray()).when(mock).getPEMAsCharArray();
        return mock;
    }

    public AuthenticationHandler getAuthHandler() throws IOException {
        final AuthenticationHandler authHandler = mock(AuthenticationHandler.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable {
                Connection conn = (Connection) invocation.getArguments()[0];
                conn.authenticateWithPassword("user", "pwd");
                return null;
            }
        }).when(authHandler).authenticate(any(Connection.class));
        doReturn("auth handler").when(authHandler).toString();
        return authHandler;
    }
}
