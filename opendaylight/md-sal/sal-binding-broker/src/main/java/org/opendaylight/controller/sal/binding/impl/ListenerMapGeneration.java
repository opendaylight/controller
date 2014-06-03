/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
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
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

/**
 * An immutable view of the current generation of listeners.
 */
final class ListenerMapGeneration {
    private final Multimap<Class<? extends Notification>, NotificationListenerRegistration<?>> listeners;

    ListenerMapGeneration() {
        listeners = ImmutableMultimap.of();
    }

    ListenerMapGeneration(final Multimap<Class<? extends Notification>, NotificationListenerRegistration<?>> listeners) {
        this.listeners = ImmutableMultimap.copyOf(listeners);
    }

    /**
     * Current listeners. Exposed for creating the next generation.
     *
     * @return Current type-to-listener map.
     */
    Multimap<Class<? extends Notification>, NotificationListenerRegistration<?>> getListeners() {
        return listeners;
    }

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

    /**
     * Look up the listeners which need to see this notification delivered.
     *
     * @param notification Notification object
     * @return Iterable of listeners, may be null
     *
     * FIXME: improve such that it always returns non-null.
     */
    public Iterable<NotificationListenerRegistration<?>> listenersFor(final Notification notification) {
        final Set<NotificationListenerRegistration<?>> ret = new HashSet<>();

        for (final Class<?> type : getNotificationTypes(notification)) {
            final Collection<NotificationListenerRegistration<?>> l = listeners.get((Class<? extends Notification>) type);
            if (l != null) {
                ret.addAll(l);
            }
        }

        return ret;
    }
}