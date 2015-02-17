package org.opendaylight.controller.md.sal.binding.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.md.sal.binding.impl.BackwardsCompatibleNotificationBroker;
import org.opendaylight.controller.md.sal.binding.test.AbstractNotificationBrokerTest;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.OpendaylightMdsalListTestListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.TwoLevelListChanged;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.TwoLevelListChangedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.binding.NotificationListener;

public class BackwardsCompatibleNotificationBrokerTest extends AbstractNotificationBrokerTest {

    private NotificationProviderService notificationProviderService;

    @Before
    public void initTest() {
        final NotificationService notificationService = getNotificationService();
        final NotificationPublishService notificationPublishService = getNotificationPublishService();
        notificationProviderService = new BackwardsCompatibleNotificationBroker(notificationPublishService, notificationService,
                getDomNotificationRouter(), getBindingToNormalizedNodeCodec().getCodecRegistry());
    }

    private TwoLevelListChanged createTestData() {
        final TwoLevelListChangedBuilder tb = new TwoLevelListChangedBuilder();
        tb.setTopLevelList(ImmutableList.of(new TopLevelListBuilder().setKey(new TopLevelListKey("test")).build()));
        return tb.build();
    }

    @Test
    public void testNotifSubscriptionForwarded() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final TwoLevelListChanged testData = createTestData();

        final NotifTestListener testNotifListener = new NotifTestListener(latch);
        final ListenerRegistration<NotificationListener> listenerRegistration =
                notificationProviderService.registerNotificationListener(testNotifListener);
        notificationProviderService.publish(testData);

        latch.await();
        assertTrue(testNotifListener.getReceivedNotifications().size() == 1);
        assertEquals(testData, testNotifListener.getReceivedNotifications().get(0));
        listenerRegistration.close();
    }

    @Test
    public void testNotifSubscriptionGeneric() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final TwoLevelListChanged testData = createTestData();

        final NotifTestGenericListener<TwoLevelListChanged> testNotifListener = new NotifTestGenericListener<>(latch);
        final ListenerRegistration<org.opendaylight.controller.sal.binding.api.NotificationListener<TwoLevelListChanged>>
                listenerRegistration = notificationProviderService.registerNotificationListener(TwoLevelListChanged.class,
                testNotifListener);
        notificationProviderService.publish(testData);

        latch.await();
        assertTrue(testNotifListener.getReceivedNotifications().size() == 1);
        assertEquals(testData, testNotifListener.getReceivedNotifications().get(0));

        listenerRegistration.close();
    }

    @Test
    public void testNotifSubscriptionAnnounceListener() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        final NotifInterestTestListener notifInterestTestListener =
                new NotifInterestTestListener(latch);
        final ListenerRegistration<NotificationProviderService.NotificationInterestListener>
                interestRegistration = notificationProviderService.registerInterestListener(notifInterestTestListener);
        final NotifTestListener testNotifListener = new NotifTestListener(null);
        final ListenerRegistration<NotificationListener> listenerRegistration =
                notificationProviderService.registerNotificationListener(testNotifListener);

        latch.await();
        assertTrue(notifInterestTestListener.getReceivedNotifications().size() == 1);
        assertEquals(TwoLevelListChanged.class, notifInterestTestListener.getReceivedNotifications().get(0));

        listenerRegistration.close();
        interestRegistration.close();
    }

    @Test
    public void testNotifSubscriptionAnnounceGenericListener() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        final NotifInterestTestListener notifInterestTestListener =
                new NotifInterestTestListener(latch);
        final ListenerRegistration<NotificationProviderService.NotificationInterestListener>
                interestRegistration = notificationProviderService.registerInterestListener(notifInterestTestListener);
        final NotifTestListener testNotifListener = new NotifTestListener(null);
        final ListenerRegistration<NotificationListener> listenerRegistration =
                notificationProviderService.registerNotificationListener(testNotifListener);

        latch.await();
        assertTrue(notifInterestTestListener.getReceivedNotifications().size() == 1);
        assertEquals(TwoLevelListChanged.class, notifInterestTestListener.getReceivedNotifications().get(0));

        listenerRegistration.close();
        interestRegistration.close();
    }

    private static class NotifTestListener implements OpendaylightMdsalListTestListener {
        private List<TwoLevelListChanged> receivedNotifications = new ArrayList<>();
        private CountDownLatch latch;

        public NotifTestListener(CountDownLatch latch) {
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

    private static class NotifTestGenericListener<T extends Notification>
            implements org.opendaylight.controller.sal.binding.api.NotificationListener<T> {

        private List<T> receivedNotifications = new ArrayList<>();
        private CountDownLatch latch;

        public NotifTestGenericListener(final CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onNotification(final T notification) {
            receivedNotifications.add(notification);
            latch.countDown();
        }

        public List<T> getReceivedNotifications() {
            return receivedNotifications;
        }
    }

    private static class NotifInterestTestListener implements NotificationProviderService.NotificationInterestListener {

        private final List<Class<? extends Notification>> receivedNotifications = new ArrayList<>();
        private final CountDownLatch latch;

        public NotifInterestTestListener(final CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onNotificationSubscribtion(final Class<? extends Notification> notificationType) {
            receivedNotifications.add(notificationType);
            latch.countDown();
        }

        public List<Class<? extends Notification>> getReceivedNotifications() {
            return receivedNotifications;
        }
    }

}
