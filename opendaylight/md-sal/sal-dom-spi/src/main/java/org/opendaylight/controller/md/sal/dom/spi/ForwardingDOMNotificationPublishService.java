/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.spi;

import com.google.common.collect.ForwardingObject;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.md.sal.dom.api.DOMNotification;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService;

/**
 * Utility implementations of {@link DOMNotificationPublishService} which forwards
 * all requests to a delegate instance.
 */
public abstract class ForwardingDOMNotificationPublishService extends ForwardingObject implements DOMNotificationPublishService {
    @Override
    protected abstract DOMNotificationPublishService delegate();

    @Override
    public ListenableFuture<?> putNotification(final DOMNotification notification) throws InterruptedException {
        return delegate().putNotification(notification);
    }

    @Override
    public ListenableFuture<?> offerNotification(final DOMNotification notification) {
        return delegate().offerNotification(notification);
    }

    @Override
    public ListenableFuture<?> offerNotification(final DOMNotification notification, final long timeout,
            final TimeUnit unit) throws InterruptedException {
        return delegate().offerNotification(notification, timeout, unit);
    }
}
