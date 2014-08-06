/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.dom.store.impl.statistics;

import java.util.concurrent.ExecutorService;

import org.opendaylight.controller.sal.core.api.statistics.ThreadExecutorStatsHelper;
import org.opendaylight.controller.sal.core.spi.data.statistics.DOMStoreStatsTracker;

/**
 * Abstract base for a class that provides the JMX data for adata store.
 *
 * @author Thomas Pantelis
 */
public class AbstractInMemoryDataStoreProviderRuntimeMXBeanImpl<E,N> implements DOMStoreStatsTracker {

    private ExecutorService dataStoreNotificationExecutor;
    private ExecutorService dataStoreExecutor;
    private final Class<E> dataStoreExecutorStatsClass;
    private final Class<N> dataStoreNotificationStatsClass;

    protected AbstractInMemoryDataStoreProviderRuntimeMXBeanImpl(
            Class<E> dataStoreExecutorStatsClass,
            Class<N> dataStoreNotificationStatsClass ) {
        this.dataStoreExecutorStatsClass = dataStoreExecutorStatsClass;
        this.dataStoreNotificationStatsClass = dataStoreNotificationStatsClass;
    }

    @Override
    public void setDataChangeListenerExecutor(ExecutorService dclExecutor) {
        dataStoreNotificationExecutor = dclExecutor;
    }

    @Override
    public void setDataStoreExecutor(ExecutorService dsExecutor) {
        dataStoreExecutor = dsExecutor;
    }

    protected N getDataStoreNotificationExecutorStats() {
        return ThreadExecutorStatsHelper.newStatsInstance(
                dataStoreNotificationExecutor, dataStoreNotificationStatsClass);
    }

    protected E getDataStoreExecutorStats() {
        return ThreadExecutorStatsHelper.newStatsInstance(
                dataStoreExecutor, dataStoreExecutorStatsClass);
    }
}
