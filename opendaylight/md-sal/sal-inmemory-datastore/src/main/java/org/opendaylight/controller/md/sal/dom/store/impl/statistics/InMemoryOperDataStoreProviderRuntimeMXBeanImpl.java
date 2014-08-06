/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.dom.store.impl.statistics;

import org.opendaylight.controller.config.yang.inmemory_datastore_provider.InMemoryOperationalDataStoreProviderRuntimeMXBean;
import org.opendaylight.controller.config.yang.inmemory_datastore_provider.OperationalStoreExecutorStats;
import org.opendaylight.controller.config.yang.inmemory_datastore_provider.OperationalStoreNotificationExecutorStats;

public class InMemoryOperDataStoreProviderRuntimeMXBeanImpl
        extends AbstractInMemoryDataStoreProviderRuntimeMXBeanImpl<OperationalStoreExecutorStats,
                                                        OperationalStoreNotificationExecutorStats>
        implements InMemoryOperationalDataStoreProviderRuntimeMXBean {

    public InMemoryOperDataStoreProviderRuntimeMXBeanImpl() {
        super(OperationalStoreExecutorStats.class, OperationalStoreNotificationExecutorStats.class);
    }

    @Override
    public OperationalStoreExecutorStats getOperationalStoreExecutorStats() {
        return super.getDataStoreExecutorStats();
    }

    @Override
    public OperationalStoreNotificationExecutorStats getOperationalStoreNotificationExecutorStats() {
        return super.getDataStoreNotificationExecutorStats();
    }
}
