/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.api;

import org.opendaylight.controller.md.sal.common.api.notify.NotificationSubscriptionService;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.Notification;

public interface NotificationService extends BindingAwareService {

    @Deprecated
    <T extends Notification> void addNotificationListener(Class<T> notificationType, NotificationListener<T> listener);

    @Deprecated
    void addNotificationListener(org.opendaylight.yangtools.yang.binding.NotificationListener listener);

    @Deprecated
    void removeNotificationListener(org.opendaylight.yangtools.yang.binding.NotificationListener listener);

    @Deprecated
    <T extends Notification> void removeNotificationListener(Class<T> notificationType, NotificationListener<T> listener);

    <T extends Notification> Registration<NotificationListener<T>> registerNotificationListener(
            Class<T> notificationType, NotificationListener<T> listener);


    Registration<org.opendaylight.yangtools.yang.binding.NotificationListener> registerNotificationListener(
            org.opendaylight.yangtools.yang.binding.NotificationListener listener);
}
