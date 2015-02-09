/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.netconf.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.api.xml.XmlNetconfConstants;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcher;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcherImpl;
import org.opendaylight.controller.netconf.client.SimpleNetconfClientSessionListener;
import org.opendaylight.controller.netconf.client.TestingNetconfClient;
import org.opendaylight.controller.netconf.client.conf.NetconfClientConfiguration;
import org.opendaylight.controller.netconf.client.conf.NetconfClientConfigurationBuilder;
import org.opendaylight.controller.netconf.impl.osgi.NetconfOperationServiceFactoryListenerImpl;
import org.opendaylight.controller.netconf.impl.osgi.SessionMonitoringService;
import org.opendaylight.controller.netconf.mapping.api.Capability;
import org.opendaylight.controller.netconf.mapping.api.HandlingPriority;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperation;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationChainedExecution;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationService;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceFactory;
import org.opendaylight.controller.netconf.nettyutil.handler.exi.NetconfStartExiMessage;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessageAdditionalHeader;
import org.opendaylight.controller.netconf.util.messages.NetconfMessageUtil;
import org.opendaylight.controller.netconf.util.test.XmlFileLoader;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.opendaylight.protocol.framework.NeverReconnectStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

@RunWith(Parameterized.class)
public class ConcurrentClientsTest {
    private static final Logger LOG = LoggerFactory.getLogger(ConcurrentClientsTest.class);

    private static ExecutorService clientExecutor;

    private static final int CONCURRENCY = 32;
    private static final InetSocketAddress netconfAddress = new InetSocketAddress("127.0.0.1", 8303);

    private int nettyThreads;
    private Class<? extends Runnable> clientRunnable;
    private Set<String> serverCaps;

    public ConcurrentClientsTest(int nettyThreads, Class<? extends Runnable> clientRunnable, Set<String> serverCaps) {
        this.nettyThreads = nettyThreads;
        this.clientRunnable = clientRunnable;
        this.serverCaps = serverCaps;
    }

