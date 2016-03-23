/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.md.sal.binding.impl;

import com.google.common.collect.ForwardingObject;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.sal.common.util.osgi.OsgiServiceUtils;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.osgi.framework.BundleContext;

public class BindingNotificationPublishAdapterModule extends AbstractBindingNotificationPublishAdapterModule {
    private BundleContext bundleContext;

    public BindingNotificationPublishAdapterModule(final ModuleIdentifier identifier, final DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public BindingNotificationPublishAdapterModule(final ModuleIdentifier identifier, final DependencyResolver dependencyResolver, final BindingNotificationPublishAdapterModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public AutoCloseable createInstance() {
        NotificationPublishService delegate = OsgiServiceUtils.waitForService(
                NotificationPublishService.class, bundleContext, OsgiServiceUtils.FIVE_MINUTES, null);
        return new ForwardingNotificationPublishService(delegate);
    }

    public void setBundleContext(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    private static class ForwardingNotificationPublishService extends ForwardingObject
            implements NotificationPublishService, AutoCloseable {
        private final NotificationPublishService delegate;

        public ForwardingNotificationPublishService(NotificationPublishService delegate) {
            this.delegate = delegate;
        }

        @Override
        public void close() throws Exception {
            // Intentional noop as the life-cycle is controlled via blueprint.
        }

        @Override
        protected NotificationPublishService delegate() {
            return delegate;
        }

        @Override
        public void putNotification(Notification notification) throws InterruptedException {
            delegate().putNotification(notification);
        }

        @Override
        public ListenableFuture<? extends Object> offerNotification(Notification notification) {
            return delegate().offerNotification(notification);
        }

        @Override
        public ListenableFuture<? extends Object> offerNotification(Notification notification, int timeout,
                TimeUnit unit) throws InterruptedException {
            return delegate().offerNotification(notification, timeout, unit);
        }
    }

}
