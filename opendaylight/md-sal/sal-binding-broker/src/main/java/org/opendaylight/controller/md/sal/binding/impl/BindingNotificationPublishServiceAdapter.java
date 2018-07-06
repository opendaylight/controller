/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.binding.impl;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.impl.BindingAdapterBuilder.Factory;
import org.opendaylight.mdsal.binding.api.BindingService;
import org.opendaylight.yangtools.yang.binding.Notification;

public class BindingNotificationPublishServiceAdapter implements NotificationPublishService {

    static final Factory<NotificationPublishService> BUILDER_FACTORY = Builder::new;

    private final org.opendaylight.mdsal.binding.api.NotificationPublishService delegate;

    public BindingNotificationPublishServiceAdapter(
            final org.opendaylight.mdsal.binding.api.NotificationPublishService delegate) {
        this.delegate = requireNonNull(delegate);
    }

    @Override
    public void putNotification(final Notification notification) throws InterruptedException {
        delegate.putNotification(notification);
    }

    @Override
    public ListenableFuture<?> offerNotification(final Notification notification) {
        return delegate.offerNotification(notification);
    }

    @Override
    public ListenableFuture<?> offerNotification(final Notification notification, final int timeout,
            final TimeUnit unit) throws InterruptedException {
        return delegate.offerNotification(notification, timeout, unit);
    }

    private static class Builder extends BindingAdapterBuilder<NotificationPublishService> {
        @Override
        public Set<? extends Class<? extends BindingService>> getRequiredDelegates() {
            return ImmutableSet.of(org.opendaylight.mdsal.binding.api.NotificationPublishService.class);
        }

        @Override
        protected NotificationPublishService createInstance(ClassToInstanceMap<BindingService> delegates) {
            return new BindingNotificationPublishServiceAdapter(delegates.getInstance(
                    org.opendaylight.mdsal.binding.api.NotificationPublishService.class));
        }
    }
}
