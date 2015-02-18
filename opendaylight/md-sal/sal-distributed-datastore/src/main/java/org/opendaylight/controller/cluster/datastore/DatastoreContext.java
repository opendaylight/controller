/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.util.Timeout;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.text.WordUtils;
import org.opendaylight.controller.cluster.datastore.config.ConfigurationReader;
import org.opendaylight.controller.cluster.datastore.config.FileConfigurationReader;
import org.opendaylight.controller.cluster.raft.ConfigParams;
import org.opendaylight.controller.cluster.raft.DefaultConfigParamsImpl;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStoreConfigProperties;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

/**
 * Contains contextual data for a data store.
 *
 * @author Thomas Pantelis
 */
public class DatastoreContext {

    public static final Duration DEFAULT_SHARD_TRANSACTION_IDLE_TIMEOUT = Duration.create(10, TimeUnit.MINUTES);
    public static final int DEFAULT_OPERATION_TIMEOUT_IN_SECONDS = 5;
    public static final int DEFAULT_SHARD_TX_COMMIT_TIMEOUT_IN_SECONDS = 30;
    public static final int DEFAULT_JOURNAL_RECOVERY_BATCH_SIZE = 1000;
    public static final int DEFAULT_SNAPSHOT_BATCH_COUNT = 20000;
    public static final int DEFAULT_HEARTBEAT_INTERVAL_IN_MILLIS = 500;
    public static final int DEFAULT_ISOLATED_LEADER_CHECK_INTERVAL_IN_MILLIS = DEFAULT_HEARTBEAT_INTERVAL_IN_MILLIS * 10;
    public static final int DEFAULT_SHARD_TX_COMMIT_QUEUE_CAPACITY = 20000;
    public static final Timeout DEFAULT_SHARD_INITIALIZATION_TIMEOUT = new Timeout(5, TimeUnit.MINUTES);
    public static final Timeout DEFAULT_SHARD_LEADER_ELECTION_TIMEOUT = new Timeout(30, TimeUnit.SECONDS);
    public static final boolean DEFAULT_PERSISTENT = true;
    public static final FileConfigurationReader DEFAULT_CONFIGURATION_READER = new FileConfigurationReader();
    public static final int DEFAULT_SHARD_SNAPSHOT_DATA_THRESHOLD_PERCENTAGE = 12;
    public static final int DEFAULT_SHARD_ELECTION_TIMEOUT_FACTOR = 2;
    public static final int DEFAULT_TX_CREATION_INITIAL_RATE_LIMIT = 100;
    public static final String UNKNOWN_DATA_STORE_TYPE = "unknown";

    private InMemoryDOMDataStoreConfigProperties dataStoreProperties;
    private Duration shardTransactionIdleTimeout = DatastoreContext.DEFAULT_SHARD_TRANSACTION_IDLE_TIMEOUT;
    private int operationTimeoutInSeconds = DEFAULT_OPERATION_TIMEOUT_IN_SECONDS;
    private String dataStoreMXBeanType;
    private int shardTransactionCommitTimeoutInSeconds = DEFAULT_SHARD_TX_COMMIT_TIMEOUT_IN_SECONDS;
    private int shardTransactionCommitQueueCapacity = DEFAULT_SHARD_TX_COMMIT_QUEUE_CAPACITY;
    private Timeout shardInitializationTimeout = DEFAULT_SHARD_INITIALIZATION_TIMEOUT;
    private Timeout shardLeaderElectionTimeout = DEFAULT_SHARD_LEADER_ELECTION_TIMEOUT;
    private boolean persistent = DEFAULT_PERSISTENT;
    private ConfigurationReader configurationReader = DEFAULT_CONFIGURATION_READER;
    private long transactionCreationInitialRateLimit = DEFAULT_TX_CREATION_INITIAL_RATE_LIMIT;
    private final DefaultConfigParamsImpl raftConfig = new DefaultConfigParamsImpl();
    private String dataStoreType = UNKNOWN_DATA_STORE_TYPE;

