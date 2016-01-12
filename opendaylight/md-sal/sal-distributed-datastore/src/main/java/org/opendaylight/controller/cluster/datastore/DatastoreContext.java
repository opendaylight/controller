/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.datastore;

import akka.util.Timeout;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.text.WordUtils;
import org.opendaylight.controller.cluster.common.actor.AkkaConfigurationReader;
import org.opendaylight.controller.cluster.common.actor.FileAkkaConfigurationReader;
import org.opendaylight.controller.cluster.raft.ConfigParams;
import org.opendaylight.controller.cluster.raft.DefaultConfigParamsImpl;
import org.opendaylight.controller.cluster.raft.PeerAddressResolver;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStoreConfigProperties;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

/**
 * Contains contextual data for a data store.
 *
 * @author Thomas Pantelis
 */
public class DatastoreContext {
    public static final String METRICS_DOMAIN = "org.opendaylight.controller.cluster.datastore";

    public static final Duration DEFAULT_SHARD_TRANSACTION_IDLE_TIMEOUT = Duration.create(10, TimeUnit.MINUTES);
    public static final int DEFAULT_OPERATION_TIMEOUT_IN_MS = 5000;
    public static final int DEFAULT_SHARD_TX_COMMIT_TIMEOUT_IN_SECONDS = 30;
    public static final int DEFAULT_JOURNAL_RECOVERY_BATCH_SIZE = 1000;
    public static final int DEFAULT_SNAPSHOT_BATCH_COUNT = 20000;
    public static final int DEFAULT_HEARTBEAT_INTERVAL_IN_MILLIS = 500;
    public static final int DEFAULT_ISOLATED_LEADER_CHECK_INTERVAL_IN_MILLIS = DEFAULT_HEARTBEAT_INTERVAL_IN_MILLIS * 10;
    public static final int DEFAULT_SHARD_TX_COMMIT_QUEUE_CAPACITY = 50000;
    public static final Timeout DEFAULT_SHARD_INITIALIZATION_TIMEOUT = new Timeout(5, TimeUnit.MINUTES);
    public static final Timeout DEFAULT_SHARD_LEADER_ELECTION_TIMEOUT = new Timeout(30, TimeUnit.SECONDS);
    public static final boolean DEFAULT_PERSISTENT = true;
    public static final FileAkkaConfigurationReader DEFAULT_CONFIGURATION_READER = new FileAkkaConfigurationReader();
    public static final int DEFAULT_SHARD_SNAPSHOT_DATA_THRESHOLD_PERCENTAGE = 12;
    public static final int DEFAULT_SHARD_ELECTION_TIMEOUT_FACTOR = 2;
    public static final int DEFAULT_TX_CREATION_INITIAL_RATE_LIMIT = 100;
    public static final String UNKNOWN_DATA_STORE_TYPE = "unknown";
    public static final int DEFAULT_SHARD_BATCHED_MODIFICATION_COUNT = 1000;
    public static final long DEFAULT_SHARD_COMMIT_QUEUE_EXPIRY_TIMEOUT_IN_MS = TimeUnit.MILLISECONDS.convert(2, TimeUnit.MINUTES);
    public static final int DEFAULT_SHARD_SNAPSHOT_CHUNK_SIZE = 2048000;

    private static final Set<String> globalDatastoreNames = Sets.newConcurrentHashSet();

    private InMemoryDOMDataStoreConfigProperties dataStoreProperties;
    private Duration shardTransactionIdleTimeout = DatastoreContext.DEFAULT_SHARD_TRANSACTION_IDLE_TIMEOUT;
    private long operationTimeoutInMillis = DEFAULT_OPERATION_TIMEOUT_IN_MS;
    private String dataStoreMXBeanType;
    private int shardTransactionCommitTimeoutInSeconds = DEFAULT_SHARD_TX_COMMIT_TIMEOUT_IN_SECONDS;
    private int shardTransactionCommitQueueCapacity = DEFAULT_SHARD_TX_COMMIT_QUEUE_CAPACITY;
    private Timeout shardInitializationTimeout = DEFAULT_SHARD_INITIALIZATION_TIMEOUT;
    private Timeout shardLeaderElectionTimeout = DEFAULT_SHARD_LEADER_ELECTION_TIMEOUT;
    private boolean persistent = DEFAULT_PERSISTENT;
    private AkkaConfigurationReader configurationReader = DEFAULT_CONFIGURATION_READER;
    private long transactionCreationInitialRateLimit = DEFAULT_TX_CREATION_INITIAL_RATE_LIMIT;
    private final DefaultConfigParamsImpl raftConfig = new DefaultConfigParamsImpl();
    private String dataStoreName = UNKNOWN_DATA_STORE_TYPE;
    private LogicalDatastoreType logicalStoreType = LogicalDatastoreType.OPERATIONAL;
    private int shardBatchedModificationCount = DEFAULT_SHARD_BATCHED_MODIFICATION_COUNT;
    private boolean writeOnlyTransactionOptimizationsEnabled = true;
    private long shardCommitQueueExpiryTimeoutInMillis = DEFAULT_SHARD_COMMIT_QUEUE_EXPIRY_TIMEOUT_IN_MS;
    private boolean transactionDebugContextEnabled = false;
    private String shardManagerPersistenceId;

