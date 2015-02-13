/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.netconf.it;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.opendaylight.controller.netconf.util.test.XmlUnitUtil.assertContainsElementWithName;
import static org.opendaylight.controller.netconf.util.test.XmlUnitUtil.assertElementsCount;
import static org.opendaylight.controller.netconf.util.xml.XmlUtil.readXmlToDocument;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import javax.management.InstanceNotFoundException;
import javax.management.Notification;
import javax.management.NotificationListener;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.config.persist.api.ConfigSnapshotHolder;
import org.opendaylight.controller.config.persist.api.Persister;
import org.opendaylight.controller.netconf.api.NetconfMessage;
import org.opendaylight.controller.netconf.api.jmx.CommitJMXNotification;
import org.opendaylight.controller.netconf.client.TestingNetconfClient;
import org.opendaylight.controller.netconf.impl.DefaultCommitNotificationProducer;
import org.opendaylight.controller.netconf.persist.impl.ConfigPersisterNotificationHandler;
import org.opendaylight.controller.netconf.util.test.XmlFileLoader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class NetconfConfigPersisterITTest extends AbstractNetconfConfigTest {

    public static final int PORT = 12026;
    private static final InetSocketAddress TCP_ADDRESS = new InetSocketAddress(LOOPBACK_ADDRESS, PORT);


    @Override
    protected SocketAddress getTcpServerAddress() {
        return TCP_ADDRESS;
    }

    @Override
    protected DefaultCommitNotificationProducer getNotificationProducer() {
        return new DefaultCommitNotificationProducer(ManagementFactory.getPlatformMBeanServer());
    }

    @Test
    public void testNetconfCommitNotifications() throws Exception {
        final VerifyingNotificationListener notificationVerifier = createCommitNotificationListener();
        final VerifyingPersister mockedAggregator = mockAggregator();

        try (TestingNetconfClient persisterClient = new TestingNetconfClient("persister", getClientDispatcher(), getClientConfiguration(TCP_ADDRESS, 4000))) {
            try (ConfigPersisterNotificationHandler configPersisterNotificationHandler = new ConfigPersisterNotificationHandler(
                    platformMBeanServer, mockedAggregator)) {

                try (TestingNetconfClient netconfClient = new TestingNetconfClient("client", getClientDispatcher(), getClientConfiguration(TCP_ADDRESS, 4000))) {
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
        notificationVerifier.assertNotificationContent(1, 4, 3, 9);

        mockedAggregator.assertSnapshotCount(2);
        // Capabilities are stripped for persister
        mockedAggregator.assertSnapshotContent(0, 0, 0, 1);
        mockedAggregator.assertSnapshotContent(1, 4, 3, 3);
    }

    private VerifyingPersister mockAggregator() throws IOException {
        return new VerifyingPersister();
    }

    private VerifyingNotificationListener createCommitNotificationListener() throws InstanceNotFoundException {
        final VerifyingNotificationListener listener = new VerifyingNotificationListener();
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

    private static class VerifyingNotificationListener implements NotificationListener {
        public List<Notification> notifications = Lists.newArrayList();

        @Override
        public void handleNotification(final Notification notification, final Object handback) {
            this.notifications.add(notification);
        }

        void assertNotificationCount(final Object size) {
            assertEquals(size, notifications.size());
        }

        void assertNotificationContent(final int notificationIndex, final int expectedModulesSize, final int expectedServicesSize, final int expectedCapsSize) {
            final Notification notification = notifications.get(notificationIndex);
            assertEquals(CommitJMXNotification.class, notification.getClass());
            final int capsSize = ((CommitJMXNotification) notification).getCapabilities().size();
            assertEquals("Expected capabilities count", expectedCapsSize, capsSize);
            final Element configSnapshot = ((CommitJMXNotification) notification).getConfigSnapshot();
            final int modulesSize = configSnapshot.getElementsByTagName("module").getLength();
            assertEquals("Expected modules count", expectedModulesSize, modulesSize);
            final int servicesSize = configSnapshot.getElementsByTagName("instance").getLength();
            assertEquals("Expected services count", expectedServicesSize, servicesSize);
        }
    }

    private static class VerifyingPersister implements Persister {

        public List<ConfigSnapshotHolder> snapshots = Lists.newArrayList();
        private Persister mockedPersister;

        public VerifyingPersister() throws IOException {
            final Persister mockedAggregator = mock(Persister.class);

            doAnswer(new Answer<Object>() {
                @Override
                public Object answer(final InvocationOnMock invocation) throws Throwable {
                    final ConfigSnapshotHolder configSnapshot = (ConfigSnapshotHolder) invocation.getArguments()[0];
                    snapshots.add(configSnapshot);
                    return null;
                }
            }).when(mockedAggregator).persistConfig(any(ConfigSnapshotHolder.class));

            this.mockedPersister = mockedAggregator;
        }

        void assertSnapshotCount(final Object size) {
            assertEquals(size, snapshots.size());
        }

        void assertSnapshotContent(final int notificationIndex, final int expectedModulesSize, final int expectedServicesSize, final int expectedCapsSize)
                throws SAXException, IOException {
            final ConfigSnapshotHolder snapshot = snapshots.get(notificationIndex);
            final int capsSize = snapshot.getCapabilities().size();
            assertEquals("Expected capabilities count", expectedCapsSize, capsSize);
            final Document configSnapshot = readXmlToDocument(snapshot.getConfigSnapshot());
            assertElementsCount(configSnapshot, "module", expectedModulesSize);
            assertElementsCount(configSnapshot, "instance", expectedServicesSize);
        }

        @Override
        public void persistConfig(final ConfigSnapshotHolder configSnapshotHolder) throws IOException {
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
