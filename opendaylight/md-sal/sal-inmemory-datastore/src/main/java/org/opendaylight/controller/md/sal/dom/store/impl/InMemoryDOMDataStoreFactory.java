/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.dom.store.impl;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nullable;

import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;
import com.google.common.util.concurrent.MoreExecutors;

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

        InMemoryDOMDataStoreConfigProperties actualProperties = properties;
        if(actualProperties == null) {
            actualProperties = InMemoryDOMDataStoreConfigProperties.getDefault();
        }

        // For DataChangeListener notifications we use an executor that provides the fastest
        // task execution time to get higher throughput as DataChangeListeners typically provide
        // much of the business logic for a data model. If the executor queue size limit is reached,
        // subsequent submitted notifications will block the calling thread.

        int dclExecutorMaxQueueSize = actualProperties.getMaxDataChangeExecutorQueueSize();
        int dclExecutorMaxPoolSize = actualProperties.getMaxDataChangeExecutorPoolSize();

        ExecutorService dataChangeListenerExecutor = SpecialExecutors.newBlockingBoundedFastThreadPool(
                dclExecutorMaxPoolSize, dclExecutorMaxQueueSize, name + "-DCL" );

        InMemoryDOMDataStore dataStore = new InMemoryDOMDataStore(name,
                MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor()),
                dataChangeListenerExecutor, actualProperties.getMaxDataChangeListenerQueueSize());

        if(schemaService != null) {
            schemaService.registerSchemaContextListener(dataStore);
        }

        return dataStore;
    }
}
