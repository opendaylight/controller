/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import org.opendaylight.controller.cluster.raft.ConfigParams;
import org.opendaylight.controller.cluster.raft.DefaultConfigParamsImpl;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStoreConfigProperties;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.util.concurrent.TimeUnit;

/**
 * Contains contextual data for a data store.
 *
 * @author Thomas Pantelis
 */
public class DatastoreContext {

    private final InMemoryDOMDataStoreConfigProperties dataStoreProperties;
    private final Duration shardTransactionIdleTimeout;
    private final int operationTimeoutInSeconds;
    private final String dataStoreMXBeanType;
    private final ConfigParams shardRaftConfig;

    public DatastoreContext() {
        this("DistributedDatastore", null, Duration.create(10, TimeUnit.MINUTES), 5, 1000, 20000, 500);
    }

    public DatastoreContext(String dataStoreMXBeanType,
            InMemoryDOMDataStoreConfigProperties dataStoreProperties,
            Duration shardTransactionIdleTimeout,
            int operationTimeoutInSeconds,
            int shardJournalRecoveryLogBatchSize,
            int shardSnapshotBatchCount,
            int shardHeartbeatIntervalInMillis) {
        this.dataStoreMXBeanType = dataStoreMXBeanType;
        this.dataStoreProperties = dataStoreProperties;
        this.shardTransactionIdleTimeout = shardTransactionIdleTimeout;
        this.operationTimeoutInSeconds = operationTimeoutInSeconds;

        DefaultConfigParamsImpl raftConfig = new DefaultConfigParamsImpl();
        raftConfig.setHeartBeatInterval(new FiniteDuration(shardHeartbeatIntervalInMillis,
                TimeUnit.MILLISECONDS));
        raftConfig.setJournalRecoveryLogBatchSize(shardJournalRecoveryLogBatchSize);
        raftConfig.setSnapshotBatchCount(shardSnapshotBatchCount);
        shardRaftConfig = raftConfig;
    }

    public InMemoryDOMDataStoreConfigProperties getDataStoreProperties() {
        return dataStoreProperties;
    }

    public Duration getShardTransactionIdleTimeout() {
        return shardTransactionIdleTimeout;
    }

    public String getDataStoreMXBeanType() {
        return dataStoreMXBeanType;
    }

    public int getOperationTimeoutInSeconds() {
        return operationTimeoutInSeconds;
    }

    public ConfigParams getShardRaftConfig() {
        return shardRaftConfig;
    }
}
