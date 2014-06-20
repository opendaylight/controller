package org.opendaylight.controller.config.yang.inmemory_datastore_provider;

import com.google.common.util.concurrent.MoreExecutors;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;

public class InMemoryOperationalDataStoreProviderModule extends org.opendaylight.controller.config.yang.inmemory_datastore_provider.AbstractInMemoryOperationalDataStoreProviderModule {
    public InMemoryOperationalDataStoreProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public InMemoryOperationalDataStoreProviderModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.inmemory_datastore_provider.InMemoryOperationalDataStoreProviderModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
      InMemoryDOMDataStore ids = new InMemoryDOMDataStore("DOM-OPER", MoreExecutors.sameThreadExecutor());
      getSchemaServiceDependency().registerSchemaServiceListener(ids);
      return ids;
    }

}
