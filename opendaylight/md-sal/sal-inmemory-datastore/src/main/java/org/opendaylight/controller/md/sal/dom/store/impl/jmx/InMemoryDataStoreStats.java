/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.dom.store.impl.jmx;

import org.opendaylight.controller.md.sal.common.util.jmx.AbstractMXBean;
import org.opendaylight.controller.md.sal.common.util.jmx.QueuedNotificationManagerMXBeanImpl;
import org.opendaylight.controller.md.sal.common.util.jmx.ThreadExecutorStatsMXBeanImpl;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.yangtools.util.concurrent.QueuedNotificationManager;

/**
 * Wrapper class for data store MXbeans.
 *
 * @author Thomas Pantelis
 */
public class InMemoryDataStoreStats implements AutoCloseable {

    private final AbstractMXBean notificationExecutorStatsBean;
    private final QueuedNotificationManagerMXBeanImpl notificationManagerStatsBean;

    public InMemoryDataStoreStats(final String mBeanType, final QueuedNotificationManager<?, ?> manager) {

        notificationManagerStatsBean = new QueuedNotificationManagerMXBeanImpl(manager,
                "notification-manager", mBeanType, null);
        notificationManagerStatsBean.registerMBean();

        notificationExecutorStatsBean = ThreadExecutorStatsMXBeanImpl.create(manager.getExecutor(),
                "notification-executor", mBeanType, null);
        if (notificationExecutorStatsBean != null) {
            notificationExecutorStatsBean.registerMBean();
        }
    }

    public InMemoryDataStoreStats(final String name, final InMemoryDOMDataStore dataStore) {
        this(name, dataStore.getDataChangeListenerNotificationManager());
    }

    @Override
    public void close() throws Exception {
        if(notificationExecutorStatsBean != null) {
            notificationExecutorStatsBean.unregisterMBean();
        }

        if(notificationManagerStatsBean != null) {
            notificationManagerStatsBean.unregisterMBean();
        }
    }
}
