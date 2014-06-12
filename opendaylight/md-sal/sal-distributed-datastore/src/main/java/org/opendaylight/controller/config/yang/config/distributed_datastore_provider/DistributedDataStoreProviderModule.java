package org.opendaylight.controller.config.yang.config.distributed_datastore_provider;

import org.opendaylight.controller.cluster.datastore.DOMStoreProxy;

public class DistributedDataStoreProviderModule extends org.opendaylight.controller.config.yang.config.distributed_datastore_provider.AbstractDistributedDataStoreProviderModule {
    public DistributedDataStoreProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public DistributedDataStoreProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.distributed_datastore_provider.DistributedDataStoreProviderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        new DOMStoreProxy();

        final class AutoCloseableDistributedDataStore implements AutoCloseable {

            @Override
            public void close() throws Exception {

            }
        }

        return new AutoCloseableDistributedDataStore();
    }

}
