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

import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opendaylight.controller.netconf.api.NetconfDocumentedException;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcher;
import org.opendaylight.controller.netconf.client.test.TestingNetconfClient;
import org.opendaylight.controller.netconf.impl.osgi.NetconfOperationServiceFactoryListenerImpl;
import org.opendaylight.controller.netconf.impl.osgi.SessionMonitoringService;
import org.opendaylight.controller.netconf.mapping.api.Capability;
import org.opendaylight.controller.netconf.mapping.api.HandlingPriority;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperation;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationChainedExecution;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationService;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationServiceFactory;
import org.opendaylight.controller.netconf.util.messages.NetconfHelloMessageAdditionalHeader;
import org.opendaylight.controller.netconf.util.messages.NetconfMessageUtil;
import org.opendaylight.controller.netconf.util.messages.NetconfStartExiMessage;
import org.opendaylight.controller.netconf.util.test.XmlFileLoader;
import org.opendaylight.controller.netconf.util.xml.XmlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;

import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.HashedWheelTimer;

public class ConcurrentClientsTest {

    private static final int CONCURRENCY = 64;
    public static final int NETTY_THREADS = 4;

    private EventLoopGroup nettyGroup;
    private NetconfClientDispatcher netconfClientDispatcher;

    private final InetSocketAddress netconfAddress = new InetSocketAddress("127.0.0.1", 8303);

    static final Logger logger = LoggerFactory.getLogger(ConcurrentClientsTest.class);

    private DefaultCommitNotificationProducer commitNot;

    HashedWheelTimer hashedWheelTimer;
    private TestingNetconfOperation testingNetconfOperation;

    public static SessionMonitoringService createMockedMonitoringService() {
        SessionMonitoringService monitoring = mock(SessionMonitoringService.class);
        doNothing().when(monitoring).onSessionUp(any(NetconfServerSession.class));
        doNothing().when(monitoring).onSessionDown(any(NetconfServerSession.class));
        return monitoring;
    }

    // TODO refactor and test with different configurations

    @Before
    public void setUp() throws Exception {

        nettyGroup = new NioEventLoopGroup(NETTY_THREADS);
        NetconfHelloMessageAdditionalHeader additionalHeader = new NetconfHelloMessageAdditionalHeader("uname", "10.10.10.1", "830", "tcp", "client");
        netconfClientDispatcher = new NetconfClientDispatcher( nettyGroup, nettyGroup, additionalHeader, 5000);

        NetconfOperationServiceFactoryListenerImpl factoriesListener = new NetconfOperationServiceFactoryListenerImpl();
        testingNetconfOperation = new TestingNetconfOperation();
        factoriesListener.onAddNetconfOperationServiceFactory(mockOpF(testingNetconfOperation));

        SessionIdProvider idProvider = new SessionIdProvider();
        hashedWheelTimer = new HashedWheelTimer();

        NetconfServerSessionNegotiatorFactory serverNegotiatorFactory = new NetconfServerSessionNegotiatorFactory(
                hashedWheelTimer, factoriesListener, idProvider, 5000, commitNot, createMockedMonitoringService());

        commitNot = new DefaultCommitNotificationProducer(ManagementFactory.getPlatformMBeanServer());

        NetconfServerDispatcher.ServerChannelInitializer serverChannelInitializer = new NetconfServerDispatcher.ServerChannelInitializer(serverNegotiatorFactory);
        final NetconfServerDispatcher dispatch = new NetconfServerDispatcher(serverChannelInitializer, nettyGroup, nettyGroup);

        ChannelFuture s = dispatch.createServer(netconfAddress);
        s.await();
    }

    @After
    public void tearDown(){
        hashedWheelTimer.stop();
        nettyGroup.shutdownGracefully();
    }

    private NetconfOperationServiceFactory mockOpF(final NetconfOperation... operations) {
        return new TestingOperationServiceFactory(operations);
    }

    @After
    public void cleanUp() throws Exception {
        commitNot.close();
    }

    @Test(timeout = 30 * 1000)
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

        assertEquals(CONCURRENCY, testingNetconfOperation.getMessageCount());
    }

    /**
     * Cannot handle CHUNK, make server configurable
     */
    @Ignore
    @Test(timeout = 30 * 1000)
    public void synchronizationTest() throws Exception {
        new BlockingThread("foo").run2();
    }

    /**
     * Cannot handle CHUNK, make server configurable
     */
    @Ignore
    @Test(timeout = 30 * 1000)
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
                logger.info("Handling netconf message from test {}", XmlUtil.toString(requestMessage));
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
                    return Sets.<NetconfOperation> newHashSet(operations);
                }

                @Override
                public void close() {
                }
            };
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
                final TestingNetconfClient netconfClient = new TestingNetconfClient(clientId, netconfAddress, netconfClientDispatcher);
                long sessionId = netconfClient.getSessionId();
                logger.info("Client with sessionid {} hello exchanged", sessionId);

                final NetconfMessage getMessage = XmlFileLoader
                        .xmlFileToNetconfMessage("netconfMessages/getConfig.xml");
                NetconfMessage result = netconfClient.sendRequest(getMessage).get();
                logger.info("Client with sessionid {} got result {}", sessionId, result);

                Preconditions.checkState(NetconfMessageUtil.isErrorMessage(result) == false,
                        "Received error response: " + XmlUtil.toString(result.getDocument()) +
                                " to request: " + XmlUtil.toString(getMessage.getDocument()));

                netconfClient.close();
                logger.info("Client with session id {} ended", sessionId);
                thrownException = Optional.absent();
            } catch (final Exception e) {
                thrownException = Optional.of(e);
            }
        }
    }
}
