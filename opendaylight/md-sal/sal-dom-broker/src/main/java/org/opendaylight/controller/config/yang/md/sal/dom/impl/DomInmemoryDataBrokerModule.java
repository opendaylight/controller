/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.md.sal.dom.impl;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitDeadlockException;
import org.opendaylight.controller.md.sal.dom.broker.impl.DOMDataBrokerImpl;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStoreFactory;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.yangtools.util.concurrent.DeadlockDetectingListeningExecutorService;
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;
import com.google.common.collect.ImmutableMap;

/**
*
*/
public final class DomInmemoryDataBrokerModule extends
        org.opendaylight.controller.config.yang.md.sal.dom.impl.AbstractDomInmemoryDataBrokerModule {

    private static final String FUTURE_CALLBACK_EXECUTOR_MAX_QUEUE_SIZE_PROP =
            "mdsal.datastore-future-callback-queue.size";
    private static final int DEFAULT_FUTURE_CALLBACK_EXECUTOR_MAX_QUEUE_SIZE = 1000;

    private static final String FUTURE_CALLBACK_EXECUTOR_MAX_POOL_SIZE_PROP =
            "mdsal.datastore-future-callback-pool.size";
    private static final int DEFAULT_FUTURE_CALLBACK_EXECUTOR_MAX_POOL_SIZE = 20;

    public DomInmemoryDataBrokerModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public DomInmemoryDataBrokerModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            final org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            final DomInmemoryDataBrokerModule oldModule, final java.lang.AutoCloseable oldInstance) {

        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    protected void customValidation() {
        // Add custom validation for module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        //Initializing Operational DOM DataStore defaulting to InMemoryDOMDataStore if one is not configured
        DOMStore operStore =  getOperationalDataStoreDependency();
        if(operStore == null){
           //we will default to InMemoryDOMDataStore creation
          operStore = InMemoryDOMDataStoreFactory.create("DOM-OPER", getSchemaServiceDependency());
        }

        DOMStore configStore = getConfigDataStoreDependency();
        if(configStore == null){
           //we will default to InMemoryDOMDataStore creation
           configStore = InMemoryDOMDataStoreFactory.create("DOM-CFG", getSchemaServiceDependency());
        }
        ImmutableMap<LogicalDatastoreType, DOMStore> datastores = ImmutableMap
                .<LogicalDatastoreType, DOMStore> builder().put(LogicalDatastoreType.OPERATIONAL, operStore)
                .put(LogicalDatastoreType.CONFIGURATION, configStore).build();

        // We use an executor for ListenableFuture callbacks that favors reusing available threads
        // over creating new threads at the expense of execution time. The assumption is that most
        // ListenableFuture callbacks won't execute a lot of business logic where we want it to run
        // quicker - many callbacks will likely just handle error conditions and do nothing on
        // success.
        Executor listenableFutureExecutor = SpecialExecutors.newBlockingBoundedCachedThreadPool(
                InMemoryDOMDataStoreFactory.getIntSystemProperty(
                        FUTURE_CALLBACK_EXECUTOR_MAX_POOL_SIZE_PROP,
                        DEFAULT_FUTURE_CALLBACK_EXECUTOR_MAX_POOL_SIZE),
                InMemoryDOMDataStoreFactory.getIntSystemProperty(
                        FUTURE_CALLBACK_EXECUTOR_MAX_QUEUE_SIZE_PROP,
                        DEFAULT_FUTURE_CALLBACK_EXECUTOR_MAX_QUEUE_SIZE), "CommitFuture");

        DOMDataBrokerImpl newDataBroker = new DOMDataBrokerImpl(datastores,
                new DeadlockDetectingListeningExecutorService(Executors.newSingleThreadExecutor(),
                    TransactionCommitDeadlockException.DEADLOCK_EXECUTOR_FUNCTION,
                    listenableFutureExecutor));

        return newDataBroker;
    }
}
