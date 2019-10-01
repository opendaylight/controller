/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.api;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

@Deprecated(forRemoval = true)
public abstract class AbstractBrokerAwareActivator implements BundleActivator {
    private final class Customizer implements ServiceTrackerCustomizer<BindingAwareBroker, BindingAwareBroker> {
        private final BundleContext context;

        Customizer(final BundleContext context) {
            this.context = requireNonNull(context);
        }

        @Override
        public BindingAwareBroker addingService(final ServiceReference<BindingAwareBroker> reference) {
            final BindingAwareBroker broker = context.getService(reference);
            MD_ACTIVATION_POOL.execute(() -> onBrokerAvailable(broker, context));
            return broker;
        }

        @Override
        public void modifiedService(final ServiceReference<BindingAwareBroker> reference,
                final BindingAwareBroker service) {
            removedService(reference, service);
            addingService(reference);
        }

        @Override
        public void removedService(final ServiceReference<BindingAwareBroker> reference,
                final BindingAwareBroker service) {
            final BindingAwareBroker broker = context.getService(reference);
            MD_ACTIVATION_POOL.execute(() -> onBrokerRemoved(broker, context));
        }
    }

    private static final ExecutorService MD_ACTIVATION_POOL = Executors.newCachedThreadPool();

    private ServiceTracker<BindingAwareBroker, BindingAwareBroker> tracker;

    @Override
    public final void start(final BundleContext bundleContext) {
        startImpl(bundleContext);
        tracker = new ServiceTracker<>(bundleContext, BindingAwareBroker.class, new Customizer(bundleContext));
        tracker.open();
    }

    @Override
    public final void stop(final BundleContext bundleContext) {
        if (tracker != null) {
            tracker.close();
        }
        stopImpl(bundleContext);
    }

    /**
     * Called when this bundle is started (before
     * {@link BindingAwareProvider#onSessionInitiated(ProviderContext)} so the Framework can perform
     * the bundle-specific activities necessary to start this bundle. This
     * method can be used to register services or to allocate any resources that
     * this bundle needs.
     *
     * <p>
     * This method must complete and return to its caller in a timely manner.
     *
     * @param bundleContext
     *            The execution context of the bundle being started.
     * @throws RuntimeException
     *             If this method throws an exception, this bundle is marked as
     *             stopped and the Framework will remove this bundle's
     *             listeners, unregister all services registered by this bundle,
     *             and release all services used by this bundle.
     */
    protected void startImpl(final BundleContext bundleContext) {
        // NOOP
    }

    /**
     * Called when this bundle is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle. In general, this
     * method should undo the work that the {@code BundleActivator.start} method
     * started. There should be no active threads that were started by this
     * bundle when this bundle returns. A stopped bundle must not call any
     * Framework objects.
     *
     * <p>
     * This method must complete and return to its caller in a timely manner.
     *
     * @param bundleContext The execution context of the bundle being stopped.
     * @throws RuntimeException If this method throws an exception, the bundle is still
     *         marked as stopped, and the Framework will remove the bundle's
     *         listeners, unregister all services registered by the bundle, and
     *         release all services used by the bundle.
     */
    protected void stopImpl(final BundleContext bundleContext) {
        // NOOP
    }

    protected abstract void onBrokerAvailable(BindingAwareBroker bindingBroker, BundleContext bundleContext);

    protected void onBrokerRemoved(final BindingAwareBroker bindingBroker, final BundleContext bundleContext) {
        stopImpl(bundleContext);
    }
}
