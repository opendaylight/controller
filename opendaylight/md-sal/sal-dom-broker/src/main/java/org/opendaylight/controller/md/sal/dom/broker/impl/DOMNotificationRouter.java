/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import com.google.common.util.concurrent.ListenableFuture;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.md.sal.dom.api.DOMEvent;
import org.opendaylight.controller.md.sal.dom.api.DOMNotification;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationListener;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.controller.md.sal.dom.spi.DOMNotificationSubscriptionListener;
import org.opendaylight.controller.md.sal.dom.spi.DOMNotificationSubscriptionListenerRegistry;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

/**
 * Joint implementation of {@link DOMNotificationPublishService} and {@link DOMNotificationService}. Provides
 * routing of notifications from publishers to subscribers.
 *
 * <p>
 * Internal implementation works by allocating a two-handler Disruptor. The first handler delivers notifications
 * to subscribed listeners and the second one notifies whoever may be listening on the returned future. Registration
 * state tracking is performed by a simple immutable multimap -- when a registration or unregistration occurs we
 * re-generate the entire map from scratch and set it atomically. While registrations/unregistrations synchronize
 * on this instance, notifications do not take any locks here.
 *
 * <p>
 * The fully-blocking {@link #offerNotification(DOMNotification)}
 * is realized using the Disruptor's native operations. The bounded-blocking
 * {@link #offerNotification(DOMNotification, long, TimeUnit)}
 * is realized by arming a background wakeup interrupt.
 */
@SuppressFBWarnings(value = "NP_NONNULL_PARAM_VIOLATION", justification = "Void is the only allowed value")
public final class DOMNotificationRouter implements AutoCloseable, DOMNotificationPublishService,
        DOMNotificationService, DOMNotificationSubscriptionListenerRegistry {

    private final org.opendaylight.mdsal.dom.api.DOMNotificationService delegateNotificationService;
    private final org.opendaylight.mdsal.dom.api.DOMNotificationPublishService delegateNotificationPublishService;
    private final org.opendaylight.mdsal.dom.spi.DOMNotificationSubscriptionListenerRegistry delegateListenerRegistry;

    private DOMNotificationRouter(
            org.opendaylight.mdsal.dom.api.DOMNotificationService delegateNotificationService,
            org.opendaylight.mdsal.dom.api.DOMNotificationPublishService delegateNotificationPublishService,
            org.opendaylight.mdsal.dom.spi.DOMNotificationSubscriptionListenerRegistry delegateListenerRegistry) {
        this.delegateNotificationService = delegateNotificationService;
        this.delegateNotificationPublishService = delegateNotificationPublishService;
        this.delegateListenerRegistry = delegateListenerRegistry;
    }

    public static DOMNotificationRouter create(final int queueDepth) {
        final org.opendaylight.mdsal.dom.broker.DOMNotificationRouter delegate =
                org.opendaylight.mdsal.dom.broker.DOMNotificationRouter.create(queueDepth);
        return create(delegate, delegate, delegate);
    }

    public static DOMNotificationRouter create(final int queueDepth, final long spinTime, final long parkTime,
                                               final TimeUnit unit) {
        final org.opendaylight.mdsal.dom.broker.DOMNotificationRouter delegate =
                org.opendaylight.mdsal.dom.broker.DOMNotificationRouter.create(queueDepth, spinTime, parkTime, unit);
        return create(delegate, delegate, delegate);
    }

    public static DOMNotificationRouter create(
            final org.opendaylight.mdsal.dom.api.DOMNotificationService delegateNotificationService,
            final org.opendaylight.mdsal.dom.api.DOMNotificationPublishService delegateNotificationPublishService,
            final org.opendaylight.mdsal.dom.spi.DOMNotificationSubscriptionListenerRegistry delegateListenerRegistry) {
        return new DOMNotificationRouter(delegateNotificationService, delegateNotificationPublishService,
                delegateListenerRegistry);
    }

    @Override
    public synchronized <T extends DOMNotificationListener> ListenerRegistration<T> registerNotificationListener(
            final T listener, final Collection<SchemaPath> types) {
        org.opendaylight.mdsal.dom.api.DOMNotificationListener delegateListener = notification -> {
            if (notification instanceof DOMNotification) {
                listener.onNotification((DOMNotification)notification);
                return;
            }

            if (notification instanceof org.opendaylight.mdsal.dom.api.DOMEvent) {
                listener.onNotification(new DefaultDOMEvent(notification,
                        (org.opendaylight.mdsal.dom.api.DOMEvent)notification));
                return;
            }

            listener.onNotification(new DefaultDOMNotification(notification));
        };

        final ListenerRegistration<org.opendaylight.mdsal.dom.api.DOMNotificationListener> reg =
                delegateNotificationService.registerNotificationListener(delegateListener, types);

        return new AbstractListenerRegistration<T>(listener) {
            @Override
            protected void removeRegistration() {
                reg.close();
            }
        };
    }

    @Override
    public <T extends DOMNotificationListener> ListenerRegistration<T> registerNotificationListener(final T listener,
            final SchemaPath... types) {
        return registerNotificationListener(listener, Arrays.asList(types));
    }

    @Override
    public <L extends DOMNotificationSubscriptionListener> ListenerRegistration<L> registerSubscriptionListener(
            final L listener) {
        return delegateListenerRegistry.registerSubscriptionListener(listener);
    }

    @Override
    public ListenableFuture<?> putNotification(final DOMNotification notification) throws InterruptedException {
        return delegateNotificationPublishService.putNotification(notification);
    }

    @Override
    public ListenableFuture<?> offerNotification(final DOMNotification notification) {
        return delegateNotificationPublishService.offerNotification(notification);
    }

    @Override
    public ListenableFuture<?> offerNotification(final DOMNotification notification, final long timeout,
                                                 final TimeUnit unit) throws InterruptedException {
        return delegateNotificationPublishService.offerNotification(notification, timeout, unit);
    }

    @Override
    public void close() {
    }

    private static class DefaultDOMNotification implements DOMNotification {
        private final SchemaPath schemaPath;
        private final ContainerNode body;

        DefaultDOMNotification(org.opendaylight.mdsal.dom.api.DOMNotification from) {
            this.schemaPath = from.getType();
            this.body = from.getBody();
        }

        @Override
        public SchemaPath getType() {
            return schemaPath;
        }

        @Override
        public ContainerNode getBody() {
            return body;
        }
    }

    private static class DefaultDOMEvent extends DefaultDOMNotification implements DOMEvent {
        private final Date eventTime;

        DefaultDOMEvent(org.opendaylight.mdsal.dom.api.DOMNotification fromNotification,
                org.opendaylight.mdsal.dom.api.DOMEvent fromEvent) {
            super(fromNotification);
            final Instant eventInstant = fromEvent.getEventInstant();
            this.eventTime = eventInstant != null ? Date.from(eventInstant) : null;
        }

        @Override
        public Date getEventTime() {
            return eventTime;
        }
    }
}
