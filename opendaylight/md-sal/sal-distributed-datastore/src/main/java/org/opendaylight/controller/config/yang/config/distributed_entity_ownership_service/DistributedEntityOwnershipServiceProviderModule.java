/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.distributed_entity_ownership_service;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.datastore.DistributedDataStore;
import org.opendaylight.controller.cluster.datastore.entityownership.DistributedEntityOwnershipService;
import org.opendaylight.controller.cluster.datastore.entityownership.selectionstrategy.EntityOwnerSelectionStrategyConfig;
import org.opendaylight.controller.cluster.datastore.entityownership.selectionstrategy.EntityOwnerSelectionStrategyConfigReader;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.osgi.framework.BundleContext;

public class DistributedEntityOwnershipServiceProviderModule extends AbstractDistributedEntityOwnershipServiceProviderModule {
    private EntityOwnerSelectionStrategyConfig strategyConfig;
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
        strategyConfig = EntityOwnerSelectionStrategyConfigReader.loadStrategyWithConfig(bundleContext);
    }

    @Override
    public boolean canReuseInstance(final AbstractDistributedEntityOwnershipServiceProviderModule oldModule) {
        return true;
    }

    @Override
    public AutoCloseable createInstance() {
        // FIXME: EntityOwnership needs only the ActorContext, not the entire datastore
        DOMStore dataStore = getDataStoreDependency();
        Preconditions.checkArgument(dataStore instanceof DistributedDataStore,
                "Injected DOMStore must be an instance of DistributedDataStore");

        final ActorContext context = ((DistributedDataStore)dataStore).getActorContext();
        return DistributedEntityOwnershipService.start(context, strategyConfig);
    }

    public void setBundleContext(final BundleContext bundleContext) {
        // What do we need from the bundle context?
        this.bundleContext = bundleContext;
    }
}
