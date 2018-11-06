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
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.md.sal.dom.api.DOMNotification;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationListener;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.controller.md.sal.dom.spi.DOMNotificationSubscriptionListener;
import org.opendaylight.controller.md.sal.dom.spi.DOMNotificationSubscriptionListenerRegistry;
import org.opendaylight.controller.sal.core.compat.LegacyDOMNotificationServiceAdapter;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
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
public final class DOMNotificationRouter extends LegacyDOMNotificationServiceAdapter implements AutoCloseable,
        DOMNotificationPublishService, DOMNotificationSubscriptionListenerRegistry {

    private final org.opendaylight.mdsal.dom.api.DOMNotificationPublishService delegateNotificationPublishService;
    private final org.opendaylight.mdsal.dom.spi.DOMNotificationSubscriptionListenerRegistry delegateListenerRegistry;

    private DOMNotificationRouter(
            final org.opendaylight.mdsal.dom.api.DOMNotificationService delegateNotificationService,
            final org.opendaylight.mdsal.dom.api.DOMNotificationPublishService delegateNotificationPublishService,
            final org.opendaylight.mdsal.dom.spi.DOMNotificationSubscriptionListenerRegistry delegateListenerRegistry) {
        super(delegateNotificationService);
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
        return super.registerNotificationListener(listener, types);
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
}
