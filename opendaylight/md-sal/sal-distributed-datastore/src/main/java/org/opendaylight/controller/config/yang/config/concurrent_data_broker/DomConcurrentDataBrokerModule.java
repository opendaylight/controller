/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.config.concurrent_data_broker;

import com.google.common.collect.Lists;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import org.opendaylight.controller.cluster.datastore.ConcurrentDOMDataBroker;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.util.jmx.AbstractMXBean;
import org.opendaylight.controller.md.sal.common.util.jmx.ThreadExecutorStatsMXBeanImpl;
import org.opendaylight.controller.md.sal.dom.broker.impl.jmx.CommitStatsMXBeanImpl;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStoreFactory;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.yangtools.util.DurationStatisticsTracker;
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;

public class DomConcurrentDataBrokerModule extends AbstractDomConcurrentDataBrokerModule {
    private static final String JMX_BEAN_TYPE = "DOMDataBroker";

    public DomConcurrentDataBrokerModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public DomConcurrentDataBrokerModule(final org.opendaylight.controller.config.api.ModuleIdentifier identifier, final DependencyResolver dependencyResolver, final DomConcurrentDataBrokerModule oldModule, final AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public AutoCloseable createInstance() {
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
        final ConcurrentDOMDataBroker cdb = new ConcurrentDOMDataBroker(datastores, listenableFutureExecutor);
        commitStatsTracker = cdb.getCommitStatsTracker();

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

        cdb.setCloseable(new AutoCloseable() {
            @Override
            public void close() {
                for(AbstractMXBean mBean: mBeans) {
                    mBean.unregisterMBean();
                }
            }
        });

        return cdb;
    }
}