    @Parameterized.Parameters()
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{{4, TestingNetconfClientRunnable.class, NetconfServerSessionNegotiatorFactory.DEFAULT_BASE_CAPABILITIES},
                                            {1, TestingNetconfClientRunnable.class, NetconfServerSessionNegotiatorFactory.DEFAULT_BASE_CAPABILITIES},
                                            // empty set of capabilities = only base 1.0 netconf capability
                                            {4, TestingNetconfClientRunnable.class, Collections.emptySet()},
                                            {4, TestingNetconfClientRunnable.class, getOnlyExiServerCaps()},
                                            {4, TestingNetconfClientRunnable.class, getOnlyChunkServerCaps()},
                                            {4, BlockingClientRunnable.class, getOnlyExiServerCaps()},
                                            {1, BlockingClientRunnable.class, getOnlyExiServerCaps()},
        });
    }

    private EventLoopGroup nettyGroup;
    private NetconfClientDispatcher netconfClientDispatcher;

    private DefaultCommitNotificationProducer commitNot;

    HashedWheelTimer hashedWheelTimer;
    private TestingNetconfOperation testingNetconfOperation;

    public static SessionMonitoringService createMockedMonitoringService() {
        SessionMonitoringService monitoring = mock(SessionMonitoringService.class);
        doNothing().when(monitoring).onSessionUp(any(NetconfServerSession.class));
        doNothing().when(monitoring).onSessionDown(any(NetconfServerSession.class));
        return monitoring;
    }

    @BeforeClass
    public static void setUpClientExecutor() {
        clientExecutor = Executors.newFixedThreadPool(CONCURRENCY, new ThreadFactory() {
            int i = 1;

            @Override
            public Thread newThread(final Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("client-" + i++);
                thread.setDaemon(true);
                return thread;
            }
        });
    }

    @Before
    public void setUp() throws Exception {
        hashedWheelTimer = new HashedWheelTimer();
        nettyGroup = new NioEventLoopGroup(nettyThreads);
        netconfClientDispatcher = new NetconfClientDispatcherImpl(nettyGroup, nettyGroup, hashedWheelTimer);

        NetconfOperationServiceFactoryListenerImpl factoriesListener = new NetconfOperationServiceFactoryListenerImpl();

        testingNetconfOperation = new TestingNetconfOperation();
        factoriesListener.onAddNetconfOperationServiceFactory(new TestingOperationServiceFactory(testingNetconfOperation));

        SessionIdProvider idProvider = new SessionIdProvider();

        NetconfServerSessionNegotiatorFactory serverNegotiatorFactory = new NetconfServerSessionNegotiatorFactory(
                hashedWheelTimer, factoriesListener, idProvider, 5000, commitNot, createMockedMonitoringService(), serverCaps);

        commitNot = new DefaultCommitNotificationProducer(ManagementFactory.getPlatformMBeanServer());

        NetconfServerDispatcherImpl.ServerChannelInitializer serverChannelInitializer = new NetconfServerDispatcherImpl.ServerChannelInitializer(serverNegotiatorFactory);
        final NetconfServerDispatcherImpl dispatch = new NetconfServerDispatcherImpl(serverChannelInitializer, nettyGroup, nettyGroup);

        ChannelFuture s = dispatch.createServer(netconfAddress);
        s.await();
    }

    @After
    public void tearDown(){
        commitNot.close();
        hashedWheelTimer.stop();
        try {
            nettyGroup.shutdownGracefully().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Ignoring exception while cleaning up after test", e);
        }
    }

    @AfterClass
    public static void tearDownClientExecutor() {
        clientExecutor.shutdownNow();
    }

    @Test(timeout = CONCURRENCY * 1000)
    public void testConcurrentClients() throws Exception {

        List<Future<?>> futures = Lists.newArrayListWithCapacity(CONCURRENCY);

        for (int i = 0; i < CONCURRENCY; i++) {
            futures.add(clientExecutor.submit(getInstanceOfClientRunnable()));
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            } catch (ExecutionException e) {
                LOG.error("Thread for testing client failed", e);
                fail("Client failed: " + e.getMessage());
            }
        }

        assertEquals(CONCURRENCY, testingNetconfOperation.getMessageCount());
    }

    public static Set<String> getOnlyExiServerCaps() {
        return Sets.newHashSet(
                XmlNetconfConstants.URN_IETF_PARAMS_NETCONF_BASE_1_0,
                XmlNetconfConstants.URN_IETF_PARAMS_NETCONF_CAPABILITY_EXI_1_0
        );
    }

    public static Set<String> getOnlyChunkServerCaps() {
        return Sets.newHashSet(
                XmlNetconfConstants.URN_IETF_PARAMS_NETCONF_BASE_1_0,
                XmlNetconfConstants.URN_IETF_PARAMS_NETCONF_BASE_1_1
        );
    }

    public Runnable getInstanceOfClientRunnable() throws Exception {
        return clientRunnable.getConstructor(ConcurrentClientsTest.class).newInstance(this);
    }

    /**
     * Responds to all operations except start-exi and counts all requests
     */
    private static class TestingNetconfOperation implements NetconfOperation {

        private final AtomicLong counter = new AtomicLong();

        @Override
        public HandlingPriority canHandle(Document message) {
            return XmlUtil.toString(message).contains(NetconfStartExiMessage.START_EXI) ?
                    HandlingPriority.CANNOT_HANDLE :
                    HandlingPriority.HANDLE_WITH_MAX_PRIORITY;
        }

        @Override
        public Document handle(Document requestMessage, NetconfOperationChainedExecution subsequentOperation) throws NetconfDocumentedException {
            try {
                LOG.info("Handling netconf message from test {}", XmlUtil.toString(requestMessage));
                counter.getAndIncrement();
                return XmlUtil.readXmlToDocument("<test/>");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public long getMessageCount() {
            return counter.get();
        }
    }

    /**
     * Hardcoded operation service factory
     */
    private static class TestingOperationServiceFactory implements NetconfOperationServiceFactory {
        private final NetconfOperation[] operations;

        public TestingOperationServiceFactory(final NetconfOperation... operations) {
            this.operations = operations;
        }

        @Override
        public NetconfOperationService createService(String netconfSessionIdForReporting) {
            return new NetconfOperationService() {
                @Override
                public Set<Capability> getCapabilities() {
                    return Collections.emptySet();
                }

                @Override
                public Set<NetconfOperation> getNetconfOperations() {
                    return Sets.newHashSet(operations);
                }

                @Override
                public void close() {}
            };
        }
    }

    /**
     * Pure socket based blocking client
     */
    public final class BlockingClientRunnable implements Runnable {

        @Override
        public void run() {
            try {
                run2();
            } catch (Exception e) {
                throw new IllegalStateException(Thread.currentThread().getName(), e);
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
            LOG.info(sb.toString());

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
            LOG.info(sb.toString());
            clientSocket.close();
        }
    }

    /**
     * TestingNetconfClient based runnable
     */
    public final class TestingNetconfClientRunnable implements Runnable {

        @Override
        public void run() {
            try {
                final TestingNetconfClient netconfClient =
                        new TestingNetconfClient(Thread.currentThread().getName(), netconfClientDispatcher, getClientConfig());
                long sessionId = netconfClient.getSessionId();
                LOG.info("Client with session id {}: hello exchanged", sessionId);

                final NetconfMessage getMessage = XmlFileLoader
                        .xmlFileToNetconfMessage("netconfMessages/getConfig.xml");
                NetconfMessage result = netconfClient.sendRequest(getMessage).get();
                LOG.info("Client with session id {}: got result {}", sessionId, result);

                Preconditions.checkState(NetconfMessageUtil.isErrorMessage(result) == false,
                        "Received error response: " + XmlUtil.toString(result.getDocument()) + " to request: "
                                + XmlUtil.toString(getMessage.getDocument()));

                netconfClient.close();
                LOG.info("Client with session id {}: ended", sessionId);
            } catch (final Exception e) {
                throw new IllegalStateException(Thread.currentThread().getName(), e);
            }
        }

        private NetconfClientConfiguration getClientConfig() {
            final NetconfClientConfigurationBuilder b = NetconfClientConfigurationBuilder.create();
            b.withAddress(netconfAddress);
            b.withAdditionalHeader(new NetconfHelloMessageAdditionalHeader("uname", "10.10.10.1", "830", "tcp",
                    "client"));
            b.withSessionListener(new SimpleNetconfClientSessionListener());
            b.withReconnectStrategy(new NeverReconnectStrategy(GlobalEventExecutor.INSTANCE,
                    NetconfClientConfigurationBuilder.DEFAULT_CONNECTION_TIMEOUT_MILLIS));
            return b.build();
        }
    }
}