    private DatastoreContext() {
        setShardJournalRecoveryLogBatchSize(DEFAULT_JOURNAL_RECOVERY_BATCH_SIZE);
        setSnapshotBatchCount(DEFAULT_SNAPSHOT_BATCH_COUNT);
        setHeartbeatInterval(DEFAULT_HEARTBEAT_INTERVAL_IN_MILLIS);
        setIsolatedLeaderCheckInterval(DEFAULT_ISOLATED_LEADER_CHECK_INTERVAL_IN_MILLIS);
        setSnapshotDataThresholdPercentage(DEFAULT_SHARD_SNAPSHOT_DATA_THRESHOLD_PERCENTAGE);
        setElectionTimeoutFactor(DEFAULT_SHARD_ELECTION_TIMEOUT_FACTOR);
    }

    private DatastoreContext(DatastoreContext other) {
        this.dataStoreProperties = other.dataStoreProperties;
        this.shardTransactionIdleTimeout = other.shardTransactionIdleTimeout;
        this.operationTimeoutInSeconds = other.operationTimeoutInSeconds;
        this.dataStoreMXBeanType = other.dataStoreMXBeanType;
        this.shardTransactionCommitTimeoutInSeconds = other.shardTransactionCommitTimeoutInSeconds;
        this.shardTransactionCommitQueueCapacity = other.shardTransactionCommitQueueCapacity;
        this.shardInitializationTimeout = other.shardInitializationTimeout;
        this.shardLeaderElectionTimeout = other.shardLeaderElectionTimeout;
        this.persistent = other.persistent;
        this.configurationReader = other.configurationReader;
        this.transactionCreationInitialRateLimit = other.transactionCreationInitialRateLimit;
        this.dataStoreType = other.dataStoreType;

        setShardJournalRecoveryLogBatchSize(other.raftConfig.getJournalRecoveryLogBatchSize());
        setSnapshotBatchCount(other.raftConfig.getSnapshotBatchCount());
        setHeartbeatInterval(other.raftConfig.getHeartBeatInterval().toMillis());
        setIsolatedLeaderCheckInterval(other.raftConfig.getIsolatedCheckIntervalInMillis());
        setSnapshotDataThresholdPercentage(other.raftConfig.getSnapshotDataThresholdPercentage());
        setElectionTimeoutFactor(other.raftConfig.getElectionTimeoutFactor());
    }

    public static Builder newBuilder() {
        return new Builder(new DatastoreContext());
    }

