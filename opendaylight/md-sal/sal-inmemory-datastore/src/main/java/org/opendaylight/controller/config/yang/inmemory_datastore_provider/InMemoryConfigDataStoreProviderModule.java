package org.opendaylight.controller.config.yang.inmemory_datastore_provider;

import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStoreConfigProperties;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStoreFactory;

public class InMemoryConfigDataStoreProviderModule extends org.opendaylight.controller.config.yang.inmemory_datastore_provider.AbstractInMemoryConfigDataStoreProviderModule {

    public InMemoryConfigDataStoreProviderModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public InMemoryConfigDataStoreProviderModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, final org.opendaylight.controller.config.yang.inmemory_datastore_provider.InMemoryConfigDataStoreProviderModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        return InMemoryDOMDataStoreFactory.create("DOM-CFG", getSchemaServiceDependency(),
                InMemoryDOMDataStoreConfigProperties.create(getMaxDataChangeExecutorPoolSize(),
                        getMaxDataChangeExecutorQueueSize(), getMaxDataChangeListenerQueueSize()));
    }

}
