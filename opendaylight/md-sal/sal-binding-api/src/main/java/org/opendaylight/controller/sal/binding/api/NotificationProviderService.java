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

    @Deprecated
    void notify(Notification notification);

    @Deprecated
    void notify(Notification notification, ExecutorService service);

    @Override
    void publish(Notification notification);

    @Override
    void publish(Notification notification, ExecutorService service);
    
    @Override
    public <T extends Notification> Registration<NotificationListener<T>> registerNotificationListener(
            Class<T> notificationType, NotificationListener<T> listener);
    
    @Override
    public Registration<org.opendaylight.yangtools.yang.binding.NotificationListener> registerNotificationListener(
            org.opendaylight.yangtools.yang.binding.NotificationListener listener);
}
