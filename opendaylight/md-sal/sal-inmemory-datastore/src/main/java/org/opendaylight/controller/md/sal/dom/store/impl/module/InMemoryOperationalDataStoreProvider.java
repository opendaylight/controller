/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.dom.store.impl.module;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStoreConfigProperties;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStoreFactory;
import org.opendaylight.controller.md.sal.dom.store.impl.jmx.InMemoryDataStoreStats;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.inmemory.datastore.provider.rev140617.DatastoreConfiguration;

public class InMemoryOperationalDataStoreProvider {

    private final InMemoryDOMDataStore dataStore;

    public InMemoryOperationalDataStoreProvider(final SchemaService schemaService, final DatastoreConfiguration datastoreConfig) {
        this.dataStore = InMemoryDOMDataStoreFactory.create("DOM-OPER",
                LogicalDatastoreType.OPERATIONAL, schemaService, datastoreConfig.isDebugTransactions(),
            InMemoryDOMDataStoreConfigProperties.create(
                    datastoreConfig.getMaxDataChangeExecutorPoolSize(),
                    datastoreConfig.getMaxDataChangeExecutorQueueSize(),
                    datastoreConfig.getMaxDataChangeListenerQueueSize(),
                    datastoreConfig.getMaxDataStoreExecutorQueueSize()));

        InMemoryDataStoreStats statsBean = new InMemoryDataStoreStats("InMemoryOperationalDataStore", dataStore);

        dataStore.setCloseable(statsBean);
    }

    public void close() {
        dataStore.close();
    }
}
