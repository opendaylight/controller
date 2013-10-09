/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.binding.api;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public abstract class AbstractBindingAwareConsumer implements BindingAwareConsumer,BundleActivator {

    @Override
    public final void start(BundleContext context) throws Exception {
        startImpl(context);
        ServiceReference<BindingAwareBroker> brokerRef = context.getServiceReference(BindingAwareBroker.class);
        BindingAwareBroker broker = context.getService(brokerRef);
        broker.registerConsumer(this, context);
        //context.ungetService(brokerRef);
    }

    /**
     * Called when this bundle is started (before
     * {@link #onSessionInitiated(ProviderContext)} so the Framework can perform
     * the bundle-specific activities necessary to start this bundle. This
     * method can be used to register services or to allocate any resources that
     * this bundle needs.
     * 
     * <p>
     * This method must complete and return to its caller in a timely manner.
     * 
     * @param context
     *            The execution context of the bundle being started.
     * @throws Exception
     *             If this method throws an exception, this bundle is marked as
     *             stopped and the Framework will remove this bundle's
     *             listeners, unregister all services registered by this bundle,
     *             and release all services used by this bundle.
     */
    protected void startImpl(BundleContext context) {
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
     * @param context The execution context of the bundle being stopped.
     * @throws Exception If this method throws an exception, the bundle is still
     *         marked as stopped, and the Framework will remove the bundle's
     *         listeners, unregister all services registered by the bundle, and
     *         release all services used by the bundle.
     */
    protected void stopImpl(BundleContext context) {
        // NOOP
    }
    
    @Override
    public final  void stop(BundleContext context) throws Exception {
        stopImpl(context);
    }

}
