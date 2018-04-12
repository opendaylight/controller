/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.test.sal.binding.it;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.junit.Test;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.bi.ba.notification.rev150205.OpendaylightTestNotificationListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.bi.ba.notification.rev150205.OutOfPixieDustNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.bi.ba.notification.rev150205.OutOfPixieDustNotificationBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.ops4j.pax.exam.util.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * covers registering of notification listener, publishing of notification and receiving of notification.
 */
public class NotificationIT extends AbstractIT {

    private static final Logger LOG = LoggerFactory
            .getLogger(NotificationIT.class);

    @Inject
    @Filter(timeout = 120 * 1000)
    NotificationProviderService notificationService;

    /**
     * test of delivering of notification
     * @throws Exception
     */
    @Test
    public void notificationTest() throws Exception {
        NotificationTestListener listener1 = new NotificationTestListener();
        ListenerRegistration<NotificationListener> listener1Reg =
                notificationService.registerNotificationListener(listener1);

        LOG.info("The notification of type FlowAdded with cookie ID 0 is created. The "
                + "delay 100ms to make sure that the notification was delivered to "
                + "listener.");
        notificationService.publish(noDustNotification("rainy day", 42));
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

        NotificationTestListener listener2 = new NotificationTestListener();
        final ListenerRegistration<NotificationListener> listener2Reg =
                notificationService.registerNotificationListener(listener2);

        LOG.info("3 notifications are published");
        notificationService.publish(noDustNotification("rainy day", 5));
        notificationService.publish(noDustNotification("rainy day", 10));
        notificationService.publish(noDustNotification("tax collector", 2));

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
        notificationService.publish(noDustNotification("entomologist hunt", 10));

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
