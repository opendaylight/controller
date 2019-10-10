/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl;

import java.util.concurrent.ExecutorService;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;

/**
 * A factory for creating InMemoryDOMDataStore instances.
 *
 * @author Thomas Pantelis
 *
 * @deprecated Use {@link org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataStoreFactory} instead.
 */
@Deprecated(forRemoval = true)
public final class InMemoryDOMDataStoreFactory {

    private InMemoryDOMDataStoreFactory() {
    }

    public static InMemoryDOMDataStore create(final String name, final @Nullable DOMSchemaService schemaService) {
        return create(name, schemaService, null);
    }

    /**
     * Creates an InMemoryDOMDataStore instance.
     *
     * @param name the name of the data store
     * @param schemaService the SchemaService to which to register the data store.
     * @param properties configuration properties for the InMemoryDOMDataStore instance. If null,
     *                   default property values are used.
     * @return an InMemoryDOMDataStore instance
     */
    public static InMemoryDOMDataStore create(final String name, final @Nullable DOMSchemaService schemaService,
            final @Nullable InMemoryDOMDataStoreConfigProperties properties) {
        return create(name, LogicalDatastoreType.OPERATIONAL, schemaService, false, properties);
    }

    /**
     * Creates an InMemoryDOMDataStore instance.
     *
     * @param name the name of the data store
     * @param type Data store type
     * @param schemaService the SchemaService to which to register the data store.
     * @param debugTransactions enable transaction debugging
     * @param properties configuration properties for the InMemoryDOMDataStore instance. If null,
     *                   default property values are used.
     * @return an InMemoryDOMDataStore instance
     */
    public static InMemoryDOMDataStore create(final String name, final LogicalDatastoreType type,
            final @Nullable DOMSchemaService schemaService, final boolean debugTransactions,
            final @Nullable InMemoryDOMDataStoreConfigProperties properties) {

        InMemoryDOMDataStoreConfigProperties actualProperties = properties;
        if (actualProperties == null) {
            actualProperties = InMemoryDOMDataStoreConfigProperties.getDefault();
        }

        // For DataChangeListener notifications we use an executor that provides the fastest
        // task execution time to get higher throughput as DataChangeListeners typically provide
        // much of the business logic for a data model. If the executor queue size limit is reached,
        // subsequent submitted notifications will block the calling thread.
        int dclExecutorMaxQueueSize = actualProperties.getMaxDataChangeExecutorQueueSize();
        int dclExecutorMaxPoolSize = actualProperties.getMaxDataChangeExecutorPoolSize();

        ExecutorService dataChangeListenerExecutor = SpecialExecutors.newBlockingBoundedFastThreadPool(
                dclExecutorMaxPoolSize, dclExecutorMaxQueueSize, name + "-DCL", InMemoryDOMDataStore.class);

        final InMemoryDOMDataStore dataStore = new InMemoryDOMDataStore(name, type, dataChangeListenerExecutor,
                actualProperties.getMaxDataChangeListenerQueueSize(), debugTransactions);

        if (schemaService != null) {
            schemaService.registerSchemaContextListener(dataStore);
        }

        return dataStore;
    }
}