    public static Set<String> getGlobalDatastoreNames() {
        return globalDatastoreNames;
    }

    private DatastoreContext() {
        setShardJournalRecoveryLogBatchSize(DEFAULT_JOURNAL_RECOVERY_BATCH_SIZE);
        setSnapshotBatchCount(DEFAULT_SNAPSHOT_BATCH_COUNT);
        setHeartbeatInterval(DEFAULT_HEARTBEAT_INTERVAL_IN_MILLIS);
        setIsolatedLeaderCheckInterval(DEFAULT_ISOLATED_LEADER_CHECK_INTERVAL_IN_MILLIS);
        setSnapshotDataThresholdPercentage(DEFAULT_SHARD_SNAPSHOT_DATA_THRESHOLD_PERCENTAGE);
        setElectionTimeoutFactor(DEFAULT_SHARD_ELECTION_TIMEOUT_FACTOR);
        setShardSnapshotChunkSize(DEFAULT_SHARD_SNAPSHOT_CHUNK_SIZE);
    }

    private DatastoreContext(DatastoreContext other) {
        this.dataStoreProperties = other.dataStoreProperties;
        this.shardTransactionIdleTimeout = other.shardTransactionIdleTimeout;
        this.operationTimeoutInMillis = other.operationTimeoutInMillis;
        this.dataStoreMXBeanType = other.dataStoreMXBeanType;
        this.shardTransactionCommitTimeoutInSeconds = other.shardTransactionCommitTimeoutInSeconds;
        this.shardTransactionCommitQueueCapacity = other.shardTransactionCommitQueueCapacity;
        this.shardInitializationTimeout = other.shardInitializationTimeout;
        this.shardLeaderElectionTimeout = other.shardLeaderElectionTimeout;
        this.persistent = other.persistent;
        this.configurationReader = other.configurationReader;
        this.transactionCreationInitialRateLimit = other.transactionCreationInitialRateLimit;
        this.dataStoreName = other.dataStoreName;
        this.logicalStoreType = other.logicalStoreType;
        this.shardBatchedModificationCount = other.shardBatchedModificationCount;
        this.writeOnlyTransactionOptimizationsEnabled = other.writeOnlyTransactionOptimizationsEnabled;
        this.shardCommitQueueExpiryTimeoutInMillis = other.shardCommitQueueExpiryTimeoutInMillis;
        this.transactionDebugContextEnabled = other.transactionDebugContextEnabled;
        this.shardManagerPersistenceId = other.shardManagerPersistenceId;

        setShardJournalRecoveryLogBatchSize(other.raftConfig.getJournalRecoveryLogBatchSize());
        setSnapshotBatchCount(other.raftConfig.getSnapshotBatchCount());
        setHeartbeatInterval(other.raftConfig.getHeartBeatInterval().toMillis());
        setIsolatedLeaderCheckInterval(other.raftConfig.getIsolatedCheckIntervalInMillis());
        setSnapshotDataThresholdPercentage(other.raftConfig.getSnapshotDataThresholdPercentage());
        setElectionTimeoutFactor(other.raftConfig.getElectionTimeoutFactor());
        setCustomRaftPolicyImplementation(other.raftConfig.getCustomRaftPolicyImplementationClass());
        setShardSnapshotChunkSize(other.raftConfig.getSnapshotChunkSize());
        setPeerAddressResolver(other.raftConfig.getPeerAddressResolver());
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

    public long getOperationTimeoutInMillis() {
        return operationTimeoutInMillis;
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

    public AkkaConfigurationReader getConfigurationReader() {
        return configurationReader;
    }

    public long getShardElectionTimeoutFactor(){
        return raftConfig.getElectionTimeoutFactor();
    }

    public String getDataStoreName(){
        return dataStoreName;
    }

    public LogicalDatastoreType getLogicalStoreType() {
        return logicalStoreType;
    }

    public long getTransactionCreationInitialRateLimit() {
        return transactionCreationInitialRateLimit;
    }

    public String getShardManagerPersistenceId() {
        return shardManagerPersistenceId;
    }

    private void setPeerAddressResolver(PeerAddressResolver resolver) {
        raftConfig.setPeerAddressResolver(resolver);
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

    private void setCustomRaftPolicyImplementation(String customRaftPolicyImplementation) {
        raftConfig.setCustomRaftPolicyImplementationClass(customRaftPolicyImplementation);
    }

    private void setSnapshotDataThresholdPercentage(int shardSnapshotDataThresholdPercentage) {
        raftConfig.setSnapshotDataThresholdPercentage(shardSnapshotDataThresholdPercentage);
    }

    private void setSnapshotBatchCount(long shardSnapshotBatchCount) {
        raftConfig.setSnapshotBatchCount(shardSnapshotBatchCount);
    }

    private void setShardSnapshotChunkSize(int shardSnapshotChunkSize) {
        raftConfig.setSnapshotChunkSize(shardSnapshotChunkSize);
    }

    public int getShardBatchedModificationCount() {
        return shardBatchedModificationCount;
    }

    public boolean isWriteOnlyTransactionOptimizationsEnabled() {
        return writeOnlyTransactionOptimizationsEnabled;
    }

    public long getShardCommitQueueExpiryTimeoutInMillis() {
        return shardCommitQueueExpiryTimeoutInMillis;
    }

    public boolean isTransactionDebugContextEnabled() {
        return transactionDebugContextEnabled;
    }

    public int getShardSnapshotChunkSize() {
        return raftConfig.getSnapshotChunkSize();
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
            datastoreContext.operationTimeoutInMillis = TimeUnit.SECONDS.toMillis(operationTimeoutInSeconds);
            return this;
        }

        public Builder operationTimeoutInMillis(long operationTimeoutInMillis) {
            datastoreContext.operationTimeoutInMillis = operationTimeoutInMillis;
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

        public Builder configurationReader(AkkaConfigurationReader configurationReader){
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

        public Builder logicalStoreType(LogicalDatastoreType logicalStoreType){
            datastoreContext.logicalStoreType = Preconditions.checkNotNull(logicalStoreType);

            // Retain compatible naming
            switch (logicalStoreType) {
            case CONFIGURATION:
                dataStoreName("config");
                break;
            case OPERATIONAL:
                dataStoreName("operational");
                break;
            default:
                dataStoreName(logicalStoreType.name());
            }

            return this;
        }

        public Builder dataStoreName(String dataStoreName){
            datastoreContext.dataStoreName = Preconditions.checkNotNull(dataStoreName);
            datastoreContext.dataStoreMXBeanType = "Distributed" + WordUtils.capitalize(dataStoreName) + "Datastore";
            return this;
        }

        public Builder shardBatchedModificationCount(int shardBatchedModificationCount) {
            datastoreContext.shardBatchedModificationCount = shardBatchedModificationCount;
            return this;
        }

        public Builder writeOnlyTransactionOptimizationsEnabled(boolean value) {
            datastoreContext.writeOnlyTransactionOptimizationsEnabled = value;
            return this;
        }

        public Builder shardCommitQueueExpiryTimeoutInMillis(long value) {
            datastoreContext.shardCommitQueueExpiryTimeoutInMillis = value;
            return this;
        }

        public Builder shardCommitQueueExpiryTimeoutInSeconds(long value) {
            datastoreContext.shardCommitQueueExpiryTimeoutInMillis = TimeUnit.MILLISECONDS.convert(
                    value, TimeUnit.SECONDS);
            return this;
        }

        public Builder transactionDebugContextEnabled(boolean value) {
            datastoreContext.transactionDebugContextEnabled = value;
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

        /**
         * For unit tests only.
         */
        @VisibleForTesting
        public Builder shardManagerPersistenceId(String id) {
            datastoreContext.shardManagerPersistenceId = id;
            return this;
        }

        public DatastoreContext build() {
            datastoreContext.dataStoreProperties = InMemoryDOMDataStoreConfigProperties.create(
                    maxShardDataChangeExecutorPoolSize, maxShardDataChangeExecutorQueueSize,
                    maxShardDataChangeListenerQueueSize, maxShardDataStoreExecutorQueueSize);

            if(datastoreContext.dataStoreName != null) {
                globalDatastoreNames.add(datastoreContext.dataStoreName);
            }

            return datastoreContext;
        }

        public Builder customRaftPolicyImplementation(String customRaftPolicyImplementation) {
            datastoreContext.setCustomRaftPolicyImplementation(customRaftPolicyImplementation);
            return this;
        }

        public Builder shardSnapshotChunkSize(int shardSnapshotChunkSize) {
            datastoreContext.setShardSnapshotChunkSize(shardSnapshotChunkSize);
            return this;
        }

        public Builder shardPeerAddressResolver(PeerAddressResolver resolver) {
            datastoreContext.setPeerAddressResolver(resolver);
            return this;
        }
    }
}
