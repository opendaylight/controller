/**
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.impl;

import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.opendaylight.controller.sal.binding.api.NotificationListener;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.controller.sal.binding.codegen.impl.SingletonHolder;
import org.opendaylight.controller.sal.binding.spi.NotificationInvokerFactory.NotificationInvoker;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.concepts.util.ListenerRegistry;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class NotificationBrokerImpl implements NotificationProviderService, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationBrokerImpl.class);

    private final ListenerRegistry<NotificationInterestListener> interestListeners =
            ListenerRegistry.create();
    private final NotificationListenerMap listeners = new NotificationListenerMap();
    private final ExecutorService executor;

    public NotificationBrokerImpl(final ExecutorService executor) {
        this.executor = Preconditions.checkNotNull(executor);
    }

    @Override
    public void publish(final Notification notification) {
        publish(notification, executor);
    }

    @Override
    public void publish(final Notification notification, final ExecutorService service) {
        for (NotificationListenerRegistration<?> r : listeners.listenersFor(notification)) {
            service.submit(new NotifyTask(r, notification));
        }
    }

    private final void addRegistrations(final NotificationListenerRegistration<?>... registrations) {
        listeners.addRegistrations(registrations);
        for (NotificationListenerRegistration<?> reg : registrations) {
            announceNotificationSubscription(reg.getType());
        }
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
    public ListenerRegistration<NotificationInterestListener> registerInterestListener(final NotificationInterestListener interestListener) {
        final ListenerRegistration<NotificationInterestListener> registration = this.interestListeners.register(interestListener);
        for (final Class<? extends Notification> notification : listeners.getKnownTypes()) {
            interestListener.onNotificationSubscribtion(notification);
        }
        return registration;
    }

    @Override
    public <T extends Notification> NotificationListenerRegistration<T> registerNotificationListener(final Class<T> notificationType, final NotificationListener<T> listener) {
        final NotificationListenerRegistration<T> reg = new AbstractNotificationListenerRegistration<T>(notificationType, listener) {
            @Override
            protected void removeRegistration() {
                listeners.removeRegistrations(this);
            }
        };

        addRegistrations(reg);
        return reg;
    }

    @Override
    public ListenerRegistration<org.opendaylight.yangtools.yang.binding.NotificationListener> registerNotificationListener(final org.opendaylight.yangtools.yang.binding.NotificationListener listener) {
        final NotificationInvoker invoker = SingletonHolder.INVOKER_FACTORY.invokerFor(listener);
        final Set<Class<? extends Notification>> types = invoker.getSupportedNotifications();
        final NotificationListenerRegistration<?>[] regs = new NotificationListenerRegistration<?>[types.size()];

        // Populate the registrations...
        int i = 0;
        for (Class<? extends Notification> type : types) {
            regs[i] = new AggregatedNotificationListenerRegistration<Notification, Object>(type, invoker.getInvocationProxy(), regs) {
                @Override
                protected void removeRegistration() {
                    // Nothing to do, will be cleaned up by parent (below)
                }
            };
            ++i;
        }

        // ... now put them to use ...
        addRegistrations(regs);

        // ... finally return the parent registration
        return new AbstractListenerRegistration<org.opendaylight.yangtools.yang.binding.NotificationListener>(listener) {
            @Override
            protected void removeRegistration() {
                listeners.removeRegistrations(regs);
                for (ListenerRegistration<?> reg : regs) {
                    reg.close();
                }
            }
        };
    }

    @Override
    public void close() {
    }

}
