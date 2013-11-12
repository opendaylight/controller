/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.config.util.ConfigRegistryJMXClient;
import org.opendaylight.controller.config.util.ConfigTransactionJMXClient;
import org.opendaylight.controller.config.yang.store.api.YangStoreService;
import org.opendaylight.controller.config.yang.store.api.YangStoreSnapshot;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.api.NetconfOperationRouter;
import org.opendaylight.controller.netconf.client.NetconfClient;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcher;
import org.opendaylight.controller.netconf.impl.osgi.NetconfOperationServiceFactoryListenerImpl;
import org.opendaylight.controller.netconf.mapping.api.Capability;
import org.opendaylight.controller.netconf.mapping.api.HandlingPriority;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperation;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationFilter;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationService;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceFactory;
import org.opendaylight.controller.netconf.util.test.XmlFileLoader;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.management.ObjectName;
import javax.net.ssl.SSLContext;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class ConcurrentClientsTest {

    private static final int CONCURRENCY = 16;
    private static EventLoopGroup nettyGroup = new NioEventLoopGroup();
    public static final NetconfClientDispatcher NETCONF_CLIENT_DISPATCHER = new NetconfClientDispatcher(
            Optional.<SSLContext> absent(), nettyGroup, nettyGroup);

    @Mock
    private YangStoreService yangStoreService;
    @Mock
    private ConfigRegistryJMXClient jmxClient;

    private final InetSocketAddress netconfAddress = new InetSocketAddress("127.0.0.1", 8303);

    static final Logger logger = LoggerFactory.getLogger(ConcurrentClientsTest.class);

    private DefaultCommitNotificationProducer commitNot;
    private NetconfServerDispatcher dispatch;


    @Before
    public void setUp() throws Exception {
        { // init mocks
            MockitoAnnotations.initMocks(this);
            final YangStoreSnapshot yStore = mock(YangStoreSnapshot.class);
            doReturn(yStore).when(this.yangStoreService).getYangStoreSnapshot();
            doReturn(Collections.emptyMap()).when(yStore).getModuleMXBeanEntryMap();
            doReturn(Collections.emptyMap()).when(yStore).getModuleMap();

            final ConfigTransactionJMXClient mockedTCl = mock(ConfigTransactionJMXClient.class);
            doReturn(mockedTCl).when(this.jmxClient).getConfigTransactionClient(any(ObjectName.class));

            doReturn(Collections.emptySet()).when(jmxClient).lookupConfigBeans();
        }

        NetconfOperationServiceFactoryListenerImpl factoriesListener = new NetconfOperationServiceFactoryListenerImpl();
        factoriesListener.onAddNetconfOperationServiceFactory(mockOpF());

        SessionIdProvider idProvider = new SessionIdProvider();
        NetconfServerSessionNegotiatorFactory serverNegotiatorFactory = new NetconfServerSessionNegotiatorFactory(
                new HashedWheelTimer(5000, TimeUnit.MILLISECONDS), factoriesListener, idProvider);

        commitNot = new DefaultCommitNotificationProducer(ManagementFactory.getPlatformMBeanServer());

        NetconfServerSessionListenerFactory listenerFactory = new NetconfServerSessionListenerFactory(
                factoriesListener, commitNot, idProvider);
        NetconfServerDispatcher.ServerSslChannelInitializer serverChannelInitializer = new NetconfServerDispatcher.ServerSslChannelInitializer(
                Optional.<SSLContext> absent(), serverNegotiatorFactory, listenerFactory);
        dispatch = new NetconfServerDispatcher(serverChannelInitializer, nettyGroup, nettyGroup);

        ChannelFuture s = dispatch.createServer(netconfAddress);
        s.await();
    }

    @AfterClass
    public static void tearDownStatic() {
        nettyGroup.shutdownGracefully();
    }

    private NetconfOperationServiceFactory mockOpF() {
        return new NetconfOperationServiceFactory() {
            @Override
            public NetconfOperationService createService(long netconfSessionId, String netconfSessionIdForReporting) {
                return new NetconfOperationService() {
                    @Override
                    public Set<Capability> getCapabilities() {
                        return Collections.emptySet();
                    }

                    @Override
                    public Set<NetconfOperation> getNetconfOperations() {
                        return Sets.<NetconfOperation> newHashSet(new NetconfOperation() {
                            @Override
                            public HandlingPriority canHandle(Document message) {
                                return HandlingPriority.getHandlingPriority(Integer.MAX_VALUE);
                            }

                            @Override
                            public Document handle(Document message, NetconfOperationRouter operationRouter)
                                    throws NetconfDocumentedException {
                                try {
                                    return XmlUtil.readXmlToDocument("<test/>");
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        });
                    }

                    @Override
                    public Set<NetconfOperationFilter> getFilters() {
                        return Collections.emptySet();
                    }

                    @Override
                    public void close() {
                    }
                };
            }
        };
    }

    @After
    public void cleanUp() throws Exception {
        commitNot.close();
    }

    @Test
    public void multipleClients() throws Exception {
        List<TestingThread> threads = new ArrayList<>();

        final int attempts = 5;
        for (int i = 0; i < CONCURRENCY; i++) {
            TestingThread thread = new TestingThread(String.valueOf(i), attempts);
            threads.add(thread);
            thread.start();
        }

        for (TestingThread thread : threads) {
            thread.join();
            if(thread.thrownException.isPresent()) {
                Exception exception = thread.thrownException.get();
                logger.error("Thread for testing client failed", exception);
                fail("Client thread " + thread + " failed: " + exception.getMessage());
            }
        }
    }

    @Test
    public void synchronizationTest() throws Exception {
        new BlockingThread("foo").run2();
    }

    @Test
    public void multipleBlockingClients() throws Exception {
        List<BlockingThread> threads = new ArrayList<>();
        for (int i = 0; i < CONCURRENCY; i++) {
            BlockingThread thread = new BlockingThread(String.valueOf(i));
            threads.add(thread);
            thread.start();
        }

        for (BlockingThread thread : threads) {
            thread.join();
            if(thread.thrownException.isPresent()) {
                Exception exception = thread.thrownException.get();
                logger.error("Thread for testing client failed", exception);
                fail("Client thread " + thread + " failed: " + exception.getMessage());
            }
        }
    }

    class BlockingThread extends Thread {
        private Optional<Exception> thrownException;

        public BlockingThread(String name) {
            super("client-" + name);
        }

        @Override
        public void run() {
            try {
                run2();
                thrownException = Optional.absent();
            } catch (Exception e) {
                thrownException = Optional.of(e);
            }
        }

        private void run2() throws Exception {
            InputStream clientHello = checkNotNull(XmlFileLoader
                    .getResourceAsStream("netconfMessages/client_hello.xml"));
            InputStream getConfig = checkNotNull(XmlFileLoader.getResourceAsStream("netconfMessages/getConfig.xml"));

            Socket clientSocket = new Socket(netconfAddress.getHostString(), netconfAddress.getPort());
            DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
            InputStreamReader inFromServer = new InputStreamReader(clientSocket.getInputStream());

            StringBuffer sb = new StringBuffer();
            while (sb.toString().endsWith("]]>]]>") == false) {
                sb.append((char) inFromServer.read());
            }
            logger.info(sb.toString());

            outToServer.write(IOUtils.toByteArray(clientHello));
            outToServer.write("]]>]]>".getBytes());
            outToServer.flush();
            // Thread.sleep(100);
            outToServer.write(IOUtils.toByteArray(getConfig));
            outToServer.write("]]>]]>".getBytes());
            outToServer.flush();
            Thread.sleep(100);
            sb = new StringBuffer();
            while (sb.toString().endsWith("]]>]]>") == false) {
                sb.append((char) inFromServer.read());
            }
            logger.info(sb.toString());
            clientSocket.close();
        }
    }

    class TestingThread extends Thread {

        private final String clientId;
        private final int attempts;
        private Optional<Exception> thrownException;

        TestingThread(String clientId, int attempts) {
            this.clientId = clientId;
            this.attempts = attempts;
            setName("client-" + clientId);
        }

        @Override
        public void run() {
            try {
                final NetconfClient netconfClient = new NetconfClient(clientId, netconfAddress, NETCONF_CLIENT_DISPATCHER);
                long sessionId = netconfClient.getSessionId();
                logger.info("Client with sessionid {} hello exchanged", sessionId);

                final NetconfMessage getMessage = XmlFileLoader
                        .xmlFileToNetconfMessage("netconfMessages/getConfig.xml");
                NetconfMessage result = netconfClient.sendMessage(getMessage);
                logger.info("Client with sessionid {} got result {}", sessionId, result);
                netconfClient.close();
                logger.info("Client with session id {} ended", sessionId);
                thrownException = Optional.absent();
            } catch (final Exception e) {
                thrownException = Optional.of(e);
            }
        }
    }
}
