/**
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.opendaylight.controller.sal.binding.api.NotificationListener;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.codegen.impl.SingletonHolder;
import org.opendaylight.controller.sal.binding.spi.NotificationInvokerFactory.NotificationInvoker;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.concepts.util.ListenerRegistry;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

public class NotificationBrokerImpl implements NotificationProviderService, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationBrokerImpl.class);

    private final ListenerRegistry<NotificationInterestListener> interestListeners =
            ListenerRegistry.create();

    private final Multimap<Class<? extends Notification>, NotificationListener<?>> listeners =
            Multimaps.synchronizedSetMultimap(HashMultimap.<Class<? extends Notification>, NotificationListener<?>>create());
    private ExecutorService executor;

    @Deprecated
    public NotificationBrokerImpl(final ExecutorService executor) {
        this.setExecutor(executor);
    }

    public void setExecutor(final ExecutorService executor) {
        this.executor = Preconditions.checkNotNull(executor);
    }

    public Iterable<Class<?>> getNotificationTypes(final Notification notification) {
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

    @Override
    public void publish(final Notification notification) {
        this.publish(notification, executor);
    }

    @Override
    public void publish(final Notification notification, final ExecutorService service) {
        Iterable<NotificationListener<?>> listenerToNotify = Collections.emptySet();
        for (final Class<?> type : getNotificationTypes(notification)) {
            listenerToNotify = Iterables.concat(listenerToNotify, listeners.get(((Class<? extends Notification>) type)));
        }

        final Set<NotifyTask> tasks = new HashSet<>();
        for (NotificationListener<?> l : listenerToNotify) {
            tasks.add(new NotifyTask(l, notification));
        }

        for (final NotifyTask task : tasks) {
            service.submit(task);
        }
    }

    @Override
    public <T extends Notification> Registration<NotificationListener<T>> registerNotificationListener(final Class<T> notificationType, final NotificationListener<T> listener) {
        final GenericNotificationRegistration<T> reg = new GenericNotificationRegistration<T>(notificationType, listener, this);
        this.listeners.put(notificationType, listener);
        this.announceNotificationSubscription(notificationType);
        return reg;
    }

    private void announceNotificationSubscription(final Class<? extends Notification> notification) {
        for (final ListenerRegistration<NotificationInterestListener> listener : interestListeners) {
            try {
                listener.getInstance().onNotificationSubscribtion(notification);
            } catch (Exception e) {
                LOG.warn("Listener {} reported unexpected error on notification {}",
                        listener.getInstance(), notification, e);
            }
        }
    }

    @Override
    public Registration<org.opendaylight.yangtools.yang.binding.NotificationListener> registerNotificationListener(final org.opendaylight.yangtools.yang.binding.NotificationListener listener) {
        final NotificationInvoker invoker = SingletonHolder.INVOKER_FACTORY.invokerFor(listener);
        for (final Class<? extends Notification> notifyType : invoker.getSupportedNotifications()) {
            listeners.put(notifyType, invoker.getInvocationProxy());
            announceNotificationSubscription(notifyType);
        }

        return new GeneratedListenerRegistration(listener, invoker, this);
    }

    protected boolean unregisterListener(final GenericNotificationRegistration<?> reg) {
        return listeners.remove(reg.getType(), reg.getInstance());
    }

    protected void unregisterListener(final GeneratedListenerRegistration reg) {
        final NotificationInvoker invoker = reg.getInvoker();
        for (final Class<? extends Notification> notifyType : invoker.getSupportedNotifications()) {
            this.listeners.remove(notifyType, invoker.getInvocationProxy());
        }
    }

    @Override
    public void close() {
    }

    @Override
    public ListenerRegistration<NotificationInterestListener> registerInterestListener(final NotificationInterestListener interestListener) {
        final ListenerRegistration<NotificationInterestListener> registration = this.interestListeners.register(interestListener);
        for (final Class<? extends Notification> notification : listeners.keySet()) {
            interestListener.onNotificationSubscribtion(notification);
        }
        return registration;
    }
}
