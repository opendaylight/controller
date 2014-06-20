package org.opendaylight.controller.config.yang.inmemory_datastore_provider;

import com.google.common.util.concurrent.MoreExecutors;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;

public class InMemoryConfigDataStoreProviderModule extends org.opendaylight.controller.config.yang.inmemory_datastore_provider.AbstractInMemoryConfigDataStoreProviderModule {
    public InMemoryConfigDataStoreProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public InMemoryConfigDataStoreProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.inmemory_datastore_provider.InMemoryConfigDataStoreProviderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
      InMemoryDOMDataStore   ids = new InMemoryDOMDataStore("DOM-CFG", MoreExecutors.sameThreadExecutor());
      getSchemaServiceDependency().registerSchemaServiceListener(ids);
      return ids;
    }

}
