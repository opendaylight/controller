/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.distributed_entity_ownership_service;

import com.google.common.base.Optional;
import com.google.common.collect.ForwardingObject;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.osgi.WaitingServiceTracker;
import org.opendaylight.controller.md.sal.common.api.clustering.CandidateAlreadyRegisteredException;
import org.opendaylight.controller.md.sal.common.api.clustering.Entity;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipCandidateRegistration;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipListener;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipListenerRegistration;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipService;
import org.opendaylight.controller.md.sal.common.api.clustering.EntityOwnershipState;
import org.osgi.framework.BundleContext;

public class DistributedEntityOwnershipServiceProviderModule extends AbstractDistributedEntityOwnershipServiceProviderModule {
    private BundleContext bundleContext;

    public DistributedEntityOwnershipServiceProviderModule(final ModuleIdentifier identifier,
            final DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public DistributedEntityOwnershipServiceProviderModule(final ModuleIdentifier identifier,
            final DependencyResolver dependencyResolver,
            final DistributedEntityOwnershipServiceProviderModule oldModule, final AutoCloseable oldInstance) {
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
    public AutoCloseable createInstance() {
        // The DistributedEntityOwnershipService is provided via blueprint so wait for and return it here for
        // backwards compatibility.
        WaitingServiceTracker<EntityOwnershipService> tracker = WaitingServiceTracker.create(
                EntityOwnershipService.class, bundleContext);
        EntityOwnershipService delegate = tracker.waitForService(WaitingServiceTracker.FIVE_MINUTES);
        return new ForwardingEntityOwnershipService(delegate, tracker);
    }

    public void setBundleContext(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    private static class ForwardingEntityOwnershipService extends ForwardingObject
            implements EntityOwnershipService, AutoCloseable {
        private final EntityOwnershipService delegate;
        private final AutoCloseable closeable;

        public ForwardingEntityOwnershipService(EntityOwnershipService delegate, AutoCloseable closeable) {
            this.delegate = delegate;
            this.closeable = closeable;
        }

        @Override
        public EntityOwnershipCandidateRegistration registerCandidate(Entity entity)
                throws CandidateAlreadyRegisteredException {
            return delegate().registerCandidate(entity);
        }

        @Override
        public EntityOwnershipListenerRegistration registerListener(String entityType,
                EntityOwnershipListener listener) {
            return delegate().registerListener(entityType, listener);
        }

        @Override
        public Optional<EntityOwnershipState> getOwnershipState(Entity forEntity) {
            return delegate().getOwnershipState(forEntity);
        }

        @Override
        public boolean isCandidateRegistered(Entity entity) {
            return delegate().isCandidateRegistered(entity);
        }

        @Override
        protected EntityOwnershipService delegate() {
            return delegate;
        }

        @Override
        public void close() throws Exception {
            // We don't close the delegate as the life-cycle is controlled via blueprint.
            closeable.close();
        }
    }
}
