/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.test.AbstractNotificationBrokerTest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.OpendaylightMdsalListTestListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.TwoLevelListChanged;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.TwoLevelListChangedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ForwardedNotificationAdapterTest extends AbstractNotificationBrokerTest {

    private static final Logger LOG = LoggerFactory.getLogger(ForwardedNotificationAdapterTest.class);

    @Override
    protected Iterable<YangModuleInfo> getModuleInfos() throws Exception {
        return ImmutableSet.of(BindingReflections.getModuleInfo(TwoLevelListChanged.class));

    }

    private TwoLevelListChanged createTestData() {
        final TwoLevelListChangedBuilder tb = new TwoLevelListChangedBuilder();
        tb.setTopLevelList(ImmutableList.of(new TopLevelListBuilder().setKey(new TopLevelListKey("test")).build()));
        return tb.build();
    }

    @Test
    public void testNotifSubscription() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final TwoLevelListChanged testData = createTestData();

        final TestNotifListener testNotifListener = new TestNotifListener(latch);
        final ListenerRegistration<TestNotifListener> listenerRegistration = getNotificationService()
                .registerNotificationListener(testNotifListener);
        getNotificationPublishService().putNotification(testData);

        latch.await();
        assertTrue(testNotifListener.getReceivedNotifications().size() == 1);
        assertEquals(testData, testNotifListener.getReceivedNotifications().get(0));

        listenerRegistration.close();
    }

    @Test
    public void testNotifSubscription2() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final TwoLevelListChanged testData = createTestData();

        final TestNotifListener testNotifListener = new TestNotifListener(latch);
        final ListenerRegistration<TestNotifListener> listenerRegistration = getNotificationService()
                .registerNotificationListener(testNotifListener);
        try {
            getNotificationPublishService().offerNotification(testData).get(1, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException e) {
            LOG.error("Notification delivery failed", e);
            Assert.fail("notification should be delivered");
        }

        latch.await();
        assertTrue(testNotifListener.getReceivedNotifications().size() == 1);
        assertEquals(testData, testNotifListener.getReceivedNotifications().get(0));

        listenerRegistration.close();
    }

    @Test
    public void testNotifSubscription3() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final TwoLevelListChanged testData = createTestData();

        final TestNotifListener testNotifListener = new TestNotifListener(latch);
        final ListenerRegistration<TestNotifListener> listenerRegistration = getNotificationService()
                .registerNotificationListener(testNotifListener);
        assertNotSame(NotificationPublishService.REJECTED,
                getNotificationPublishService().offerNotification(testData, 5, TimeUnit.SECONDS));

        latch.await();
        assertTrue(testNotifListener.getReceivedNotifications().size() == 1);
        assertEquals(testData, testNotifListener.getReceivedNotifications().get(0));

        listenerRegistration.close();
    }

    private static class TestNotifListener implements OpendaylightMdsalListTestListener {
        private List<TwoLevelListChanged> receivedNotifications = new ArrayList<>();
        private CountDownLatch latch;

        public TestNotifListener(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onTwoLevelListChanged(TwoLevelListChanged notification) {
            receivedNotifications.add(notification);
            latch.countDown();
        }

        public List<TwoLevelListChanged> getReceivedNotifications() {
            return receivedNotifications;
        }
    }
}
