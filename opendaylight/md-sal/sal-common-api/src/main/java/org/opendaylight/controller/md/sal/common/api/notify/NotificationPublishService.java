/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.notify;

import java.util.concurrent.ExecutorService;

/**
 * Interface for publishing YANG-modeled notifications.
 * <p>
 * Users of this interface can publish any YANG-modeled notification which will
 * be delivered to all subscribed listeners.
 * <p>
 * Preferred way of publishing of notifications is done by invoking {@link #publish(Object)}.
 *
 * <p>You may consider using {@link #publish(Object, ExecutorService)} if and only if
 * your use-case requires customized  execution policy or run-to-completion
 * inside process.
 *
 * <p>
 * The metadata required to deliver a notification to the correct listeners is
 * extracted from the published notification.
 *
 *
 * FIXME: Consider clarification of execution/delivery policy, how it will be
 * affected by Actor model and cluster-wide notifications.
 *
 * @param <N>
 *            the type of notifications
 */
public interface NotificationPublishService<N> {

    /**
     * Publishes a notification and notifies subscribed listeners. All listener
     * notifications are done via a default executor.
     * <p>
     * <b>Note:</b> This call will block when the default executor is saturated
     * and the notification queue for this executor is full.
     *
     * @param notification
     *            the notification to publish.
     */
    void publish(N notification);

    /**
     * Publishes a notification and notifies subscribed listeners. All listener
     * notifications are done via the provided executor.
     * <p>
     * <b>Note:</b> Use only if necessary. Consider using
     * {@link #publish(Object)} for most use-cases.
     *
     * <p>
     * By using this method you could customize execution policy of listeners present
     * inside process (e.g. using  single-threaded executor or even same-thread executor
     * delivery.
     *
     * <p>
     * This executor is used only for inside-process notification deliveries.
     *
     * @param notification
     *            the notification to publish.
     * @param executor
     *            the executor that will be used to deliver notifications to
     *            subscribed listeners.
     */
    void publish(N notification, ExecutorService executor);
}
