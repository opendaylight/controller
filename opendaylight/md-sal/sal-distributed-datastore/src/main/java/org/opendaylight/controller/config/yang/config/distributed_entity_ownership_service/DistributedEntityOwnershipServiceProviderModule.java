package org.opendaylight.controller.config.yang.config.distributed_entity_ownership_service;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.datastore.DistributedDataStore;
import org.opendaylight.controller.cluster.datastore.entityownership.DistributedEntityOwnershipService;
import org.opendaylight.controller.cluster.datastore.entityownership.selectionstrategy.EntityOwnerSelectionStrategyConfig;
import org.opendaylight.controller.cluster.datastore.entityownership.selectionstrategy.EntityOwnerSelectionStrategyConfigReader;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.osgi.framework.BundleContext;


public class DistributedEntityOwnershipServiceProviderModule extends org.opendaylight.controller.config.yang.config.distributed_entity_ownership_service.AbstractDistributedEntityOwnershipServiceProviderModule {
    private BundleContext bundleContext;

    public DistributedEntityOwnershipServiceProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public DistributedEntityOwnershipServiceProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.distributed_entity_ownership_service.DistributedEntityOwnershipServiceProviderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public boolean canReuseInstance(AbstractDistributedEntityOwnershipServiceProviderModule oldModule) {
        return true;
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        DOMStore dataStore = getDataStoreDependency();
        Preconditions.checkArgument(dataStore instanceof DistributedDataStore,
                "Injected DOMStore must be an instance of DistributedDataStore");
        EntityOwnerSelectionStrategyConfig strategyConfig = new EntityOwnerSelectionStrategyConfigReader(bundleContext).getConfig();
        DistributedEntityOwnershipService service = new DistributedEntityOwnershipService((DistributedDataStore)dataStore, strategyConfig);
        service.start();
        return service;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
