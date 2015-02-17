/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl.compat;

import java.util.concurrent.ExecutorService;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HeliumNotificationProviderServiceAdapter extends HeliumNotificationServiceAdapter implements NotificationProviderService, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(HeliumNotificationProviderServiceAdapter.class);

    private final NotificationPublishService notificationPublishService;

    public HeliumNotificationProviderServiceAdapter(NotificationPublishService notificationPublishService,
                                                 NotificationService notificationService) {
        super(notificationService);
        this.notificationPublishService = notificationPublishService;
    }

    @Override
    public void publish(final Notification notification) {
        try {
            notificationPublishService.putNotification(notification);
        } catch (InterruptedException e) {
            LOG.error("Notification publication was interupted: "  + e);
        }
    }

    @Override
    public void publish(final Notification notification, final ExecutorService executor) {
        try {
            notificationPublishService.putNotification(notification);
        } catch (InterruptedException e) {
            LOG.error("Notification publication was interupted: "  + e);
        }
    }

    @Override
    public ListenerRegistration<NotificationInterestListener> registerInterestListener(
            NotificationInterestListener interestListener) {
        throw new UnsupportedOperationException("InterestListener is not supported.");
    }

    @Override
    public void close() throws Exception {

    }

}
