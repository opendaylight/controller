/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.config.concurrent_data_broker;

import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.osgi.WaitingServiceTracker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.spi.ForwardingDOMDataBroker;
import org.osgi.framework.BundleContext;

public class DomConcurrentDataBrokerModule extends AbstractDomConcurrentDataBrokerModule {
    private BundleContext bundleContext;

    public DomConcurrentDataBrokerModule(final ModuleIdentifier identifier,
            final DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public DomConcurrentDataBrokerModule(final ModuleIdentifier identifier,
            final DependencyResolver dependencyResolver, final DomConcurrentDataBrokerModule oldModule,
            final AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public boolean canReuseInstance(AbstractDomConcurrentDataBrokerModule oldModule) {
        return true;
    }

    @Override
    public AutoCloseable createInstance() {
        // The ConcurrentDOMDataBroker is provided via blueprint so wait for and return it here for
        // backwards compatibility.
        WaitingServiceTracker<DOMDataBroker> tracker = WaitingServiceTracker.create(
                DOMDataBroker.class, bundleContext, "(type=default)");
        DOMDataBroker delegate = tracker.waitForService(WaitingServiceTracker.FIVE_MINUTES);
        return new ForwardingConcurrentDOMBroker(delegate, tracker);
    }

    public void setBundleContext(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    private static class ForwardingConcurrentDOMBroker extends ForwardingDOMDataBroker implements AutoCloseable {
        private final DOMDataBroker delegate;
        private final AutoCloseable closeable;

        ForwardingConcurrentDOMBroker(DOMDataBroker delegate, AutoCloseable closeable) {
            this.delegate = delegate;
            this.closeable = closeable;
        }

        @Override
        protected DOMDataBroker delegate() {
            return delegate;
        }

        @Override
        public void close() throws Exception {
            // We don't close the delegate as the life-cycle is controlled via blueprint.
            closeable.close();
        }
    }
}