    public static Builder newBuilderFrom(DatastoreContext context) {
        return new Builder(new DatastoreContext(context));
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
        return raftConfig;
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

    public Timeout getShardLeaderElectionTimeout() {
        return shardLeaderElectionTimeout;
    }

    public boolean isPersistent() {
        return persistent;
    }

    public ConfigurationReader getConfigurationReader() {
        return configurationReader;
    }

    public long getShardElectionTimeoutFactor(){
        return raftConfig.getElectionTimeoutFactor();
    }

    public String getDataStoreType(){
        return dataStoreType;
    }

    public long getTransactionCreationInitialRateLimit() {
        return transactionCreationInitialRateLimit;
    }

    private void setHeartbeatInterval(long shardHeartbeatIntervalInMillis){
        raftConfig.setHeartBeatInterval(new FiniteDuration(shardHeartbeatIntervalInMillis,
                TimeUnit.MILLISECONDS));
    }

    private void setShardJournalRecoveryLogBatchSize(int shardJournalRecoveryLogBatchSize){
        raftConfig.setJournalRecoveryLogBatchSize(shardJournalRecoveryLogBatchSize);
    }


    private void setIsolatedLeaderCheckInterval(long shardIsolatedLeaderCheckIntervalInMillis) {
        raftConfig.setIsolatedLeaderCheckInterval(
                new FiniteDuration(shardIsolatedLeaderCheckIntervalInMillis, TimeUnit.MILLISECONDS));
    }

    private void setElectionTimeoutFactor(long shardElectionTimeoutFactor) {
        raftConfig.setElectionTimeoutFactor(shardElectionTimeoutFactor);
    }

    private void setSnapshotDataThresholdPercentage(int shardSnapshotDataThresholdPercentage) {
        raftConfig.setSnapshotDataThresholdPercentage(shardSnapshotDataThresholdPercentage);
    }

    private void setSnapshotBatchCount(long shardSnapshotBatchCount) {
        raftConfig.setSnapshotBatchCount(shardSnapshotBatchCount);
    }

    public static class Builder {
        private final DatastoreContext datastoreContext;
        private int maxShardDataChangeExecutorPoolSize =
                InMemoryDOMDataStoreConfigProperties.DEFAULT_MAX_DATA_CHANGE_EXECUTOR_POOL_SIZE;
        private int maxShardDataChangeExecutorQueueSize =
                InMemoryDOMDataStoreConfigProperties.DEFAULT_MAX_DATA_CHANGE_EXECUTOR_QUEUE_SIZE;
        private int maxShardDataChangeListenerQueueSize =
                InMemoryDOMDataStoreConfigProperties.DEFAULT_MAX_DATA_CHANGE_LISTENER_QUEUE_SIZE;
        private int maxShardDataStoreExecutorQueueSize =
                InMemoryDOMDataStoreConfigProperties.DEFAULT_MAX_DATA_STORE_EXECUTOR_QUEUE_SIZE;

        private Builder(DatastoreContext datastoreContext) {
            this.datastoreContext = datastoreContext;

            if(datastoreContext.getDataStoreProperties() != null) {
                maxShardDataChangeExecutorPoolSize =
                        datastoreContext.getDataStoreProperties().getMaxDataChangeExecutorPoolSize();
                maxShardDataChangeExecutorQueueSize =
                        datastoreContext.getDataStoreProperties().getMaxDataChangeExecutorQueueSize();
                maxShardDataChangeListenerQueueSize =
                        datastoreContext.getDataStoreProperties().getMaxDataChangeListenerQueueSize();
                maxShardDataStoreExecutorQueueSize =
                        datastoreContext.getDataStoreProperties().getMaxDataStoreExecutorQueueSize();
            }
        }

        public Builder boundedMailboxCapacity(int boundedMailboxCapacity) {
            // TODO - this is defined in the yang DataStoreProperties but not currently used.
            return this;
        }

        public Builder enableMetricCapture(boolean enableMetricCapture) {
            // TODO - this is defined in the yang DataStoreProperties but not currently used.
            return this;
        }

        public Builder shardTransactionIdleTimeout(long timeout, TimeUnit unit) {
            datastoreContext.shardTransactionIdleTimeout = Duration.create(timeout, unit);
            return this;
        }

        public Builder shardTransactionIdleTimeoutInMinutes(long timeout) {
            return shardTransactionIdleTimeout(timeout, TimeUnit.MINUTES);
        }

        public Builder operationTimeoutInSeconds(int operationTimeoutInSeconds) {
            datastoreContext.operationTimeoutInSeconds = operationTimeoutInSeconds;
            return this;
        }

        public Builder dataStoreMXBeanType(String dataStoreMXBeanType) {
            datastoreContext.dataStoreMXBeanType = dataStoreMXBeanType;
            return this;
        }

        public Builder shardTransactionCommitTimeoutInSeconds(int shardTransactionCommitTimeoutInSeconds) {
            datastoreContext.shardTransactionCommitTimeoutInSeconds = shardTransactionCommitTimeoutInSeconds;
            return this;
        }

        public Builder shardJournalRecoveryLogBatchSize(int shardJournalRecoveryLogBatchSize) {
            datastoreContext.setShardJournalRecoveryLogBatchSize(shardJournalRecoveryLogBatchSize);
            return this;
        }

        public Builder shardSnapshotBatchCount(int shardSnapshotBatchCount) {
            datastoreContext.setSnapshotBatchCount(shardSnapshotBatchCount);
            return this;
        }

        public Builder shardSnapshotDataThresholdPercentage(int shardSnapshotDataThresholdPercentage) {
            datastoreContext.setSnapshotDataThresholdPercentage(shardSnapshotDataThresholdPercentage);
            return this;
        }

        public Builder shardHeartbeatIntervalInMillis(int shardHeartbeatIntervalInMillis) {
            datastoreContext.setHeartbeatInterval(shardHeartbeatIntervalInMillis);
            return this;
        }

        public Builder shardTransactionCommitQueueCapacity(int shardTransactionCommitQueueCapacity) {
            datastoreContext.shardTransactionCommitQueueCapacity = shardTransactionCommitQueueCapacity;
            return this;
        }

        public Builder shardInitializationTimeout(long timeout, TimeUnit unit) {
            datastoreContext.shardInitializationTimeout = new Timeout(timeout, unit);
            return this;
        }

        public Builder shardInitializationTimeoutInSeconds(long timeout) {
            return shardInitializationTimeout(timeout, TimeUnit.SECONDS);
        }

        public Builder shardLeaderElectionTimeout(long timeout, TimeUnit unit) {
            datastoreContext.shardLeaderElectionTimeout = new Timeout(timeout, unit);
            return this;
        }

        public Builder shardLeaderElectionTimeoutInSeconds(long timeout) {
            return shardLeaderElectionTimeout(timeout, TimeUnit.SECONDS);
        }

        public Builder configurationReader(ConfigurationReader configurationReader){
            datastoreContext.configurationReader = configurationReader;
            return this;
        }

        public Builder persistent(boolean persistent){
            datastoreContext.persistent = persistent;
            return this;
        }

        public Builder shardIsolatedLeaderCheckIntervalInMillis(int shardIsolatedLeaderCheckIntervalInMillis) {
            datastoreContext.setIsolatedLeaderCheckInterval(shardIsolatedLeaderCheckIntervalInMillis);
            return this;
        }

        public Builder shardElectionTimeoutFactor(long shardElectionTimeoutFactor){
            datastoreContext.setElectionTimeoutFactor(shardElectionTimeoutFactor);
            return this;
        }

        public Builder transactionCreationInitialRateLimit(long initialRateLimit){
            datastoreContext.transactionCreationInitialRateLimit = initialRateLimit;
            return this;
        }

        public Builder dataStoreType(String dataStoreType){
            datastoreContext.dataStoreType = dataStoreType;
            datastoreContext.dataStoreMXBeanType = "Distributed" + WordUtils.capitalize(dataStoreType) + "Datastore";
            return this;
        }

        public Builder maxShardDataChangeExecutorPoolSize(int maxShardDataChangeExecutorPoolSize) {
            this.maxShardDataChangeExecutorPoolSize = maxShardDataChangeExecutorPoolSize;
            return this;
        }

        public Builder maxShardDataChangeExecutorQueueSize(int maxShardDataChangeExecutorQueueSize) {
            this.maxShardDataChangeExecutorQueueSize = maxShardDataChangeExecutorQueueSize;
            return this;
        }

        public Builder maxShardDataChangeListenerQueueSize(int maxShardDataChangeListenerQueueSize) {
            this.maxShardDataChangeListenerQueueSize = maxShardDataChangeListenerQueueSize;
            return this;
        }

        public Builder maxShardDataStoreExecutorQueueSize(int maxShardDataStoreExecutorQueueSize) {
            this.maxShardDataStoreExecutorQueueSize = maxShardDataStoreExecutorQueueSize;
            return this;
        }

        public DatastoreContext build() {
            datastoreContext.dataStoreProperties = InMemoryDOMDataStoreConfigProperties.create(
                    maxShardDataChangeExecutorPoolSize, maxShardDataChangeExecutorQueueSize,
                    maxShardDataChangeListenerQueueSize, maxShardDataStoreExecutorQueueSize);
            return datastoreContext;
        }
    }
}
