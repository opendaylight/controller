/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.actor_system_provider.impl;

import akka.actor.ActorSystem;
import com.google.common.collect.ForwardingObject;
import org.opendaylight.controller.cluster.ActorSystemProvider;
import org.opendaylight.controller.cluster.ActorSystemProviderListener;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.osgi.WaitingServiceTracker;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.osgi.framework.BundleContext;

public class ActorSystemProviderModule extends AbstractActorSystemProviderModule {
    private BundleContext bundleContext;

    public ActorSystemProviderModule(ModuleIdentifier identifier, DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public ActorSystemProviderModule(ModuleIdentifier identifier, DependencyResolver dependencyResolver,
            ActorSystemProviderModule oldModule, AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public boolean canReuseInstance(AbstractActorSystemProviderModule oldModule) {
        return true;
    }

    @Override
    public AutoCloseable createInstance() {
        // The service is provided via blueprint so wait for and return it here for backwards compatibility.
        WaitingServiceTracker<ActorSystemProvider> tracker = WaitingServiceTracker.create(
                ActorSystemProvider.class, bundleContext);
        ActorSystemProvider delegate = tracker.waitForService(WaitingServiceTracker.FIVE_MINUTES);
        return new ForwardingActorSystemProvider(delegate, tracker);
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    private static class ForwardingActorSystemProvider extends ForwardingObject
            implements ActorSystemProvider, AutoCloseable {
        private final ActorSystemProvider delegate;
        private final AutoCloseable closeable;

        ForwardingActorSystemProvider(ActorSystemProvider delegate, AutoCloseable closeable) {
            this.delegate = delegate;
            this.closeable = closeable;
        }

        @Override
        public ActorSystem getActorSystem() {
            return delegate().getActorSystem();
        }

        @Override
        public ListenerRegistration<ActorSystemProviderListener> registerActorSystemProviderListener(
                ActorSystemProviderListener listener) {
            return delegate().registerActorSystemProviderListener(listener);
        }

        @Override
        protected ActorSystemProvider delegate() {
            return delegate;
        }

        @Override
        public void close() throws Exception {
            // We don't close the delegate as the life-cycle is controlled via blueprint.
            closeable.close();
        }
    }
}
