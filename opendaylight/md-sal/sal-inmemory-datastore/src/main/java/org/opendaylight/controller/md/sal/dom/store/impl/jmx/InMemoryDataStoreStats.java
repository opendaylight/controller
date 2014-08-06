/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.dom.store.impl.jmx;

import java.util.concurrent.ExecutorService;

import org.opendaylight.controller.sal.core.spi.data.statistics.DOMStoreStatsTracker;
import org.opendaylight.yangtools.util.concurrent.QueuedNotificationManager;
import org.opendaylight.yangtools.util.jmx.QueuedNotificationManagerMXBeanImpl;
import org.opendaylight.yangtools.util.jmx.ThreadExecutorStatsMXBeanImpl;

/**
 * Wrapper class for data store MXbeans.
 *
 * @author Thomas Pantelis
 */
public class InMemoryDataStoreStats implements DOMStoreStatsTracker, AutoCloseable {

    private final String mBeanType;
    private ThreadExecutorStatsMXBeanImpl notificationExecutorStatsBean;
    private ThreadExecutorStatsMXBeanImpl dataStoreExecutorStatsBean;
    private QueuedNotificationManagerMXBeanImpl notificationManagerStatsBean;

    public InMemoryDataStoreStats(String mBeanType) {
        super();
        this.mBeanType = mBeanType;
    }

    @Override
    public void setDataChangeListenerExecutor(ExecutorService dclExecutor) {
        this.notificationExecutorStatsBean = new ThreadExecutorStatsMXBeanImpl(dclExecutor,
                "notification-executor", mBeanType, null);
        this.notificationExecutorStatsBean.registerMBean();
    }

    @Override
    public void setDataStoreExecutor(ExecutorService dsExecutor) {
        this.dataStoreExecutorStatsBean = new ThreadExecutorStatsMXBeanImpl(dsExecutor,
                "data-store-executor", mBeanType, null);
        this.dataStoreExecutorStatsBean.registerMBean();
    }

    @Override
    public void setNotificationManager(QueuedNotificationManager<?, ?> manager) {
        this.notificationManagerStatsBean = new QueuedNotificationManagerMXBeanImpl(manager,
                "notification-manager", mBeanType, null);
        notificationManagerStatsBean.registerMBean();
    }

    @Override
    public void close() throws Exception {
        if(notificationExecutorStatsBean != null) {
            notificationExecutorStatsBean.unregisterMBean();
        }

        if(dataStoreExecutorStatsBean != null) {
            dataStoreExecutorStatsBean.unregisterMBean();
        }

        if(notificationManagerStatsBean != null) {
            notificationManagerStatsBean.unregisterMBean();
        }
    }
}
