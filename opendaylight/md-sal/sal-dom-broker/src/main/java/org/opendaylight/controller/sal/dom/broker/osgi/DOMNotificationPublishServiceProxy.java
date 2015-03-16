/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.dom.broker.osgi;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.dom.api.DOMNotification;
import org.opendaylight.controller.md.sal.dom.api.DOMNotificationPublishService;
import org.opendaylight.controller.sal.core.api.notify.NotificationListener;
import org.opendaylight.controller.sal.core.api.notify.NotificationPublishService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.CompositeNode;
import org.osgi.framework.ServiceReference;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;

public class DOMNotificationPublishServiceProxy extends AbstractBrokerServiceProxy<DOMNotificationPublishService> implements DOMNotificationPublishService {

    public DOMNotificationPublishServiceProxy(ServiceReference<DOMNotificationPublishService> ref,
                                              DOMNotificationPublishService delegate) {
        super(ref, delegate);
    }

    @Nonnull
    @Override
    public ListenableFuture<? extends Object> putNotification(@Nonnull final DOMNotification notification) throws InterruptedException {
        return getDelegate().putNotification(notification);
    }

    @Nonnull
    @Override
    public ListenableFuture<? extends Object> offerNotification(@Nonnull final DOMNotification notification) {
        return getDelegate().offerNotification(notification);
    }

    @Nonnull
    @Override
    public ListenableFuture<? extends Object> offerNotification(@Nonnull final DOMNotification notification, @Nonnegative final long timeout, @Nonnull final TimeUnit unit) throws InterruptedException {
        return getDelegate().offerNotification(notification, timeout, unit);
    }
}
