/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.api;

import java.util.Collection;
import java.util.Collections;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public abstract class AbstractProvider implements BundleActivator, Provider,ServiceTrackerCustomizer<Broker, Broker> {

    private Broker broker;
    private BundleContext context;
    private ServiceTracker<Broker, Broker> tracker;
    @Override
    public Collection<ProviderFunctionality> getProviderFunctionality() {
        return Collections.emptySet();
    }

    @Override
    public final void start(BundleContext context) throws Exception {
        this.context = context;
        this.startImpl(context);
        tracker = new ServiceTracker<>(context, Broker.class, this);
        tracker.open();
    }

    protected void startImpl(BundleContext context) {
        // NOOP
    }
    protected void stopImpl(BundleContext context) {
        // NOOP
    }

    @Override
    public final void stop(BundleContext context) throws Exception {
        broker = null;
        tracker.close();
        tracker = null;
        stopImpl(context);
    }

    @Override
    public Broker addingService(ServiceReference<Broker> reference) {
        if(broker == null) {
            broker = context.getService(reference);
            broker.registerProvider(this, context);
            return broker;
        }

        return null;
    }

    @Override
    public void modifiedService(ServiceReference<Broker> reference, Broker service) {
        // NOOP
    }

    @Override
    public void removedService(ServiceReference<Broker> reference, Broker service) {
        stopImpl(context);
    }

}
