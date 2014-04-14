/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.it;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.opendaylight.controller.netconf.util.test.XmlUnitUtil.assertContainsElementWithName;
import static org.opendaylight.controller.netconf.util.test.XmlUnitUtil.assertElementsCount;
import static org.opendaylight.controller.netconf.util.xml.XmlUtil.readXmlToDocument;
import io.netty.channel.ChannelFuture;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.management.InstanceNotFoundException;
import javax.management.Notification;
import javax.management.NotificationListener;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.config.manager.impl.factoriesresolver.HardcodedModuleFactoriesResolver;
import org.opendaylight.controller.config.persist.api.ConfigSnapshotHolder;
import org.opendaylight.controller.config.persist.api.Persister;
import org.opendaylight.controller.config.spi.ModuleFactory;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.api.jmx.CommitJMXNotification;
import org.opendaylight.controller.netconf.api.monitoring.NetconfManagementSession;
import org.opendaylight.controller.netconf.client.NetconfClientDispatcher;
import org.opendaylight.controller.netconf.client.test.TestingNetconfClient;
import org.opendaylight.controller.netconf.confignetconfconnector.osgi.NetconfOperationServiceFactoryImpl;
import org.opendaylight.controller.netconf.confignetconfconnector.osgi.YangStoreException;
import org.opendaylight.controller.netconf.impl.DefaultCommitNotificationProducer;
import org.opendaylight.controller.netconf.impl.NetconfServerDispatcher;
import org.opendaylight.controller.netconf.impl.osgi.NetconfMonitoringServiceImpl;
import org.opendaylight.controller.netconf.impl.osgi.NetconfOperationServiceFactoryListenerImpl;
import org.opendaylight.controller.netconf.impl.osgi.NetconfOperationServiceSnapshotImpl;
import org.opendaylight.controller.netconf.impl.osgi.SessionMonitoringService;
import org.opendaylight.controller.netconf.mapping.api.Capability;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationProvider;
import org.opendaylight.controller.netconf.mapping.api.NetconfOperationService;
import org.opendaylight.controller.netconf.monitoring.osgi.NetconfMonitoringActivator;
import org.opendaylight.controller.netconf.monitoring.osgi.NetconfMonitoringOperationService;
import org.opendaylight.controller.netconf.persist.impl.ConfigPersisterNotificationHandler;
import org.opendaylight.controller.netconf.util.test.XmlFileLoader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class NetconfConfigPersisterITTest extends AbstractNetconfConfigTest {

    private static final InetSocketAddress tcpAddress = new InetSocketAddress("127.0.0.1", 12023);



    private NetconfClientDispatcher clientDispatcher;

    DefaultCommitNotificationProducer commitNotifier;

    @Before
    public void setUp() throws Exception {
        super.initConfigTransactionManagerImpl(new HardcodedModuleFactoriesResolver(NetconfITTest.getModuleFactoriesS().toArray(
                new ModuleFactory[0])));

        NetconfMonitoringServiceImpl monitoringService = new NetconfMonitoringServiceImpl(getNetconfOperationProvider());

        NetconfOperationServiceFactoryListenerImpl factoriesListener = new NetconfOperationServiceFactoryListenerImpl();
        factoriesListener.onAddNetconfOperationServiceFactory(new NetconfOperationServiceFactoryImpl(getYangStore()));
        factoriesListener
                .onAddNetconfOperationServiceFactory(new NetconfMonitoringActivator.NetconfMonitoringOperationServiceFactory(
                        new NetconfMonitoringOperationService(monitoringService)));


        commitNotifier = new DefaultCommitNotificationProducer(platformMBeanServer);
        NetconfServerDispatcher dispatch = createDispatcher(factoriesListener, mockSessionMonitoringService(), commitNotifier);
        ChannelFuture s = dispatch.createServer(tcpAddress);
        s.await();

        clientDispatcher = new NetconfClientDispatcher(nettyThreadgroup, nettyThreadgroup, 5000);
    }

    @After
    public void cleanUp(){
        commitNotifier.close();
    }

    private HardcodedYangStoreService getYangStore() throws YangStoreException, IOException {
        final Collection<InputStream> yangDependencies = NetconfITTest.getBasicYangs();
        return new HardcodedYangStoreService(yangDependencies);
    }


    protected SessionMonitoringService mockSessionMonitoringService() {
        SessionMonitoringService mockedSessionMonitor = mock(SessionMonitoringService.class);
        doNothing().when(mockedSessionMonitor).onSessionUp(any(NetconfManagementSession.class));
        doNothing().when(mockedSessionMonitor).onSessionDown(any(NetconfManagementSession.class));
        return mockedSessionMonitor;
    }



    @Test
    public void testNetconfCommitNotifications() throws Exception {

        VerifyingNotificationListener notificationVerifier = createCommitNotificationListener();
        VerifyingPersister mockedAggregator = mockAggregator();

        try (TestingNetconfClient persisterClient = new TestingNetconfClient("persister", tcpAddress, 4000, clientDispatcher)) {
            try (ConfigPersisterNotificationHandler configPersisterNotificationHandler = new ConfigPersisterNotificationHandler(
                    platformMBeanServer, mockedAggregator)) {


                try (TestingNetconfClient netconfClient = new TestingNetconfClient("client", tcpAddress, 4000, clientDispatcher)) {
                    NetconfMessage response = netconfClient.sendMessage(loadGetConfigMessage());
                    assertContainsElementWithName(response.getDocument(), "modules");
                    assertContainsElementWithName(response.getDocument(), "services");
                    response = netconfClient.sendMessage(loadCommitMessage());
                    assertContainsElementWithName(response.getDocument(), "ok");

                    response = netconfClient.sendMessage(loadEditConfigMessage());
                    assertContainsElementWithName(response.getDocument(), "ok");
                    response = netconfClient.sendMessage(loadCommitMessage());
                    assertContainsElementWithName(response.getDocument(), "ok");
                }
            }
        }

        notificationVerifier.assertNotificationCount(2);
        notificationVerifier.assertNotificationContent(0, 0, 0, 9);
        notificationVerifier.assertNotificationContent(1, 4, 4, 9);

        mockedAggregator.assertSnapshotCount(2);
        // Capabilities are stripped for persister
        mockedAggregator.assertSnapshotContent(0, 0, 0, 1);
        mockedAggregator.assertSnapshotContent(1, 4, 4, 3);
    }

    private VerifyingPersister mockAggregator() throws IOException {
        return new VerifyingPersister();
    }

    private VerifyingNotificationListener createCommitNotificationListener() throws InstanceNotFoundException {
        VerifyingNotificationListener listener = new VerifyingNotificationListener();
        platformMBeanServer.addNotificationListener(DefaultCommitNotificationProducer.OBJECT_NAME, listener, null, null);
        return listener;
    }

    private NetconfMessage loadGetConfigMessage() throws Exception {
        return XmlFileLoader.xmlFileToNetconfMessage("netconfMessages/getConfig.xml");
    }

    private NetconfMessage loadEditConfigMessage() throws Exception {
        return XmlFileLoader.xmlFileToNetconfMessage("netconfMessages/editConfig.xml");
    }

    private NetconfMessage loadCommitMessage() throws Exception {
        return XmlFileLoader.xmlFileToNetconfMessage("netconfMessages/commit.xml");
    }


    public NetconfOperationProvider getNetconfOperationProvider() {
        NetconfOperationProvider factoriesListener = mock(NetconfOperationProvider.class);
        NetconfOperationServiceSnapshotImpl snap = mock(NetconfOperationServiceSnapshotImpl.class);
        NetconfOperationService service = mock(NetconfOperationService.class);
        Set<Capability> caps = Sets.newHashSet();
        doReturn(caps).when(service).getCapabilities();
        Set<NetconfOperationService> services = Sets.newHashSet(service);
        doReturn(services).when(snap).getServices();
        doReturn(snap).when(factoriesListener).openSnapshot(anyString());

        return factoriesListener;
    }

    private static class VerifyingNotificationListener implements NotificationListener {
        public List<Notification> notifications = Lists.newArrayList();

        @Override
        public void handleNotification(Notification notification, Object handback) {
            this.notifications.add(notification);
        }

        void assertNotificationCount(Object size) {
            assertEquals(size, notifications.size());
        }

        void assertNotificationContent(int notificationIndex, int expectedModulesSize, int expectedServicesSize, int expectedCapsSize) {
            Notification notification = notifications.get(notificationIndex);
            assertEquals(CommitJMXNotification.class, notification.getClass());
            int capsSize = ((CommitJMXNotification) notification).getCapabilities().size();
            assertEquals("Expected capabilities count", expectedCapsSize, capsSize);
            Element configSnapshot = ((CommitJMXNotification) notification).getConfigSnapshot();
            int modulesSize = configSnapshot.getElementsByTagName("module").getLength();
            assertEquals("Expected modules count", expectedModulesSize, modulesSize);
            int servicesSize = configSnapshot.getElementsByTagName("instance").getLength();
            assertEquals("Expected services count", expectedServicesSize, servicesSize);
        }
    }

    private static class VerifyingPersister implements Persister {

        public List<ConfigSnapshotHolder> snapshots = Lists.newArrayList();
        private Persister mockedPersister;

        public VerifyingPersister() throws IOException {
            Persister mockedAggregator = mock(Persister.class);

            doAnswer(new Answer<Object>() {
                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable {
                    ConfigSnapshotHolder configSnapshot = (ConfigSnapshotHolder) invocation.getArguments()[0];
                    snapshots.add(configSnapshot);
                    return null;
                }
            }).when(mockedAggregator).persistConfig(any(ConfigSnapshotHolder.class));

            this.mockedPersister = mockedAggregator;
        }

        void assertSnapshotCount(Object size) {
            assertEquals(size, snapshots.size());
        }

        void assertSnapshotContent(int notificationIndex, int expectedModulesSize, int expectedServicesSize, int expectedCapsSize)
                throws SAXException, IOException {
            ConfigSnapshotHolder snapshot = snapshots.get(notificationIndex);
            int capsSize = snapshot.getCapabilities().size();
            assertEquals("Expected capabilities count", expectedCapsSize, capsSize);
            Document configSnapshot = readXmlToDocument(snapshot.getConfigSnapshot());
            assertElementsCount(configSnapshot, "module", expectedModulesSize);
            assertElementsCount(configSnapshot, "instance", expectedServicesSize);
        }

        @Override
        public void persistConfig(ConfigSnapshotHolder configSnapshotHolder) throws IOException {
            mockedPersister.persistConfig(configSnapshotHolder);
        }

        @Override
        public List<ConfigSnapshotHolder> loadLastConfigs() throws IOException {
            return mockedPersister.loadLastConfigs();
        }

        @Override
        public void close() {
            mockedPersister.close();
        }
    }
}
