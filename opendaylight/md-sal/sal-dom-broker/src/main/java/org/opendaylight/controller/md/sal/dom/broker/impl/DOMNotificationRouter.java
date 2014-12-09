/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.InsufficientCapacityException;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.md.sal.dom.api.DOMNotification;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationListener;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationService;
import org.opendaylight.yangtools.concepts.AbstractListenerRegistration;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

/**
 * Joint implementation of {@link DOMNotificationPublishService} and {@link DOMNotificationService}. Provides
 * routing of notifications from publishers to subscribers.
 *
 * Internal implementation works by allocating a two-handler Disruptor. The first handler delivers notifications
 * to subscribed listeners and the second one notifies whoever may be listening on the returned future. Registration
 * state tracking is performed by a simple immutable multimap -- when a registration or unregistration occurs we
 * re-generate the entire map from scratch and set it atomically. While registrations/unregistrations synchronize
 * on this instance, notifications do not take any locks here.
 *
 * The fully-blocking {@link #publish(long, DOMNotification, Collection)} and non-blocking {@link #offerNotification(DOMNotification)}
 * are realized using the Disruptor's native operations. The bounded-blocking {@link #offerNotification(DOMNotification, long, TimeUnit)}
 * is realized by arming a background wakeup interrupt.
 */
public final class DOMNotificationRouter implements AutoCloseable, DOMNotificationPublishService, DOMNotificationService {
    private static final ListenableFuture<Void> NO_LISTENERS = Futures.immediateFuture(null);
    private static final WaitStrategy DEFAULT_STRATEGY = new SleepingWaitStrategy();
    private static final EventHandler<DOMNotificationRouterEvent> DISPATCH_NOTIFICATIONS = new EventHandler<DOMNotificationRouterEvent>() {
        @Override
        public void onEvent(final DOMNotificationRouterEvent event, final long sequence, final boolean endOfBatch) throws Exception {
            event.deliverNotification();

        }
    };
    private static final EventHandler<DOMNotificationRouterEvent> NOTIFY_FUTURE = new EventHandler<DOMNotificationRouterEvent>() {
        @Override
        public void onEvent(final DOMNotificationRouterEvent event, final long sequence, final boolean endOfBatch) {
            event.setFuture();
        }
    };

    private final Disruptor<DOMNotificationRouterEvent> disruptor;
    private final ExecutorService executor;
    private volatile Multimap<SchemaPath, ListenerRegistration<? extends DOMNotificationListener>> listeners = ImmutableMultimap.of();

    private DOMNotificationRouter(final ExecutorService executor, final Disruptor<DOMNotificationRouterEvent> disruptor) {
        this.executor = Preconditions.checkNotNull(executor);
        this.disruptor = Preconditions.checkNotNull(disruptor);
    }

    @SuppressWarnings("unchecked")
    public static DOMNotificationRouter create(final int queueDepth) {
        final ExecutorService executor = Executors.newCachedThreadPool();
        final Disruptor<DOMNotificationRouterEvent> disruptor = new Disruptor<>(DOMNotificationRouterEvent.FACTORY, queueDepth, executor, ProducerType.MULTI, DEFAULT_STRATEGY);

        disruptor.after(DISPATCH_NOTIFICATIONS).handleEventsWith(NOTIFY_FUTURE);
        disruptor.start();

        return new DOMNotificationRouter(executor, disruptor);
    }

    @Override
    public synchronized <T extends DOMNotificationListener> ListenerRegistration<T> registerNotificationListener(final T listener, final Collection<SchemaPath> types) {
        final ListenerRegistration<T> reg = new AbstractListenerRegistration<T>(listener) {
            @Override
            protected void removeRegistration() {
                final ListenerRegistration<T> me = this;

                synchronized (DOMNotificationRouter.this) {
                    listeners = ImmutableMultimap.copyOf(Multimaps.filterValues(listeners, new Predicate<ListenerRegistration<? extends DOMNotificationListener>>() {
                        @Override
                        public boolean apply(final ListenerRegistration<? extends DOMNotificationListener> input) {
                            return input != me;
                        }
                    }));
                }
            }
        };

        if (!types.isEmpty()) {
            final Builder<SchemaPath, ListenerRegistration<? extends DOMNotificationListener>> b = ImmutableMultimap.builder();
            b.putAll(listeners);

            for (SchemaPath t : types) {
                b.put(t, reg);
            }

            listeners = b.build();
        }

        return reg;
    }

    @Override
    public <T extends DOMNotificationListener> ListenerRegistration<T> registerNotificationListener(final T listener, final SchemaPath... types) {
        return registerNotificationListener(listener, Arrays.asList(types));
    }

    private ListenableFuture<Void> publish(final long seq, final DOMNotification notification, final Collection<ListenerRegistration<? extends DOMNotificationListener>> subscribers) {
        final DOMNotificationRouterEvent event = disruptor.get(seq);
        final ListenableFuture<Void> future = event.initialize(notification, subscribers);
        disruptor.getRingBuffer().publish(seq);
        return future;
    }

    @Override
    public ListenableFuture<? extends Object> putNotification(final DOMNotification notification) throws InterruptedException {
        final Collection<ListenerRegistration<? extends DOMNotificationListener>> subscribers = listeners.get(notification.getType());
        if (subscribers.isEmpty()) {
            return NO_LISTENERS;
        }

        final long seq = disruptor.getRingBuffer().next();
        return publish(seq, notification, subscribers);
    }

    private ListenableFuture<? extends Object> tryPublish(final DOMNotification notification, final Collection<ListenerRegistration<? extends DOMNotificationListener>> subscribers) {
        final long seq;
        try {
             seq = disruptor.getRingBuffer().tryNext();
        } catch (InsufficientCapacityException e) {
            return DOMNotificationPublishService.REJECTED;
        }

        return publish(seq, notification, subscribers);
    }

    @Override
    public ListenableFuture<? extends Object> offerNotification(final DOMNotification notification) {
        final Collection<ListenerRegistration<? extends DOMNotificationListener>> subscribers = listeners.get(notification.getType());
        if (subscribers.isEmpty()) {
            return NO_LISTENERS;
        }

        return tryPublish(notification, subscribers);
    }

    @Override
    public ListenableFuture<? extends Object> offerNotification(final DOMNotification notification, final long timeout,
            final TimeUnit unit) throws InterruptedException {
        final Collection<ListenerRegistration<? extends DOMNotificationListener>> subscribers = listeners.get(notification.getType());
        if (subscribers.isEmpty()) {
            return NO_LISTENERS;
        }

        // Attempt to perform a non-blocking publish first
        final ListenableFuture<? extends Object> noBlock = tryPublish(notification, subscribers);
        if (!DOMNotificationPublishService.REJECTED.equals(noBlock)) {
            return noBlock;
        }

        /*
         * FIXME: we need a background thread, which will watch out for blocking too long. Here
         *        we will arm a tasklet for it and synchronize delivery of interrupt properly.
         */
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void close() {
        disruptor.shutdown();
        executor.shutdown();
    }
}
