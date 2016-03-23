/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.md.sal.binding.impl;

import com.google.common.collect.ForwardingObject;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.sal.common.util.osgi.OsgiServiceUtils;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.NotificationListener;
import org.osgi.framework.BundleContext;

public class BindingNotificationAdapterModule extends AbstractBindingNotificationAdapterModule  {
    private BundleContext bundleContext;

    public BindingNotificationAdapterModule(final ModuleIdentifier identifier, final DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public BindingNotificationAdapterModule(final ModuleIdentifier identifier, final DependencyResolver dependencyResolver, final org.opendaylight.controller.config.yang.md.sal.binding.impl.BindingNotificationAdapterModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public AutoCloseable createInstance() {
        NotificationService delegate = OsgiServiceUtils.waitForService(
                NotificationService.class, bundleContext, OsgiServiceUtils.FIVE_MINUTES, null);
        return new ForwardingNotificationService(delegate);
    }

    public void setBundleContext(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    private static class ForwardingNotificationService extends ForwardingObject implements NotificationService, AutoCloseable {
        private final NotificationService delegate;

        public ForwardingNotificationService(NotificationService delegate) {
            this.delegate = delegate;
        }

        @Override
        public void close() throws Exception {
            // Intentional noop as the life-cycle is controlled via blueprint.
        }

        @Override
        public <T extends NotificationListener> ListenerRegistration<T> registerNotificationListener(T listener) {
            return delegate().registerNotificationListener(listener);
        }

        @Override
        protected NotificationService delegate() {
            return delegate;
        }
    }
}
