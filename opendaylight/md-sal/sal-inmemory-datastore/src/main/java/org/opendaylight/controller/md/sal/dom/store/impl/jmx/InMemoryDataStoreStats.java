/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.dom.store.impl.jmx;

import org.opendaylight.controller.md.sal.common.util.jmx.QueuedNotificationManagerMXBeanImpl;
import org.opendaylight.controller.md.sal.common.util.jmx.ThreadExecutorStatsMXBeanImpl;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.controller.sal.core.spi.data.statistics.DOMStoreTransactionStatsMXBeanImpl;
import org.opendaylight.yangtools.util.concurrent.QueuedNotificationManager;

/**
 * Wrapper class for data store MXbeans.
 *
 * @author Thomas Pantelis
 */
public class InMemoryDataStoreStats implements AutoCloseable {

    private final ThreadExecutorStatsMXBeanImpl notificationExecutorStatsBean;
    private final ThreadExecutorStatsMXBeanImpl dataStoreExecutorStatsBean;
    private final QueuedNotificationManagerMXBeanImpl notificationManagerStatsBean;
    private final DOMStoreTransactionStatsMXBeanImpl txStatsBean;

    public InMemoryDataStoreStats(String mBeanType, InMemoryDOMDataStore dataStore) {

        QueuedNotificationManager<?, ?> manager = dataStore.getDataChangeListenerNotificationManager();

        this.notificationManagerStatsBean = new QueuedNotificationManagerMXBeanImpl(manager,
                "NotificationManagerStats", mBeanType, null);
        notificationManagerStatsBean.registerMBean();

        this.notificationExecutorStatsBean = new ThreadExecutorStatsMXBeanImpl(manager.getExecutor(),
                "NotificationExecutorStats", mBeanType, null);
        this.notificationExecutorStatsBean.registerMBean();

        this.dataStoreExecutorStatsBean = new ThreadExecutorStatsMXBeanImpl(
                dataStore.getDomStoreExecutor(), "DatastoreExecutorStats", mBeanType, null);
        this.dataStoreExecutorStatsBean.registerMBean();

        this.txStatsBean = new DOMStoreTransactionStatsMXBeanImpl(
                dataStore.getDOMStoreTransactionStatsTracker(), "TransactionStats", mBeanType, null);
        txStatsBean.registerMBean();
    }

    @Override
    public void close() {
        notificationExecutorStatsBean.unregisterMBean();
        dataStoreExecutorStatsBean.unregisterMBean();
        notificationManagerStatsBean.unregisterMBean();
        txStatsBean.unregisterMBean();
    }
}
