/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.distributed_mdsal_entity_ownership_service;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ForwardingObject;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.osgi.WaitingServiceTracker;
import org.opendaylight.mdsal.common.api.clustering.CandidateAlreadyRegisteredException;
import org.opendaylight.mdsal.common.api.clustering.EntityOwnershipState;
import org.opendaylight.mdsal.dom.api.clustering.DOMEntity;
import org.opendaylight.mdsal.dom.api.clustering.DOMEntityOwnershipCandidateRegistration;
import org.opendaylight.mdsal.dom.api.clustering.DOMEntityOwnershipListener;
import org.opendaylight.mdsal.dom.api.clustering.DOMEntityOwnershipListenerRegistration;
import org.opendaylight.mdsal.dom.api.clustering.DOMEntityOwnershipService;
import org.osgi.framework.BundleContext;

public class DOMDistributedEntityOwnershipServiceProviderModule
        extends AbstractDOMDistributedEntityOwnershipServiceProviderModule {

    private BundleContext bundleContext;

    public DOMDistributedEntityOwnershipServiceProviderModule(final ModuleIdentifier identifier,
            final DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public DOMDistributedEntityOwnershipServiceProviderModule(final ModuleIdentifier identifier,
            final DependencyResolver dependencyResolver,
            final DOMDistributedEntityOwnershipServiceProviderModule oldModule,
            final AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
    }

    @Override
    public boolean canReuseInstance(final AbstractDOMDistributedEntityOwnershipServiceProviderModule oldModule) {
        return true;
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        // The DOMDistributedEntityOwnershipService is provided via blueprint so wait for and return
        // it here for backwards compatibility.
        final WaitingServiceTracker<DOMEntityOwnershipService> tracker = WaitingServiceTracker
                .create(DOMEntityOwnershipService.class, bundleContext);
        final DOMEntityOwnershipService delegate = tracker.waitForService(WaitingServiceTracker.FIVE_MINUTES);
        return new DOMForwardingEntityOwnershipServiceMdsal(delegate, tracker);
    }

    public void setBundleContext(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    private static class DOMForwardingEntityOwnershipServiceMdsal extends ForwardingObject
            implements DOMEntityOwnershipService, AutoCloseable {

        private final DOMEntityOwnershipService delegate;
        private final AutoCloseable closeable;

        public DOMForwardingEntityOwnershipServiceMdsal(final DOMEntityOwnershipService delegate,
                final AutoCloseable closeable) {
            this.delegate = Preconditions.checkNotNull(delegate);
            this.closeable = Preconditions.checkNotNull(closeable);
        }

        @Override
        public void close() throws Exception {
            // we don't close the delegate as the life-cycle is controlled via blueprint.
            closeable.close();
        }

        @Override
        public DOMEntityOwnershipCandidateRegistration registerCandidate(final DOMEntity entity)
                throws CandidateAlreadyRegisteredException {
            return delegate.registerCandidate(entity);
        }

        @Override
        public DOMEntityOwnershipListenerRegistration registerListener(final String entityType,
                final DOMEntityOwnershipListener listener) {
            return delegate.registerListener(entityType, listener);
        }

        @Override
        public Optional<EntityOwnershipState> getOwnershipState(final DOMEntity forEntity) {
            return delegate.getOwnershipState(forEntity);
        }

        @Override
        public boolean isCandidateRegistered(final DOMEntity forEntity) {
            return delegate.isCandidateRegistered(forEntity);
        }

        @Override
        protected DOMEntityOwnershipService delegate() {
            return delegate;
        }

    }
}
