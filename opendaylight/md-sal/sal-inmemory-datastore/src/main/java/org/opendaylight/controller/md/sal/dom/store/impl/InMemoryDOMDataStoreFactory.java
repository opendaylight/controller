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
import org.opendaylight.controller.sal.core.spi.data.statistics.DOMStoreStatsTracker;
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;
import org.opendaylight.yangtools.util.PropertyUtils;

import com.google.common.util.concurrent.MoreExecutors;

/**
 * A factory for creating InMemoryDOMDataStore instances.
 *
 * @author Thomas Pantelis
 */
public final class InMemoryDOMDataStoreFactory {

    private static final String DCL_EXECUTOR_MAX_QUEUE_SIZE_PROP =
            "mdsal.datastore-dcl-notification-queue.size";
    private static final int DEFAULT_DCL_EXECUTOR_MAX_QUEUE_SIZE = 1000;

    private static final String DCL_EXECUTOR_MAX_POOL_SIZE_PROP =
            "mdsal.datastore-dcl-notification-pool.size";
    private static final int DEFAULT_DCL_EXECUTOR_MAX_POOL_SIZE = 20;

    private static final String DOM_STORE_EXECUTOR_MAX_QUEUE_SIZE_PROP =
            "mdsal.datastore-executor-queue.size";
    private static final int DEFAULT_DOM_STORE_EXECUTOR_MAX_QUEUE_SIZE = 5000;

    private InMemoryDOMDataStoreFactory() {
    }

    /**
     * Creates an InMemoryDOMDataStore instance.
     *
     * @param name the name of the data store
     * @param schemaService the SchemaService to which to register the data store.
     * @return an InMemoryDOMDataStore instance
     */
    public static InMemoryDOMDataStore create(final String name,
            @Nullable final SchemaService schemaService) {
        return create(name, schemaService, null);
    }

    /**
     * Creates an InMemoryDOMDataStore instance.
     *
     * @param name the name of the data store
     * @param schemaService the SchemaService to which to register the data store.
     * @param statsTracker the stats tracker for the data store.
     * @return an InMemoryDOMDataStore instance
     */
    public static InMemoryDOMDataStore create(final String name,
            @Nullable final SchemaService schemaService,
            @Nullable final DOMStoreStatsTracker statsTracker ) {

        // For DataChangeListener notifications we use an executor that provides the fastest
        // task execution time to get higher throughput as DataChangeListeners typically provide
        // much of the business logic for a data model. If the executor queue size limit is reached,
        // subsequent submitted notifications will block the calling thread.

        int dclExecutorMaxQueueSize = PropertyUtils.getIntSystemProperty(
                DCL_EXECUTOR_MAX_QUEUE_SIZE_PROP, DEFAULT_DCL_EXECUTOR_MAX_QUEUE_SIZE);
        int dclExecutorMaxPoolSize = PropertyUtils.getIntSystemProperty(
                DCL_EXECUTOR_MAX_POOL_SIZE_PROP, DEFAULT_DCL_EXECUTOR_MAX_POOL_SIZE);

        ExecutorService dataChangeListenerExecutor = SpecialExecutors.newBlockingBoundedFastThreadPool(
                dclExecutorMaxPoolSize, dclExecutorMaxQueueSize, name + "-DCL" );

        ExecutorService domStoreExecutor = SpecialExecutors.newBoundedSingleThreadExecutor(
                PropertyUtils.getIntSystemProperty(
                        DOM_STORE_EXECUTOR_MAX_QUEUE_SIZE_PROP,
                        DEFAULT_DOM_STORE_EXECUTOR_MAX_QUEUE_SIZE), "DOMStore-" + name );

        InMemoryDOMDataStore dataStore = new InMemoryDOMDataStore(name,
                MoreExecutors.listeningDecorator(domStoreExecutor),
                dataChangeListenerExecutor);

        if(schemaService != null) {
            schemaService.registerSchemaContextListener(dataStore);
        }

        if(statsTracker != null) {
            statsTracker.setDataChangeListenerExecutor(dataChangeListenerExecutor);
            statsTracker.setDataStoreExecutor(domStoreExecutor);
        }

        return dataStore;
    }
}
