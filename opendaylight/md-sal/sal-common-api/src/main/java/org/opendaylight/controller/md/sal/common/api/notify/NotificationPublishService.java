/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.common.api.notify;

import java.util.concurrent.ExecutorService;

public interface NotificationPublishService<N> {


    /**
     * Publishes a notification, listener callbacks are done via default executor.
     *
     * <p>
     * Published notification which will be delivered using default executor
     *
     * <b>Note:</b> This call is blocking when default executor is saturated,
     * and notification queue for this executor is full.
     *
     * <p>
     * FIXME:API Usability: Should this return ListenableFuture, which will complete once
     * all listeners received their callbacks?
     *
     * @param notification Notification to be published.
     *
     */
    void publish(N notification);

    /**
     * Publishes a notification, listener callbacks are done via provided executor.
     *
     * <p>
     * FIXME:API Usability: Should this return ListenableFuture, which will complete once
     * all listeners received their callbacks?
     *
     * @param notification Notification to be published
     * @param executor Executor which will be used to deliver callbacks for listener
     *   inside same JVM
     *
     */
    void publish(N notification,ExecutorService executor);
}
