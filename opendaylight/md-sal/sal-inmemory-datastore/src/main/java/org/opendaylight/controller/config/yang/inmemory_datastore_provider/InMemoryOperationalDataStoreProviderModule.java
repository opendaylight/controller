package org.opendaylight.controller.config.yang.inmemory_datastore_provider;

import java.util.concurrent.Executors;

import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;

import com.google.common.util.concurrent.MoreExecutors;

public class InMemoryOperationalDataStoreProviderModule extends org.opendaylight.controller.config.yang.inmemory_datastore_provider.AbstractInMemoryOperationalDataStoreProviderModule {
    public InMemoryOperationalDataStoreProviderModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public InMemoryOperationalDataStoreProviderModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, final org.opendaylight.controller.config.yang.inmemory_datastore_provider.InMemoryOperationalDataStoreProviderModule oldModule, final java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
      InMemoryDOMDataStore ids = new InMemoryDOMDataStore("DOM-OPER", MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor()));
      getSchemaServiceDependency().registerSchemaServiceListener(ids);
      return ids;
    }

}
