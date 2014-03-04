/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.api;

import java.util.EventListener;
import java.util.concurrent.ExecutorService;

import org.opendaylight.controller.md.sal.common.api.notify.NotificationPublishService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.Notification;

public interface NotificationProviderService extends NotificationService, NotificationPublishService<Notification> {
    /**
     * Publishes a notification.
     *
     * @param Notification
     *            notification to publish.
     *
     */
    @Override
    void publish(Notification notification);

    /**
     * Publishes a notification, listener calls are done in provided executor.
     *
     */
    @Override
    void publish(Notification notification, ExecutorService service);

    ListenerRegistration<NotificationInterestListener> registerInterestListener(
            NotificationInterestListener interestListener);

    public interface NotificationInterestListener extends EventListener {

        void onNotificationSubscribtion(Class<? extends Notification> notificationType);
    }
}
