/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.impl;

import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.concurrent.GuardedBy;

import org.opendaylight.yangtools.yang.binding.Notification;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * A multi-generation, isolated notification type to listener map.
 */
final class GenerationalListenerMap {
    private final AtomicReference<ListenerMapGeneration> current = new AtomicReference<>(new ListenerMapGeneration());

    Iterable<NotificationListenerRegistration<?>> listenersFor(final Notification notification) {
        return current.get().listenersFor(notification);
    }

    Iterable<Class<? extends Notification>> getKnownTypes() {
        // Note: this relies on current having immutable listeners
        return current.get().getListeners().keySet();
    }

    @GuardedBy("this")
    private Multimap<Class<? extends Notification>, NotificationListenerRegistration<?>> mutableListeners() {
        return HashMultimap.create(current.get().getListeners());
    }

    synchronized void addRegistrations(final NotificationListenerRegistration<?>... registrations) {
        Multimap<Class<? extends Notification>, NotificationListenerRegistration<?>> listeners =
                mutableListeners();

        for (NotificationListenerRegistration<?> reg : registrations) {
            listeners.put(reg.getType(), reg);
        }

        current.set(new ListenerMapGeneration(listeners));
    }

    synchronized void removeRegistrations(final NotificationListenerRegistration<?>... registrations) {
        Multimap<Class<? extends Notification>, NotificationListenerRegistration<?>> listeners =
                mutableListeners();

        for (NotificationListenerRegistration<?> reg : registrations) {
            listeners.remove(reg.getType(), reg);
        }

        current.set(new ListenerMapGeneration(listeners));
    }
}
