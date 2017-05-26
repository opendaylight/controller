/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.inmemory_datastore_provider;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStoreConfigProperties;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStoreFactory;
import org.opendaylight.controller.md.sal.dom.store.impl.jmx.InMemoryDataStoreStats;

/**
 * The in-memory data store isn't used anymore. Deprecation notice in Carbon. Removal plan in Nitrogen.
 */
@Deprecated
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
        InMemoryDOMDataStore dataStore = InMemoryDOMDataStoreFactory.create("DOM-CFG",
            LogicalDatastoreType.CONFIGURATION, getSchemaServiceDependency(), getDebugTransactions(),
            InMemoryDOMDataStoreConfigProperties.create(getMaxDataChangeExecutorPoolSize(),
                getMaxDataChangeExecutorQueueSize(), getMaxDataChangeListenerQueueSize(),
                getMaxDataStoreExecutorQueueSize()));

        InMemoryDataStoreStats statsBean = new InMemoryDataStoreStats("InMemoryConfigDataStore", dataStore);
        dataStore.setCloseable(statsBean);

        return dataStore;
    }

}
