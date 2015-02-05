/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.test.sal.binding.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.api.NotificationService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.bi.ba.notification.rev150205.OpendaylightTestNotificationListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.bi.ba.notification.rev150205.OutOfPixieDustNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.bi.ba.notification.rev150205.OutOfPixieDustNotificationBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * covers registering of notification listener, publishing of notification and receiving of notification.
 */
public class NotificationTest extends AbstractTest {
    
    private static final Logger LOG = LoggerFactory
            .getLogger(NotificationTest.class);

    protected final NotificationTestListener listener1 = new NotificationTestListener();
    protected final NotificationTestListener listener2 = new NotificationTestListener();

    protected ListenerRegistration<NotificationListener> listener1Reg;
    protected ListenerRegistration<NotificationListener> listener2Reg;

    protected NotificationProviderService notifyProviderService;

    /**
     * test of delivering of notification
     * @throws Exception
     */
    @Test
    public void notificationTest() throws Exception {
        LOG.info("The registration of the Provider 1.");
        AbstractTestProvider provider1 = new AbstractTestProvider() {
            @Override
            public void onSessionInitiated(ProviderContext session) {
                notifyProviderService = session.getSALService(NotificationProviderService.class);
            }
        };

        // registerProvider method calls onSessionInitiated method above
        broker.registerProvider(provider1);
        assertNotNull(notifyProviderService);

        LOG.info("The registration of the Consumer 1. It retrieves Notification Service "
                + "from MD-SAL and registers OpendaylightTestNotificationListener as notification listener");
        BindingAwareConsumer consumer1 = new BindingAwareConsumer() {
            @Override
            public void onSessionInitialized(ConsumerContext session) {
                NotificationService notificationService = session.getSALService(NotificationService.class);
                assertNotNull(notificationService);
                listener1Reg = notificationService.registerNotificationListener(listener1);
            }
        };
        // registerConsumer method calls onSessionInitialized method above
        broker.registerConsumer(consumer1);

        assertNotNull(listener1Reg);

        LOG.info("The notification of type FlowAdded with cookie ID 0 is created. The "
                + "delay 100ms to make sure that the notification was delivered to "
                + "listener.");
        notifyProviderService.publish(noDustNotification("rainy day", 42));
        Thread.sleep(100);

        /**
         * Check that one notification was delivered and has correct cookie.
         *
         */
        assertEquals(1, listener1.notificationBag.size());
        assertEquals("rainy day", listener1.notificationBag.get(0).getReason());
        assertEquals(42, listener1.notificationBag.get(0).getDaysTillNewDust().intValue());

        LOG.info("The registration of the Consumer 2. SalFlowListener is registered "
                + "registered as notification listener.");
        BindingAwareProvider provider = new BindingAwareProvider() {

            @Override
            public void onSessionInitiated(ProviderContext session) {
                listener2Reg = session.getSALService(NotificationProviderService.class).registerNotificationListener(
                        listener2);
            }
        };

        // registerConsumer method calls onSessionInitialized method above
        broker.registerProvider(provider);

        LOG.info("3 notifications are published");
        notifyProviderService.publish(noDustNotification("rainy day", 5));
        notifyProviderService.publish(noDustNotification("rainy day", 10));
        notifyProviderService.publish(noDustNotification("tax collector", 2));

        /**
         * The delay 100ms to make sure that the notifications were delivered to
         * listeners.
         */
        Thread.sleep(100);

        /**
         * Check that 3 notification was delivered to both listeners (first one
         * received 4 in total, second 3 in total).
         *
         */
        assertEquals(4, listener1.notificationBag.size());
        assertEquals(3, listener2.notificationBag.size());

        /**
         * The second listener is closed (unregistered)
         *
         */
        listener2Reg.close();

        LOG.info("The notification 5 is published");
        notifyProviderService.publish(noDustNotification("entomologist hunt", 10));

        /**
         * The delay 100ms to make sure that the notification was delivered to
         * listener.
         */
        Thread.sleep(100);

        /**
         * Check that first consumer received 5 notifications in total, second
         * consumer received only three. Last notification was never received by
         * second consumer because its listener was unregistered.
         *
         */
        assertEquals(5, listener1.notificationBag.size());
        assertEquals(3, listener2.notificationBag.size());

    }

    /**
     * Creates instance of the type OutOfPixieDustNotification. It is
     * used only for testing purpose.
     *
     * @param reason
     * @param days
     * @return instance of the type OutOfPixieDustNotification
     */
    public static OutOfPixieDustNotification noDustNotification(String reason, int days) {
        OutOfPixieDustNotificationBuilder ret = new OutOfPixieDustNotificationBuilder();
        ret.setReason(reason).setDaysTillNewDust(days);
        return ret.build();
    }

    /**
     *
     * Implements
     * {@link OpendaylightTestNotificationListener} and contains attributes which keep lists of objects of
     * the type {@link OutOfFairyDustNotification}.
     */
    public static class NotificationTestListener implements OpendaylightTestNotificationListener {

        List<OutOfPixieDustNotification> notificationBag = new ArrayList<>();

        @Override
        public void onOutOfPixieDustNotification(OutOfPixieDustNotification arg0) {
            notificationBag.add(arg0);
        }

    }
}
