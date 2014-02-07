/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker.impl;

import java.util.Collection;

import org.opendaylight.controller.sal.core.api.notify.NotificationListener;
import org.opendaylight.controller.sal.dom.broker.spi.NotificationRouter;
import org.opendaylight.yangtools.concepts.AbstractObjectRegistration;
import org.opendaylight.yangtools.concepts.Registration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

public class NotificationRouterImpl implements NotificationRouter {
    private static Logger log = LoggerFactory.getLogger(NotificationRouterImpl.class);

    private final Multimap<QName, Registration<NotificationListener>> listeners = Multimaps.synchronizedSetMultimap(HashMultimap.<QName, Registration<NotificationListener>>create());
//    private Registration<NotificationListener> defaultListener;

    private void sendNotification(CompositeNode notification) {
        final QName type = notification.getNodeType();
        final Collection<Registration<NotificationListener>> toNotify = listeners.get(type);
        log.trace("Publishing notification " + type);

        if ((toNotify == null) || toNotify.isEmpty()) {
            log.debug("No listener registered for handling of notification {}", type);
            return;
        }

        for (Registration<NotificationListener> listener : toNotify) {
            try {
                // FIXME: ensure that notification is immutable
                listener.getInstance().onNotification(notification);
            } catch (Exception e) {
                log.error("Uncaught exception in NotificationListener", e);
            }
        }
    }

    @Override
    public void publish(CompositeNode notification) {
        sendNotification(notification);
    }

    @Override
    public Registration<NotificationListener> addNotificationListener(QName notification, NotificationListener listener) {
        ListenerRegistration ret = new ListenerRegistration(notification, listener);
        listeners.put(notification, ret);
        return ret;
    }

    private class ListenerRegistration extends AbstractObjectRegistration<NotificationListener> {

        final QName type;

        public ListenerRegistration(QName type, NotificationListener instance) {
            super(instance);
            this.type = type;
        }

        @Override
        protected void removeRegistration() {
            listeners.remove(type, this);
        }
    }
}
