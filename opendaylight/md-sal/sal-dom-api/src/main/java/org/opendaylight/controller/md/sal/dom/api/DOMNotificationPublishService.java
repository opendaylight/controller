/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.api;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A {@link DOMService} which allows its user to send {@link DOMNotification}s.
 */
public interface DOMNotificationPublishService extends DOMService {
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
     * @throws NullPointerException if notification is null.
     */
    @Nonnull ListenableFuture<? extends Object> putNotification(@Nonnull DOMNotification notification);

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
     *         or null if resource constraints prevent notification delivery from being
     *         initiated.
     * @throws NullPointerException if notification is null.
     */
    @Nullable ListenableFuture<? extends Object> offerNotification(@Nonnull DOMNotification notification);

    /**
     * Attempt to publish a notification. The result of this method is a {@link ListenableFuture}
     * which will complete once the notification has been delivered to all immediate
     * registrants. The type of the object resulting from the future is not defined
     * and implementations may use it to convey additional information related to the
     * publishing process. Unlike {@link #putNotification(DOMNotification)}, this method
     * is guaranteed to not block if the underlying implementation encounters contention
     * which cannot be resolved within specified timeout..
     *
     * @param notification Notification to be published.
     * @param timeout how long to wait before giving up, in units of unit
     * @param unit a TimeUnit determining how to interpret the timeout parameter
     * @return A listenable future which will report completion when the service
     *         has finished propagating the notification to its immediate registrants,
     *         or null if resource constraints prevent notification delivery from being
     *         initiated.
     * @throws NullPointerException if notification or unit is null.
     * @throws IllegalArgumentException if timeout is negative.
     */
    @Nullable ListenableFuture<? extends Object> offerNotification(@Nonnull DOMNotification notification,
        @Nonnegative long timeout, @Nonnull TimeUnit unit);
}
