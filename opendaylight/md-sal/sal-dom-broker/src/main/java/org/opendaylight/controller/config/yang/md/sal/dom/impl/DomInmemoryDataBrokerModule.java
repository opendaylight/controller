/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.md.sal.dom.impl;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
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

        /*
         * We use a single-threaded executor for commits with a bounded queue capacity. If the
         * queue capacity is reached, subsequent commit tasks will be rejected and the commits will
         * fail. This is done to relieve back pressure. This should be an extreme scenario - either
         * there's deadlock(s) somewhere and the controller is unstable or some rogue component is
         * continuously hammering commits too fast or the controller is just over-capacity for the
         * system it's running on.
         */
        ExecutorService commitExecutor = SpecialExecutors.newBoundedSingleThreadExecutor(
                getMaxDataBrokerCommitQueueSize(), "WriteTxCommit");

        /*
         * We use an executor for commit ListenableFuture callbacks that favors reusing available
         * threads over creating new threads at the expense of execution time. The assumption is
         * that most ListenableFuture callbacks won't execute a lot of business logic where we want
         * it to run quicker - many callbacks will likely just handle error conditions and do
         * nothing on success. The executor queue capacity is bounded and, if the capacity is
         * reached, subsequent submitted tasks will block the caller.
         */
        Executor listenableFutureExecutor = SpecialExecutors.newBlockingBoundedCachedThreadPool(
                getMaxDataBrokerFutureCallbackPoolSize(), getMaxDataBrokerFutureCallbackQueueSize(),
                "CommitFutures");

        DOMDataBrokerImpl newDataBroker = new DOMDataBrokerImpl(datastores,
                new DeadlockDetectingListeningExecutorService(commitExecutor,
                    TransactionCommitDeadlockException.DEADLOCK_EXECUTOR_FUNCTION,
                    listenableFutureExecutor));

        return newDataBroker;
    }
}
