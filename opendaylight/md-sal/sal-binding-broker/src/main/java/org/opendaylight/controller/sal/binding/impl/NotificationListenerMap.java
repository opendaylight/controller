/**
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.opendaylight.yangtools.yang.binding.Notification;

import com.google.common.base.Predicate;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

final class NotificationListenerMap {
    private final Multimap<Class<? extends Notification>, NotificationListenerRegistration<?>> listeners =
            Multimaps.synchronizedSetMultimap(HashMultimap.<Class<? extends Notification>, NotificationListenerRegistration<?>>create());

    private static Iterable<Class<?>> getNotificationTypes(final Notification notification) {
        final Class<?>[] ifaces = notification.getClass().getInterfaces();
        return Iterables.filter(Arrays.asList(ifaces), new Predicate<Class<?>>() {
            @Override
            public boolean apply(final Class<?> input) {
                if (Notification.class.equals(input)) {
                    return false;
                }
                return Notification.class.isAssignableFrom(input);
            }
        });
    }

    Iterable<NotificationListenerRegistration<?>> listenersFor(final Notification notification) {
        final Set<NotificationListenerRegistration<?>> toNotify = new HashSet<>();

        for (final Class<?> type : getNotificationTypes(notification)) {
            final Collection<NotificationListenerRegistration<?>> l = listeners.get((Class<? extends Notification>) type);
            if (l != null) {
                toNotify.addAll(l);
            }
        }

        return toNotify;
    }

    Iterable<Class<? extends Notification>> getKnownTypes() {
        return ImmutableList.copyOf(listeners.keySet());
    }

    void addRegistrations(final NotificationListenerRegistration<?>... registrations) {
        for (NotificationListenerRegistration<?> reg : registrations) {
            listeners.put(reg.getType(), reg);
        }
    }

    void removeRegistrations(final NotificationListenerRegistration<?>... registrations) {
        for (NotificationListenerRegistration<?> reg : registrations) {
            listeners.remove(reg.getType(), reg);
        }
    }

}
