/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.dom.store.impl.jmx;

import java.util.concurrent.ExecutorService;
import org.opendaylight.controller.md.sal.common.util.jmx.AbstractMXBean;
import org.opendaylight.controller.md.sal.common.util.jmx.QueuedNotificationManagerMXBeanImpl;
import org.opendaylight.controller.md.sal.common.util.jmx.ThreadExecutorStatsMXBeanImpl;
import org.opendaylight.yangtools.util.concurrent.QueuedNotificationManager;

/**
 * Wrapper class for data store MXbeans.
 *
 * @author Thomas Pantelis
 */
public class InMemoryDataStoreStats implements AutoCloseable {

    private final AbstractMXBean notificationExecutorStatsBean;
    private final AbstractMXBean dataStoreExecutorStatsBean;
    private final QueuedNotificationManagerMXBeanImpl notificationManagerStatsBean;

    public InMemoryDataStoreStats(final String mBeanType, final QueuedNotificationManager<?, ?> manager,
            final ExecutorService dataStoreExecutor) {

        notificationManagerStatsBean = new QueuedNotificationManagerMXBeanImpl(manager,
                "notification-manager", mBeanType, null);
        notificationManagerStatsBean.registerMBean();

        notificationExecutorStatsBean = ThreadExecutorStatsMXBeanImpl.create(manager.getExecutor(),
                "notification-executor", mBeanType, null);
        if (notificationExecutorStatsBean != null) {
            notificationExecutorStatsBean.registerMBean();
        }

        dataStoreExecutorStatsBean = ThreadExecutorStatsMXBeanImpl.create(dataStoreExecutor,
                "data-store-executor", mBeanType, null);
        if (dataStoreExecutorStatsBean != null) {
            dataStoreExecutorStatsBean.registerMBean();
        }
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
