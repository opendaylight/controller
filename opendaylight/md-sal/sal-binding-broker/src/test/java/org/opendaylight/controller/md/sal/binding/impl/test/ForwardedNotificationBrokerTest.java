package org.opendaylight.controller.md.sal.binding.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javassist.ClassPool;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.md.sal.binding.impl.ForwardedNotificationPublishService;
import org.opendaylight.controller.md.sal.binding.impl.ForwardedNotificationService;
import org.opendaylight.controller.md.sal.dom.broker.impl.DOMNotificationRouter;
import org.opendaylight.controller.sal.binding.codegen.impl.SingletonHolder;
import org.opendaylight.controller.sal.binding.spi.NotificationInvokerFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.OpendaylightMdsalListTestListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.TwoLevelListChanged;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.TwoLevelListChangedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.list.rev140701.two.level.list.TopLevelListKey;
import org.opendaylight.yangtools.binding.data.codec.gen.impl.DataObjectSerializerGenerator;
import org.opendaylight.yangtools.binding.data.codec.gen.impl.StreamWriterGenerator;
import org.opendaylight.yangtools.binding.data.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.sal.binding.generator.util.JavassistUtils;

public class ForwardedNotificationBrokerTest {
    private NotificationPublishService forwardedNotificationPublishService;
    private NotificationService forwardedNotificationService;

    @Before
    public void setUp() {
        ClassPool pool = ClassPool.getDefault();
        DataObjectSerializerGenerator generator = StreamWriterGenerator.create(JavassistUtils.forClassPool(pool));
        BindingNormalizedNodeCodecRegistry codecRegistry = new BindingNormalizedNodeCodecRegistry(generator);
        final NotificationInvokerFactory invokerFactory = SingletonHolder.INVOKER_FACTORY;
        DOMNotificationRouter domNotificationRouter = DOMNotificationRouter.create(8);
        forwardedNotificationService = new ForwardedNotificationService(codecRegistry, domNotificationRouter, invokerFactory);
        forwardedNotificationPublishService = new ForwardedNotificationPublishService(codecRegistry, domNotificationRouter);
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
        final ListenerRegistration<TestNotifListener> listenerRegistration =
                forwardedNotificationService.registerNotificationListener(testNotifListener);
        forwardedNotificationPublishService.putNotification(testData);
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
        final ListenerRegistration<TestNotifListener> listenerRegistration =
                forwardedNotificationService.registerNotificationListener(testNotifListener);
        assertTrue(forwardedNotificationPublishService.offerNotification(testData));
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
        final ListenerRegistration<TestNotifListener> listenerRegistration =
                forwardedNotificationService.registerNotificationListener(testNotifListener);
        assertTrue(forwardedNotificationPublishService.offerNotification(testData, 5, TimeUnit.SECONDS));
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
