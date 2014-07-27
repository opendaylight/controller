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

import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * A factory for creating InMemoryDOMDataStore instances.
 *
 * @author Thomas Pantelis
 */
public final class InMemoryDOMDataStoreFactory {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryDOMDataStoreFactory.class);

    private static final String DCL_EXECUTOR_MAX_QUEUE_SIZE_PROP =
            "mdsal.datastore-dcl-notification-queue.size";
    private static final int DEFAULT_DCL_EXECUTOR_MAX_QUEUE_SIZE = 1000;

    private static final String DCL_EXECUTOR_MAX_POOL_SIZE_PROP =
            "mdsal.datastore-dcl-notification-pool.size";
    private static final int DEFAULT_DCL_EXECUTOR_MAX_POOL_SIZE = 20;

    private InMemoryDOMDataStoreFactory() {
    }

    public static int getIntSystemProperty( String propName, int defaultValue ) {
        // TODO - this should really be in some general utility class.
        int propValue = defaultValue;
        String strValue = System.getProperty(propName);
        if (!Strings.isNullOrEmpty(strValue) && !strValue.trim().isEmpty() ) {
            try {
                propValue = Integer.parseInt(strValue);
            } catch (NumberFormatException e) {
                LOG.warn("Cannot parse value {} for system property {}, using default {}",
                         strValue, propName, defaultValue);
            }
        }

        return propValue;
    }

    /**
     * Creates an InMemoryDOMDataStore instance.
     *
     * @param name the name of the data store
     * @param schemaService the SchemaService to which to register the data store.
     * @return an InMemoryDOMDataStore instance
     */
    public static InMemoryDOMDataStore create(final String name, final SchemaService schemaService) {

        // For DataChangeListener notifications we use an executor that provides the fastest
        // task execution time to get higher throughput as DataChangeListeners typically provide
        // much of the business logic for a data model. If the executor queue size limit is reached,
        // subsequent submitted notifications will block the calling thread.

        int dclExecutorMaxQueueSize = getIntSystemProperty(DCL_EXECUTOR_MAX_QUEUE_SIZE_PROP,
                DEFAULT_DCL_EXECUTOR_MAX_QUEUE_SIZE);
        int dclExecutorMaxPoolSize = getIntSystemProperty(DCL_EXECUTOR_MAX_POOL_SIZE_PROP,
                DEFAULT_DCL_EXECUTOR_MAX_POOL_SIZE);
        ExecutorService dataChangeListenerExecutor = SpecialExecutors.newBlockingBoundedFastThreadPool(
                dclExecutorMaxPoolSize, dclExecutorMaxQueueSize, name + "-DCL" );

        InMemoryDOMDataStore dataStore = new InMemoryDOMDataStore(name,
                MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor()),
                dataChangeListenerExecutor);

        schemaService.registerSchemaServiceListener(dataStore);
        return dataStore;
    }
}
