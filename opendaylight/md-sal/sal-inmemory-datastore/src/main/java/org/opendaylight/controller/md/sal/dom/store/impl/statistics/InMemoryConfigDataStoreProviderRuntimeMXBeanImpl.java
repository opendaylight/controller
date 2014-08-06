/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.dom.store.impl.statistics;

import org.opendaylight.controller.config.yang.inmemory_datastore_provider.ConfigStoreExecutorStats;
import org.opendaylight.controller.config.yang.inmemory_datastore_provider.ConfigStoreNotificationExecutorStats;
import org.opendaylight.controller.config.yang.inmemory_datastore_provider.InMemoryConfigDataStoreProviderRuntimeMXBean;

/**
 * Implementation of the yang-generated InMemoryConfigDataStoreProviderRuntimeMXBean
 * interface that provides the JMX data for the config data store.
 *
 * @author Thomas Pantelis
 */
public class InMemoryConfigDataStoreProviderRuntimeMXBeanImpl
        extends AbstractInMemoryDataStoreProviderRuntimeMXBeanImpl<ConfigStoreExecutorStats,
                                                            ConfigStoreNotificationExecutorStats>
        implements InMemoryConfigDataStoreProviderRuntimeMXBean {

    public InMemoryConfigDataStoreProviderRuntimeMXBeanImpl() {
        super(ConfigStoreExecutorStats.class, ConfigStoreNotificationExecutorStats.class);
    }

    @Override
    public ConfigStoreNotificationExecutorStats getConfigStoreNotificationExecutorStats() {
        return super.getDataStoreNotificationExecutorStats();
    }

    @Override
    public ConfigStoreExecutorStats getConfigStoreExecutorStats() {
        return super.getDataStoreExecutorStats();
    }
}
