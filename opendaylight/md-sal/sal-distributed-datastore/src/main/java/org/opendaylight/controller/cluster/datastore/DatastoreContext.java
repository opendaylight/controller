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
import akka.util.Timeout;
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
    private final int shardTransactionCommitTimeoutInSeconds;
    private final int shardTransactionCommitQueueCapacity;
    private final Timeout shardInitializationTimeout;

    private DatastoreContext(InMemoryDOMDataStoreConfigProperties dataStoreProperties,
            ConfigParams shardRaftConfig, String dataStoreMXBeanType, int operationTimeoutInSeconds,
            Duration shardTransactionIdleTimeout, int shardTransactionCommitTimeoutInSeconds,
            int shardTransactionCommitQueueCapacity, Timeout shardInitializationTimeout) {
        this.dataStoreProperties = dataStoreProperties;
        this.shardRaftConfig = shardRaftConfig;
        this.dataStoreMXBeanType = dataStoreMXBeanType;
        this.operationTimeoutInSeconds = operationTimeoutInSeconds;
        this.shardTransactionIdleTimeout = shardTransactionIdleTimeout;
        this.shardTransactionCommitTimeoutInSeconds = shardTransactionCommitTimeoutInSeconds;
        this.shardTransactionCommitQueueCapacity = shardTransactionCommitQueueCapacity;
        this.shardInitializationTimeout = shardInitializationTimeout;
    }

    public static Builder newBuilder() {
        return new Builder();
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

    public int getShardTransactionCommitTimeoutInSeconds() {
        return shardTransactionCommitTimeoutInSeconds;
    }

    public int getShardTransactionCommitQueueCapacity() {
        return shardTransactionCommitQueueCapacity;
    }

    public Timeout getShardInitializationTimeout() {
        return shardInitializationTimeout;
    }

    public static class Builder {
        private InMemoryDOMDataStoreConfigProperties dataStoreProperties;
        private Duration shardTransactionIdleTimeout = Duration.create(10, TimeUnit.MINUTES);
        private int operationTimeoutInSeconds = 5;
        private String dataStoreMXBeanType;
        private int shardTransactionCommitTimeoutInSeconds = 30;
        private int shardJournalRecoveryLogBatchSize = 1000;
        private int shardSnapshotBatchCount = 20000;
        private int shardHeartbeatIntervalInMillis = 500;
        private int shardTransactionCommitQueueCapacity = 20000;
        private Timeout shardInitializationTimeout = new Timeout(5, TimeUnit.MINUTES);

        public Builder shardTransactionIdleTimeout(Duration shardTransactionIdleTimeout) {
            this.shardTransactionIdleTimeout = shardTransactionIdleTimeout;
            return this;
        }

        public Builder operationTimeoutInSeconds(int operationTimeoutInSeconds) {
            this.operationTimeoutInSeconds = operationTimeoutInSeconds;
            return this;
        }

        public Builder dataStoreMXBeanType(String dataStoreMXBeanType) {
            this.dataStoreMXBeanType = dataStoreMXBeanType;
            return this;
        }

        public Builder dataStoreProperties(InMemoryDOMDataStoreConfigProperties dataStoreProperties) {
            this.dataStoreProperties = dataStoreProperties;
            return this;
        }

        public Builder shardTransactionCommitTimeoutInSeconds(int shardTransactionCommitTimeoutInSeconds) {
            this.shardTransactionCommitTimeoutInSeconds = shardTransactionCommitTimeoutInSeconds;
            return this;
        }

        public Builder shardJournalRecoveryLogBatchSize(int shardJournalRecoveryLogBatchSize) {
            this.shardJournalRecoveryLogBatchSize = shardJournalRecoveryLogBatchSize;
            return this;
        }

        public Builder shardSnapshotBatchCount(int shardSnapshotBatchCount) {
            this.shardSnapshotBatchCount = shardSnapshotBatchCount;
            return this;
        }

        public Builder shardHeartbeatIntervalInMillis(int shardHeartbeatIntervalInMillis) {
            this.shardHeartbeatIntervalInMillis = shardHeartbeatIntervalInMillis;
            return this;
        }

        public Builder shardTransactionCommitQueueCapacity(int shardTransactionCommitQueueCapacity) {
            this.shardTransactionCommitQueueCapacity = shardTransactionCommitQueueCapacity;
            return this;
        }

        public Builder shardInitializationTimeout(long timeout, TimeUnit unit) {
            this.shardInitializationTimeout = new Timeout(timeout, unit);
            return this;
        }

        public DatastoreContext build() {
            DefaultConfigParamsImpl raftConfig = new DefaultConfigParamsImpl();
            raftConfig.setHeartBeatInterval(new FiniteDuration(shardHeartbeatIntervalInMillis,
                    TimeUnit.MILLISECONDS));
            raftConfig.setJournalRecoveryLogBatchSize(shardJournalRecoveryLogBatchSize);
            raftConfig.setSnapshotBatchCount(shardSnapshotBatchCount);

            return new DatastoreContext(dataStoreProperties, raftConfig, dataStoreMXBeanType,
                    operationTimeoutInSeconds, shardTransactionIdleTimeout,
                    shardTransactionCommitTimeoutInSeconds, shardTransactionCommitQueueCapacity,
                    shardInitializationTimeout);
        }
    }
}
