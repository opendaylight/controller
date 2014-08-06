package org.opendaylight.controller.config.yang.inmemory_datastore_provider;

import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStoreFactory;
import org.opendaylight.controller.md.sal.dom.store.impl.statistics.InMemoryConfigDataStoreProviderRuntimeMXBeanImpl;

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

        InMemoryConfigDataStoreProviderRuntimeMXBeanImpl statsMXBean =
                new InMemoryConfigDataStoreProviderRuntimeMXBeanImpl();

        InMemoryDOMDataStore dataStore = InMemoryDOMDataStoreFactory.create(
                "DOM-CFG", getSchemaServiceDependency(), statsMXBean);

        InMemoryConfigDataStoreProviderRuntimeRegistration statsMXBeanReg =
                getRootRuntimeBeanRegistratorWrapper().register(statsMXBean);
        dataStore.setCloseable(statsMXBeanReg);

        return dataStore;
    }

}
