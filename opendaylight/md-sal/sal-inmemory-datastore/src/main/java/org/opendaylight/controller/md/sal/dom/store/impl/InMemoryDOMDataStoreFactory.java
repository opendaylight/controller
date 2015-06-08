/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl;

import java.util.concurrent.ExecutorService;
import javax.annotation.Nullable;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;
import org.opendaylight.yangtools.yang.data.api.schema.tree.TreeType;

/**
 * A factory for creating InMemoryDOMDataStore instances.
 *
 * @author Thomas Pantelis
 */
public final class InMemoryDOMDataStoreFactory {

    private InMemoryDOMDataStoreFactory() {
    }

    public static InMemoryDOMDataStore create(final String name,
            @Nullable final SchemaService schemaService) {
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
    public static InMemoryDOMDataStore create(final String name,
            @Nullable final SchemaService schemaService,
            @Nullable final InMemoryDOMDataStoreConfigProperties properties) {
        return create(name, TreeType.OPERATIONAL, schemaService, false, properties);
    }


    /**
     * Creates an InMemoryDOMDataStore instance.
     *
     * @param name the name of the data store
     * @param schemaService the SchemaService to which to register the data store.
     * @param debugTransactions enable transaction debugging
     * @param properties configuration properties for the InMemoryDOMDataStore instance. If null,
     *                   default property values are used.
     * @return an InMemoryDOMDataStore instance
     */
    public static InMemoryDOMDataStore create(final String name,
            @Nullable final SchemaService schemaService, final boolean debugTransactions,
            @Nullable final InMemoryDOMDataStoreConfigProperties properties) {
        return create(name, TreeType.OPERATIONAL, schemaService, debugTransactions, properties);
    }

    /**
     * Creates an InMemoryDOMDataStore instance.
     *
     * @param name the name of the data store
     * @param schemaService the SchemaService to which to register the data store.
     * @param debugTransactions enable transaction debugging
     * @param properties configuration properties for the InMemoryDOMDataStore instance. If null,
     *                   default property values are used.
     * @return an InMemoryDOMDataStore instance
     */
    public static InMemoryDOMDataStore create(final String name, final TreeType treeType,
            @Nullable final SchemaService schemaService, final boolean debugTransactions,
            @Nullable final InMemoryDOMDataStoreConfigProperties properties) {

        InMemoryDOMDataStoreConfigProperties actualProperties = properties;
        if (actualProperties == null) {
            actualProperties = InMemoryDOMDataStoreConfigProperties.getDefault();
        }

        // For DataChangeListener notifications we use an executor that provides the fastest
        // task execution time to get higher throughput as DataChangeListeners typically provide
        // much of the business logic for a data model. If the executor queue size limit is reached,
        // subsequent submitted notifications will block the calling thread.
        final int dclExecutorMaxQueueSize = actualProperties.getMaxDataChangeExecutorQueueSize();
        final int dclExecutorMaxPoolSize = actualProperties.getMaxDataChangeExecutorPoolSize();

        final ExecutorService dataChangeListenerExecutor = SpecialExecutors.newBlockingBoundedFastThreadPool(
                dclExecutorMaxPoolSize, dclExecutorMaxQueueSize, name + "-DCL" );


        final InMemoryDOMDataStore dataStore = new InMemoryDOMDataStore(name, treeType, dataChangeListenerExecutor,
                actualProperties.getMaxDataChangeListenerQueueSize(), debugTransactions);

        if (schemaService != null) {
            schemaService.registerSchemaContextListener(dataStore);
        }

        return dataStore;
    }
}
