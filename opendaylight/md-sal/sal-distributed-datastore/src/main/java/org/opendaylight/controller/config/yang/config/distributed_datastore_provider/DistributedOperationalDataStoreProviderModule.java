/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.distributed_datastore_provider;

import org.opendaylight.controller.cluster.datastore.DistributedDataStoreInterface;
import org.opendaylight.controller.cluster.datastore.compat.LegacyDOMStoreAdapter;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.osgi.WaitingServiceTracker;
import org.osgi.framework.BundleContext;

@Deprecated
public class DistributedOperationalDataStoreProviderModule
        extends AbstractDistributedOperationalDataStoreProviderModule {
    private BundleContext bundleContext;

    public DistributedOperationalDataStoreProviderModule(final ModuleIdentifier identifier,
            final DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public DistributedOperationalDataStoreProviderModule(final ModuleIdentifier identifier,
            final DependencyResolver dependencyResolver,final DistributedOperationalDataStoreProviderModule oldModule,
            final AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public boolean canReuseInstance(final AbstractDistributedOperationalDataStoreProviderModule oldModule) {
        return true;
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        // The DistributedOperDataStore is provided via blueprint so wait for and return it here for
        // backwards compatibility
        WaitingServiceTracker<DistributedDataStoreInterface> tracker = WaitingServiceTracker.create(
                DistributedDataStoreInterface.class, bundleContext, "(type=distributed-operational)");
        DistributedDataStoreInterface delegate = tracker.waitForService(WaitingServiceTracker.FIVE_MINUTES);
        return new LegacyDOMStoreAdapter(delegate) {
            @Override
            public void close() {
                tracker.close();
            }
        };
    }

    public void setBundleContext(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

}
