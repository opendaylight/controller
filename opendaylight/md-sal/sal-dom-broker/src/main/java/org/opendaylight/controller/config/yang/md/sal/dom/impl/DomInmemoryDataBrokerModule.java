/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.md.sal.dom.impl;

import com.google.common.collect.Lists;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitDeadlockException;
import org.opendaylight.controller.md.sal.common.util.jmx.AbstractMXBean;
import org.opendaylight.controller.md.sal.common.util.jmx.ThreadExecutorStatsMXBeanImpl;
import org.opendaylight.controller.md.sal.dom.broker.impl.SerializedDOMDataBroker;
import org.opendaylight.controller.md.sal.dom.broker.impl.jmx.CommitStatsMXBeanImpl;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStoreFactory;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.yangtools.util.DurationStatisticsTracker;
import org.opendaylight.yangtools.util.concurrent.DeadlockDetectingListeningExecutorService;
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;

/**
*
*/
public final class DomInmemoryDataBrokerModule extends
        org.opendaylight.controller.config.yang.md.sal.dom.impl.AbstractDomInmemoryDataBrokerModule {

    private static final String JMX_BEAN_TYPE = "DOMDataBroker";

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
          operStore = InMemoryDOMDataStoreFactory.create("DOM-OPER", false, getSchemaServiceDependency());
        }

        DOMStore configStore = getConfigDataStoreDependency();
        if(configStore == null){
           //we will default to InMemoryDOMDataStore creation
           configStore = InMemoryDOMDataStoreFactory.create("DOM-CFG", true, getSchemaServiceDependency());
        }

        final Map<LogicalDatastoreType, DOMStore> datastores = new EnumMap<>(LogicalDatastoreType.class);
        datastores.put(LogicalDatastoreType.OPERATIONAL, operStore);
        datastores.put(LogicalDatastoreType.CONFIGURATION, configStore);

        /*
         * We use an executor for commit ListenableFuture callbacks that favors reusing available
         * threads over creating new threads at the expense of execution time. The assumption is
         * that most ListenableFuture callbacks won't execute a lot of business logic where we want
         * it to run quicker - many callbacks will likely just handle error conditions and do
         * nothing on success. The executor queue capacity is bounded and, if the capacity is
         * reached, subsequent submitted tasks will block the caller.
         */
        ExecutorService listenableFutureExecutor = SpecialExecutors.newBlockingBoundedCachedThreadPool(
                getMaxDataBrokerFutureCallbackPoolSize(), getMaxDataBrokerFutureCallbackQueueSize(),
                "CommitFutures");

        final List<AbstractMXBean> mBeans = Lists.newArrayList();
        final DurationStatisticsTracker commitStatsTracker;

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

        SerializedDOMDataBroker sdb = new SerializedDOMDataBroker(datastores,
            new DeadlockDetectingListeningExecutorService(commitExecutor,
                TransactionCommitDeadlockException.DEADLOCK_EXCEPTION_SUPPLIER,
                listenableFutureExecutor));
        commitStatsTracker = sdb.getCommitStatsTracker();

        final AbstractMXBean commitExecutorStatsMXBean =
                ThreadExecutorStatsMXBeanImpl.create(commitExecutor, "CommitExecutorStats",
                    JMX_BEAN_TYPE, null);
        if(commitExecutorStatsMXBean != null) {
            mBeans.add(commitExecutorStatsMXBean);
        }

        if(commitStatsTracker != null) {
            final CommitStatsMXBeanImpl commitStatsMXBean = new CommitStatsMXBeanImpl(
                    commitStatsTracker, JMX_BEAN_TYPE);
            commitStatsMXBean.registerMBean();
            mBeans.add(commitStatsMXBean);
        }

        final AbstractMXBean commitFutureStatsMXBean =
                ThreadExecutorStatsMXBeanImpl.create(listenableFutureExecutor,
                        "CommitFutureExecutorStats", JMX_BEAN_TYPE, null);
        if(commitFutureStatsMXBean != null) {
            mBeans.add(commitFutureStatsMXBean);
        }

        sdb.setCloseable(new AutoCloseable() {
            @Override
            public void close() {
                for(AbstractMXBean mBean: mBeans) {
                    mBean.unregisterMBean();
                }
            }
        });

        return sdb;
    }
}
