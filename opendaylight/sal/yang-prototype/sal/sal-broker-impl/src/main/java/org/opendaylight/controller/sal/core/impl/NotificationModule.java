/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.opendaylight.controller.sal.core.api.BrokerService;
import org.opendaylight.controller.sal.core.api.Broker.ConsumerSession;
import org.opendaylight.controller.sal.core.api.Broker.ProviderSession;
import org.opendaylight.controller.sal.core.api.Consumer.ConsumerFunctionality;
import org.opendaylight.controller.sal.core.api.Provider.ProviderFunctionality;
import org.opendaylight.controller.sal.core.api.notify.NotificationListener;
import org.opendaylight.controller.sal.core.api.notify.NotificationProviderService;
import org.opendaylight.controller.sal.core.api.notify.NotificationService;
import org.opendaylight.controller.sal.core.spi.BrokerModule;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.data.api.CompositeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

public class NotificationModule implements BrokerModule {
    private static Logger log = LoggerFactory
            .getLogger(NotificationModule.class);

    private Multimap<QName, NotificationListener> listeners = HashMultimap
            .create();

    private static final Set<Class<? extends BrokerService>> PROVIDED_SERVICE_TYPE = ImmutableSet
            .of((Class<? extends BrokerService>) NotificationService.class,
                    NotificationProviderService.class);

    private static final Set<Class<? extends ConsumerFunctionality>> SUPPORTED_CONSUMER_FUNCTIONALITY = ImmutableSet
            .of((Class<? extends ConsumerFunctionality>) NotificationListener.class,
                    NotificationListener.class); // Workaround: if we use the
                                                 // version of method with only
                                                 // one argument, the generics
                                                 // inference will not work

    @Override
    public Set<Class<? extends BrokerService>> getProvidedServices() {
        return PROVIDED_SERVICE_TYPE;
    }

    @Override
    public Set<Class<? extends ConsumerFunctionality>> getSupportedConsumerFunctionality() {
        return SUPPORTED_CONSUMER_FUNCTIONALITY;
    }

    @Override
    public <T extends BrokerService> T getServiceForSession(Class<T> service,
            ConsumerSession session) {
        if (NotificationProviderService.class.equals(service)
                && session instanceof ProviderSession) {
            @SuppressWarnings("unchecked")
            T ret = (T) newNotificationProviderService(session);
            return ret;
        } else if (NotificationService.class.equals(service)) {

            @SuppressWarnings("unchecked")
            T ret = (T) newNotificationConsumerService(session);
            return ret;
        }

        throw new IllegalArgumentException(
                "The requested session-specific service is not provided by this module.");
    }

    private void sendNotification(CompositeNode notification) {
        QName type = notification.getNodeType();
        Collection<NotificationListener> toNotify = listeners.get(type);
        log.info("Publishing notification " + type);

        if (toNotify == null) {
            // No listeners were registered - returns.
            return;
        }

        for (NotificationListener listener : toNotify) {
            try {
                // FIXME: ensure that notification is immutable
                listener.onNotification(notification);
            } catch (Exception e) {
                log.error("Uncaught exception in NotificationListener", e);
            }
        }

    }

    private NotificationService newNotificationConsumerService(
            ConsumerSession session) {
        return new NotificationConsumerSessionImpl();
    }

    private NotificationProviderService newNotificationProviderService(
            ConsumerSession session) {
        return new NotificationProviderSessionImpl();
    }

    private class NotificationConsumerSessionImpl implements
            NotificationService {

        private Multimap<QName, NotificationListener> consumerListeners = HashMultimap
                .create();
        private boolean closed = false;

        @Override
        public void addNotificationListener(QName notification,
                NotificationListener listener) {
            checkSessionState();
            if (notification == null) {
                throw new IllegalArgumentException(
                        "Notification type must not be null.");
            }
            if (listener == null) {
                throw new IllegalArgumentException("Listener must not be null.");
            }

            consumerListeners.put(notification, listener);
            listeners.put(notification, listener);
            log.info("Registered listener for notification: " + notification);
        }

        @Override
        public void removeNotificationListener(QName notification,
                NotificationListener listener) {
            checkSessionState();
            if (notification == null) {
                throw new IllegalArgumentException(
                        "Notification type must not be null.");
            }
            if (listener == null) {
                throw new IllegalArgumentException("Listener must not be null.");
            }
            consumerListeners.remove(notification, listener);
            listeners.remove(notification, listener);
        }

        @Override
        public void closeSession() {
            closed = true;
            Map<QName, Collection<NotificationListener>> toRemove = consumerListeners
                    .asMap();
            for (Entry<QName, Collection<NotificationListener>> entry : toRemove
                    .entrySet()) {
                listeners.remove(entry.getKey(), entry.getValue());
            }
        }

        protected void checkSessionState() {
            if (closed)
                throw new IllegalStateException("Session is closed");
        }
    }

    private class NotificationProviderSessionImpl extends
            NotificationConsumerSessionImpl implements
            NotificationProviderService {

        @Override
        public void sendNotification(CompositeNode notification) {
            checkSessionState();
            if (notification == null)
                throw new IllegalArgumentException(
                        "Notification must not be null.");
            NotificationModule.this.sendNotification(notification);
        }
    }

    @Override
    public Set<Class<? extends ProviderFunctionality>> getSupportedProviderFunctionality() {
        return Collections.emptySet();
    }
}
