/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.impl

import org.opendaylight.controller.sal.binding.api.NotificationProviderService
import org.opendaylight.yangtools.yang.binding.Notification
import com.google.common.collect.Multimap
import org.opendaylight.controller.sal.binding.api.NotificationListener
import com.google.common.collect.HashMultimap
import java.util.concurrent.ExecutorService
import java.util.Collection

class NotificationBrokerImpl implements NotificationProviderService {

    val Multimap<Class<? extends Notification>, NotificationListener<?>> listeners;
    val ExecutorService executor;

    new(ExecutorService executor) {
        listeners = HashMultimap.create()
        this.executor = executor;
    }

    override <T extends Notification> addNotificationListener(Class<T> notificationType,
        NotificationListener<T> listener) {
        listeners.put(notificationType, listener)
    }

    override <T extends Notification> removeNotificationListener(Class<T> notificationType,
        NotificationListener<T> listener) {
        listeners.remove(notificationType, listener)
    }

    override notify(Notification notification) {
        notification.notificationTypes.forEach [
            listeners.get(it as Class<? extends Notification>)?.notifyAll(notification)
        ]
    }

    def getNotificationTypes(Notification notification) {
        notification.class.interfaces.filter[it != Notification && Notification.isAssignableFrom(it)]
    }

    def notifyAll(Collection<NotificationListener<?>> listeners, Notification notification) {
        listeners.forEach[(it as NotificationListener).onNotification(notification)]
    }
}
