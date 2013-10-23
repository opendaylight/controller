/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.api;

import java.util.concurrent.ExecutorService;

import org.opendaylight.controller.md.sal.common.api.notify.NotificationPublishService;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.Notification;

public interface NotificationProviderService extends NotificationService, NotificationPublishService<Notification> {

    /**
     * Deprecated. Use {@link #publish(Notification)}.
     * 
     * @param notification
     */
    @Deprecated
    void notify(Notification notification);

    /**
     * Deprecated. Use {@link #publish(Notification,ExecutorService)}.
     * 
     * @param notification
     */
    @Deprecated
    void notify(Notification notification, ExecutorService service);

    /**
     * Publishes a notification.
     * 
     * @param Notification notification to publish.
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
}
