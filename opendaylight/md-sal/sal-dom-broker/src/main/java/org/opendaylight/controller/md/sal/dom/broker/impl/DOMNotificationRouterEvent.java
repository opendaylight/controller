/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.lmax.disruptor.EventFactory;
import java.util.Collection;
import org.opendaylight.controller.md.sal.dom.api.DOMNotification;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationListener;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A single notification event in the disruptor ringbuffer. These objects are reused,
 * so they do have mutable state.
 */
final class DOMNotificationRouterEvent {
    private static final Logger LOG = LoggerFactory.getLogger(DOMNotificationRouterEvent.class);
    public static final EventFactory<DOMNotificationRouterEvent> FACTORY = new EventFactory<DOMNotificationRouterEvent>() {
        @Override
        public DOMNotificationRouterEvent newInstance() {
            return new DOMNotificationRouterEvent();
        }
    };

    private Collection<ListenerRegistration<? extends DOMNotificationListener>> subscribers;
    private DOMNotification notification;
    private SettableFuture<Void> future;

    private DOMNotificationRouterEvent() {
        // Hidden on purpose, initialized in initialize()
    }

    ListenableFuture<Void> initialize(final DOMNotification notification, final Collection<ListenerRegistration<? extends DOMNotificationListener>> subscribers) {
        this.notification = Preconditions.checkNotNull(notification);
        this.subscribers = Preconditions.checkNotNull(subscribers);
        this.future = SettableFuture.create();
        return this.future;
    }

    void deliverNotification() {
        LOG.trace("Start delivery of notification {}", notification);
        for (ListenerRegistration<? extends DOMNotificationListener> r : subscribers) {
            final DOMNotificationListener listener = r.getInstance();
            if (listener != null) {
                try {
                    LOG.trace("Notifying listener {}", listener);
                    listener.onNotification(notification);
                    LOG.trace("Listener notification completed");
                } catch (Exception e) {
                    LOG.error("Delivery of notification {} caused an error in listener {}", notification, listener, e);
                }
            }
        }
        LOG.trace("Delivery completed");
    }

    void setFuture() {
        future.set(null);
        notification = null;
        subscribers = null;
        future = null;
    }
}