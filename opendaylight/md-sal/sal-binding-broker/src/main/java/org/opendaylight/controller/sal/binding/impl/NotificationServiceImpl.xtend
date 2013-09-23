/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.impl

import org.opendaylight.controller.sal.binding.api.NotificationService
import org.opendaylight.controller.sal.binding.api.NotificationListener
import org.opendaylight.yangtools.yang.binding.Notification
import com.google.common.collect.Multimap
import com.google.common.collect.HashMultimap

class NotificationServiceImpl implements NotificationService {
    val Multimap<Class<? extends Notification>, NotificationListener<?>> listeners;

    new() {
        listeners = HashMultimap.create()
    }

    override <T extends Notification> addNotificationListener(Class<T> notificationType,
        NotificationListener<T> listener) {
        listeners.put(notificationType, listener)
    }

    override <T extends Notification> removeNotificationListener(Class<T> notificationType,
        NotificationListener<T> listener) {
        listeners.remove(notificationType, listener)
    }

}
