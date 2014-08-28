/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.dom.store.impl.jmx;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.opendaylight.controller.md.sal.common.util.jmx.MBeanRegistrar;
import org.opendaylight.controller.md.sal.common.util.jmx.QueuedNotificationManagerMXBeanImpl;
import org.opendaylight.controller.md.sal.common.util.jmx.ThreadExecutorStatsMXBeanImpl;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.yangtools.util.concurrent.QueuedNotificationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper class for data store MXbeans.
 *
 * @author Thomas Pantelis
 */
public class InMemoryDataStoreStats implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryDataStoreStats.class);

    private final ThreadExecutorStatsMXBeanImpl notificationExecutorStatsBean;
    private final ThreadExecutorStatsMXBeanImpl dataStoreExecutorStatsBean;
    private final QueuedNotificationManagerMXBeanImpl notificationManagerStatsBean;
    private final ObjectName statsMBeanON;

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

        ObjectName localStatsMBeanON = null;
        try {
            localStatsMBeanON = MBeanRegistrar.buildMBeanObjectName(
                    "TransactionStats", mBeanType, null);
            MBeanRegistrar.registerMBean(dataStore.getDOMStoreTransactionStatsMXBean(),
                    localStatsMBeanON);
        } catch(MalformedObjectNameException e) {
            LOG.error("Error building MBean ObjectName", e);
        }

        statsMBeanON = localStatsMBeanON;
    }

    @Override
    public void close() {
        notificationExecutorStatsBean.unregisterMBean();
        dataStoreExecutorStatsBean.unregisterMBean();
        notificationManagerStatsBean.unregisterMBean();
        MBeanRegistrar.unregisterMBean(statsMBeanON);
    }
}
