/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.api;

import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.binding.Notification;

public interface NotificationService extends BindingAwareService {
    /**
     * Register a generic listener for specified notification type only.
     *
     * @param notificationType
     * @param listener
     * @return Registration for listener. To unregister listener invoke {@link Registration#close()} method.
     */
    <T extends Notification> Registration<NotificationListener<T>> registerNotificationListener(
            Class<T> notificationType, NotificationListener<T> listener);

    /**
     * Register a listener which implements generated notification interfaces derived from
     * {@link org.opendaylight.yangtools.yang.binding.NotificationListener}.
     * Listener is registered for all notifications present in implemented interfaces.
     *
     * @param listener
     * @return Registration for listener. To unregister listener invoke {@link Registration#close()} method.
     */
    Registration<org.opendaylight.yangtools.yang.binding.NotificationListener> registerNotificationListener(
            org.opendaylight.yangtools.yang.binding.NotificationListener listener);
}
