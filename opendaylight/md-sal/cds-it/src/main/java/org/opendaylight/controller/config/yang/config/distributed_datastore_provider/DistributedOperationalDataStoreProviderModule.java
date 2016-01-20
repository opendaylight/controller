package org.opendaylight.controller.config.yang.config.distributed_datastore_provider;
public class DistributedOperationalDataStoreProviderModule extends org.opendaylight.controller.config.yang.config.distributed_datastore_provider.AbstractDistributedOperationalDataStoreProviderModule {
    public DistributedOperationalDataStoreProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public DistributedOperationalDataStoreProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.distributed_datastore_provider.DistributedOperationalDataStoreProviderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        // TODO:implement
        throw new java.lang.UnsupportedOperationException();
    }

}
