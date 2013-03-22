/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.impl.notify;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import org.opendaylight.controller.sal.core.impl.BrokerServiceImpl;
import org.opendaylight.controller.sal.core.impl.Utils;
import org.opendaylight.controller.sal.core.spi.BrokerModule;
import org.opendaylight.controller.yang.common.QName;
import org.opendaylight.controller.yang.data.api.CompositeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class NotificationModule implements BrokerModule {
    private static Logger log = LoggerFactory
            .getLogger(NotificationModule.class);

    private Map<QName, List<NotificationListener>> listeners = new HashMap<QName, List<NotificationListener>>();

    @Override
    public Set<Class<? extends BrokerService>> getProvidedServices() {
        // FIXME Refactor
        Set<Class<? extends BrokerService>> ret = new HashSet<Class<? extends BrokerService>>();
        ret.add(NotificationService.class);
        ret.add(NotificationProviderService.class);
        return ret;
    }

    @Override
    public Set<Class<? extends ConsumerFunctionality>> getSupportedConsumerFunctionality() {
        // FIXME Refactor
        Set<Class<? extends ConsumerFunctionality>> ret = new HashSet<Class<? extends ConsumerFunctionality>>();
        ret.add(NotificationListener.class);
        return ret;
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
        List<NotificationListener> toNotify = listeners.get(type);
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

    private class NotificationConsumerSessionImpl extends BrokerServiceImpl
            implements NotificationService {

        Map<QName, List<NotificationListener>> consumerListeners = new HashMap<QName, List<NotificationListener>>();
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

            Utils.addToMap(consumerListeners, notification, listener);
            Utils.addToMap(listeners, notification, listener);
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
            Utils.removeFromMap(consumerListeners, notification, listener);
            Utils.removeFromMap(listeners, notification, listener);
        }

        @Override
        public void closeSession() {
            closed = true;
            Set<Entry<QName, List<NotificationListener>>> toRemove = consumerListeners
                    .entrySet();
            for (Entry<QName, List<NotificationListener>> entry : toRemove) {
                listeners.get(entry.getKey()).removeAll(entry.getValue());
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

