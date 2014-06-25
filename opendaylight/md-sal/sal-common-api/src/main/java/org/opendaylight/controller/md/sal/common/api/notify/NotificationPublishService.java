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
 * Users of this interface can publish any YANG-modeled notification which will be delivered
 * to all subscribed listeners.
 * <p>
 * Publication of notifications is done by invoking
 * {@link #publish(Notification)} or
 * {@link #publish(Notification, ExecutorService)}.
 * <p>
 * The metadata required to deliver a notification to the correct listeners is extracted
 * from the published notification.
 *
 * @param <N> the type of notifications
 */
public interface NotificationPublishService<N> {

    /**
     * Publishes a notification and notifies subscribed listeners. All listener notifications are
     * done via a default executor.
     * <p>
     * <b>Note:</b> This call will block when the default executor is saturated
     * and the notification queue for this executor is full.
     *
     * <p>
     * FIXME:API Usability: Should this return ListenableFuture, which will complete once
     * all listeners received their callbacks?
     *
     * @param notification the notification to publish.
     */
    void publish(N notification);

    /**
     * Publishes a notification and notifies subscribed listeners. All listener notifications are done
     * via the provided executor.
     *
     * <p>
     * FIXME:API Usability: Should this return ListenableFuture, which will complete once
     * all listeners received their callbacks?
     *
     * @param notification the notification to publish.
     * @param executor the executor that will be used to deliver notifications to subscribed listeners.
     */
    void publish(N notification,ExecutorService executor);
}
