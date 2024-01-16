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
import javax.inject.Inject;
import org.junit.Test;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.bi.ba.notification.rev150205.OutOfPixieDustNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.bi.ba.notification.rev150205.OutOfPixieDustNotificationBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.ops4j.pax.exam.util.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * covers registering of notification listener, publishing of notification and receiving of notification.
 */
public class NotificationIT extends AbstractIT {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationIT.class);

    @Inject
    @Filter(timeout = 120 * 1000)
    NotificationService notificationService;

    @Inject
    @Filter(timeout = 120 * 1000)
    NotificationPublishService notificationPublishService;

    /**
     * Test of delivering of notification.
     */
    @Test
    public void notificationTest() throws Exception {
        final var bag1 = new ArrayList<OutOfPixieDustNotification>();
        try (var reg1 = notificationService.registerListener(OutOfPixieDustNotification.class, bag1::add)) {
            LOG.info("""
                The notification of type FlowAdded with cookie ID 0 is created. The\s\
                delay 100ms to make sure that the notification was delivered to\s\
                listener.""");
            notificationPublishService.putNotification(noDustNotification("rainy day", 42));
            Thread.sleep(100);

            // Check that one notification was delivered and has correct cookie.
            assertEquals(1, bag1.size());
            assertEquals("rainy day", bag1.get(0).getReason());
            assertEquals(42, bag1.get(0).getDaysTillNewDust().intValue());

            LOG.info("The registration of the Consumer 2. SalFlowListener is registered "
                + "registered as notification listener.");

            final var bag2 = new ArrayList<OutOfPixieDustNotification>();
            try (var reg2 = notificationService.registerListener(OutOfPixieDustNotification.class, bag2::add)) {
                LOG.info("3 notifications are published");
                notificationPublishService.putNotification(noDustNotification("rainy day", 5));
                notificationPublishService.putNotification(noDustNotification("rainy day", 10));
                notificationPublishService.putNotification(noDustNotification("tax collector", 2));

                // The delay 100ms to make sure that the notifications were delivered to listeners.
                Thread.sleep(100);

                // Check that 3 notification was delivered to both listeners (first one  received 4 in total, second 3
                // in total).
                assertEquals(4, bag1.size());
                assertEquals(3, bag2.size());

                // The second listener is closed (unregistered)
                reg2.close();

                LOG.info("The notification 5 is published");
                notificationPublishService.putNotification(noDustNotification("entomologist hunt", 10));

                // The delay 100ms to make sure that the notification was delivered to listener.
                Thread.sleep(100);

                // Check that first consumer received 5 notifications in total, second  consumer received only three.
                // Last notification was never received by second consumer because its listener was unregistered.
                assertEquals(5, bag1.size());
                assertEquals(3, bag2.size());
            }
        }
    }

    /**
     * Creates instance of the type OutOfPixieDustNotification. It is used only for testing purpose.
     *
     * @return instance of the type OutOfPixieDustNotification
     */
    public static OutOfPixieDustNotification noDustNotification(final String reason, final int days) {
        OutOfPixieDustNotificationBuilder ret = new OutOfPixieDustNotificationBuilder();
        ret.setReason(reason).setDaysTillNewDust(Uint16.valueOf(days));
        return ret.build();
    }
}
