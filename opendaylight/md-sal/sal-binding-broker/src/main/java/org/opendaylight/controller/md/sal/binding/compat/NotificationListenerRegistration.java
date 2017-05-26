/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.compat;

import org.opendaylight.controller.sal.binding.api.NotificationListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.Notification;

/**
 * A registration of a {@link NotificationListener}. Allows query of the type
 * of the notification and dispatching the notification atomically with regard
 * to unregistration.
 *
 * @param <T> Type of notification
 */
interface NotificationListenerRegistration<T extends Notification> extends ListenerRegistration<NotificationListener<T>> {
    /**
     * Return the interface class of the notification type.
     *
     * @return Notification type.
     */
    Class<? extends Notification> getType();

    /**
     * Dispatch a notification to the listener.
     *
     * @param notification Notification to be dispatched
     */
    void notify(Notification notification);
}
