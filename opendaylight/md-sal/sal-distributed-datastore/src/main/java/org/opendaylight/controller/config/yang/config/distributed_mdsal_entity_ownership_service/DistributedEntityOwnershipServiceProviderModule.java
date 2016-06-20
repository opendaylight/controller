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
import org.opendaylight.mdsal.binding.api.clustering.Entity;
import org.opendaylight.mdsal.binding.api.clustering.EntityOwnershipCandidateRegistration;
import org.opendaylight.mdsal.binding.api.clustering.EntityOwnershipListener;
import org.opendaylight.mdsal.binding.api.clustering.EntityOwnershipListenerRegistration;
import org.opendaylight.mdsal.binding.api.clustering.EntityOwnershipService;
import org.opendaylight.mdsal.common.api.clustering.CandidateAlreadyRegisteredException;
import org.opendaylight.mdsal.common.api.clustering.EntityOwnershipState;
import org.osgi.framework.BundleContext;

public class DistributedEntityOwnershipServiceProviderModule
        extends AbstractDistributedEntityOwnershipServiceProviderModule {

    private BundleContext bundleContext;

    public DistributedEntityOwnershipServiceProviderModule(final ModuleIdentifier identifier,
            final DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public DistributedEntityOwnershipServiceProviderModule(final ModuleIdentifier identifier,
            final DependencyResolver dependencyResolver,
            final DistributedEntityOwnershipServiceProviderModule oldModule,
            final AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
    }

    @Override
    public boolean canReuseInstance(final AbstractDistributedEntityOwnershipServiceProviderModule oldModule) {
        return true;
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        // The DistributedEntityOwnershipService is provided via blueprint so wait for and return it
        // here for backwards compatiblity.
        final WaitingServiceTracker<EntityOwnershipService> tracker = WaitingServiceTracker
                .create(EntityOwnershipService.class, bundleContext);
        final EntityOwnershipService delegate = tracker.waitForService(WaitingServiceTracker.FIVE_MINUTES);
        return new ForwardingEntityOwnershipServiceMdsal(delegate, tracker);
    }

    public void setBundleContext(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    private static class ForwardingEntityOwnershipServiceMdsal extends ForwardingObject
            implements EntityOwnershipService, AutoCloseable {

        private final EntityOwnershipService delegate;
        private final AutoCloseable closeable;

        public ForwardingEntityOwnershipServiceMdsal(final EntityOwnershipService delegate,
                final AutoCloseable closeable) {
            this.delegate = Preconditions.checkNotNull(delegate);
            this.closeable = Preconditions.checkNotNull(closeable);
        }

        @Override
        public void close() throws Exception {
            // we don't close the delegate as the life-cycle is controlled via blueprint
            closeable.close();
        }

        @Override
        public EntityOwnershipCandidateRegistration registerCandidate(final Entity entity)
                throws CandidateAlreadyRegisteredException {
            return delegate.registerCandidate(entity);
        }

        @Override
        public EntityOwnershipListenerRegistration registerListener(final String entityType,
                final EntityOwnershipListener listener) {
            return delegate.registerListener(entityType, listener);
        }

        @Override
        public Optional<EntityOwnershipState> getOwnershipState(final Entity forEntity) {
            return delegate.getOwnershipState(forEntity);
        }

        @Override
        public boolean isCandidateRegistered(final Entity forEntity) {
            return delegate.isCandidateRegistered(forEntity);
        }

        @Override
        protected EntityOwnershipService delegate() {
            return delegate;
        }

    }
}
