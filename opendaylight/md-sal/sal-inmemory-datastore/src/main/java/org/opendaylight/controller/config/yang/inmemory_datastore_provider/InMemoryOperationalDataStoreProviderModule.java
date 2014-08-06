package org.opendaylight.controller.config.yang.inmemory_datastore_provider;

import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStoreFactory;
import org.opendaylight.controller.md.sal.dom.store.impl.statistics.InMemoryOperDataStoreProviderRuntimeMXBeanImpl;

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

        InMemoryOperDataStoreProviderRuntimeMXBeanImpl statsMXBean =
                new InMemoryOperDataStoreProviderRuntimeMXBeanImpl();

        InMemoryDOMDataStore dataStore = InMemoryDOMDataStoreFactory.create(
                "DOM-OPER", getOperationalSchemaServiceDependency(), statsMXBean);

        InMemoryOperationalDataStoreProviderRuntimeRegistration statsMXBeanReg =
                getRootRuntimeBeanRegistratorWrapper().register(statsMXBean);
        dataStore.setCloseable(statsMXBeanReg);

        return dataStore;
    }

}
