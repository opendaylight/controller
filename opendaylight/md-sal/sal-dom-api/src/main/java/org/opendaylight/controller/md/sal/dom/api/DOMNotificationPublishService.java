/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.api;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

/**
 * A {@link DOMService} which allows its user to send {@link DOMNotification}s. It
 * two styles of initiating the notification delivery, similar to {@link java.util.concurrent.BlockingQueue}:
 * a put-style method which waits until the implementation can accept the notification
 * for delivery, and an offer-style method, which attempts to enqueue the notification,
 * but allows the caller to specify that it should never wait, or put an upper bound
 * on how long it is going to wait.
 */
public interface DOMNotificationPublishService extends DOMService {
    /**
     * Well-known value indicating that the implementation is currently not
     * able to accept a notification.
     */
    public static final ListenableFuture<Object> WOULD_BLOCK = Futures.immediateFailedFuture(new Throwable("Unacceptable blocking conditions encountered"));

    /**
     * Publish a notification. The result of this method is a {@link ListenableFuture}
     * which will complete once the notification has been delivered to all immediate
     * registrants. The type of the object resulting from the future is not defined
     * and implementations may use it to convey additional information related to the
     * publishing process. Implementations of this method may block indefinitely.
     *
     * @param notification Notification to be published.
     * @return A listenable future which will report completion when the service
     *         has finished propagating the notification to its immediate registrants.
     * @throws InterruptedException if interrupted while waiting
     * @throws NullPointerException if notification is null.
     */
    @Nonnull ListenableFuture<? extends Object> putNotification(@Nonnull DOMNotification notification) throws InterruptedException;

    /**
     * Attempt to publish a notification. The result of this method is a {@link ListenableFuture}
     * which will complete once the notification has been delivered to all immediate
     * registrants. The type of the object resulting from the future is not defined
     * and implementations may use it to convey additional information related to the
     * publishing process. Unlike {@link #putNotification(DOMNotification)}, this method
     * is guaranteed to not block if the underlying implementation encounters contention.
     *
     * @param notification Notification to be published.
     * @return A listenable future which will report completion when the service
     *         has finished propagating the notification to its immediate registrants,
     *         or {@value #WOULD_BLOCK} if resource constraints prevent
     *         the implementation from accepting the notification for delivery.
     * @throws NullPointerException if notification is null.
     */
    @Nonnull ListenableFuture<? extends Object> offerNotification(@Nonnull DOMNotification notification);

    /**
     * Attempt to publish a notification. The result of this method is a {@link ListenableFuture}
     * which will complete once the notification has been delivered to all immediate
     * registrants. The type of the object resulting from the future is not defined
     * and implementations may use it to convey additional information related to the
     * publishing process. Unlike {@link #putNotification(DOMNotification)}, this method
     * is guaranteed to not block if the underlying implementation encounters contention
     * which cannot be resolved within specified timeout.
     *
     * @param notification Notification to be published.
     * @param timeout how long to wait before giving up, in units of unit
     * @param unit a TimeUnit determining how to interpret the timeout parameter
     * @return A listenable future which will report completion when the service
     *         has finished propagating the notification to its immediate registrants,
     *         or {@value #WOULD_BLOCK} if resource constraints prevent
     *         the implementation from accepting the notification for delivery.
     * @throws InterruptedException if interrupted while waiting
     * @throws NullPointerException if notification or unit is null.
     * @throws IllegalArgumentException if timeout is negative.
     */
    @Nonnull ListenableFuture<? extends Object> offerNotification(@Nonnull DOMNotification notification,
        @Nonnegative long timeout, @Nonnull TimeUnit unit) throws InterruptedException;
}
