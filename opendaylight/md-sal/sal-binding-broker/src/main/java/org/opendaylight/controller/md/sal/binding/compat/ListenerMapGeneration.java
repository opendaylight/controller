/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.compat;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.opendaylight.yangtools.yang.binding.Notification;

import com.google.common.base.Predicate;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

/**
 * An immutable view of the current generation of listeners.
 */
final class ListenerMapGeneration {
    private static final int CACHE_MAX_ENTRIES = 1000;

    /**
     * Constant map of notification type to subscribed listeners.
     */
    private final Multimap<Class<? extends Notification>, NotificationListenerRegistration<?>> typeToListeners;

    /**
     * Dynamic cache of notification implementation to matching listeners. This cache loads entries based on
     * the contents of the {@link #typeToListeners} map.
     */
    private final LoadingCache<Class<?>, Iterable<NotificationListenerRegistration<?>>> implementationToListeners =
            CacheBuilder.newBuilder()
            .weakKeys()
            .maximumSize(CACHE_MAX_ENTRIES)
            .build(new CacheLoader<Class<?>, Iterable<NotificationListenerRegistration<?>>>() {
                @Override
                public Iterable<NotificationListenerRegistration<?>> load(final Class<?> key) {
                    final Set<NotificationListenerRegistration<?>> regs = new HashSet<>();

                    for (final Class<?> type : getNotificationTypes(key)) {
                        @SuppressWarnings("unchecked")
                        final Collection<NotificationListenerRegistration<?>> l = typeToListeners.get((Class<? extends Notification>) type);
                        if (l != null) {
                            regs.addAll(l);
                        }
                    }

                    return ImmutableSet.copyOf(regs);
                }
            });

    ListenerMapGeneration() {
        typeToListeners = ImmutableMultimap.of();
    }

    ListenerMapGeneration(final Multimap<Class<? extends Notification>, NotificationListenerRegistration<?>> listeners) {
        this.typeToListeners = ImmutableMultimap.copyOf(listeners);
    }

    /**
     * Current listeners. Exposed for creating the next generation.
     *
     * @return Current type-to-listener map.
     */
    Multimap<Class<? extends Notification>, NotificationListenerRegistration<?>> getListeners() {
        return typeToListeners;
    }

    /**
     * Look up the listeners which need to see this notification delivered.
     *
     * @param notification Notification object
     * @return Iterable of listeners, guaranteed to be nonnull.
     */
    public Iterable<NotificationListenerRegistration<?>> listenersFor(final Notification notification) {
        // Safe to use, as our loader does not throw checked exceptions
        return implementationToListeners.getUnchecked(notification.getClass());
    }

    public Iterable<Class<? extends Notification>> getKnownTypes() {
        return typeToListeners.keySet();
    }

    private static Iterable<Class<?>> getNotificationTypes(final Class<?> cls) {
        final Class<?>[] ifaces = cls.getInterfaces();
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
}