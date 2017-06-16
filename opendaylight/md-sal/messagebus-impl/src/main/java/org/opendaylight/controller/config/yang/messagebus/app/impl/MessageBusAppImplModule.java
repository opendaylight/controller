/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.messagebus.app.impl;

import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.osgi.WaitingServiceTracker;
import org.opendaylight.controller.messagebus.spi.EventSource;
import org.opendaylight.controller.messagebus.spi.EventSourceRegistration;
import org.opendaylight.controller.messagebus.spi.EventSourceRegistry;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class MessageBusAppImplModule extends AbstractMessageBusAppImplModule {
    private static final Logger LOG = LoggerFactory.getLogger(MessageBusAppImplModule.class);

    private BundleContext bundleContext;

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public MessageBusAppImplModule(final ModuleIdentifier identifier, final DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public MessageBusAppImplModule(final ModuleIdentifier identifier, final DependencyResolver dependencyResolver,
            final MessageBusAppImplModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final WaitingServiceTracker<EventSourceRegistry> tracker =
                WaitingServiceTracker.create(EventSourceRegistry.class, bundleContext);
        final EventSourceRegistry service = tracker.waitForService(WaitingServiceTracker.FIVE_MINUTES);

        return new EventSourceRegistry() {
            @Override
            public void close() {
                // We need to close the WaitingServiceTracker however we don't want to close the actual
                // service instance because its life-cycle is controlled via blueprint.
                tracker.close();
            }

            @Override
            public <T extends EventSource> EventSourceRegistration<T> registerEventSource(T eventSource) {
                return service.registerEventSource(eventSource);
            }
        };
    }
}
