/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import akka.util.Timeout;
import com.google.common.annotations.VisibleForTesting;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.apache.commons.text.WordUtils;
import org.opendaylight.controller.cluster.access.client.AbstractClientConnection;
import org.opendaylight.controller.cluster.access.client.ClientActorConfig;
import org.opendaylight.controller.cluster.common.actor.AkkaConfigurationReader;
import org.opendaylight.controller.cluster.common.actor.FileAkkaConfigurationReader;
import org.opendaylight.controller.cluster.raft.ConfigParams;
import org.opendaylight.controller.cluster.raft.DefaultConfigParamsImpl;
import org.opendaylight.controller.cluster.raft.PeerAddressResolver;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataStoreConfigProperties;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.FiniteDuration;

/**
 * Contains contextual data for a data store.
 *
 * @author Thomas Pantelis
 */
// Non-final for mocking
public class DatastoreContext implements ClientActorConfig {
    public static final String METRICS_DOMAIN = "org.opendaylight.controller.cluster.datastore";

    public static final FiniteDuration DEFAULT_SHARD_TRANSACTION_IDLE_TIMEOUT = FiniteDuration.create(10,
        TimeUnit.MINUTES);
    public static final int DEFAULT_OPERATION_TIMEOUT_IN_MS = 5000;
    public static final int DEFAULT_SHARD_TX_COMMIT_TIMEOUT_IN_SECONDS = 30;
    public static final int DEFAULT_JOURNAL_RECOVERY_BATCH_SIZE = 1;
    public static final int DEFAULT_SNAPSHOT_BATCH_COUNT = 20000;
    public static final int DEFAULT_HEARTBEAT_INTERVAL_IN_MILLIS = 500;
    public static final int DEFAULT_ISOLATED_LEADER_CHECK_INTERVAL_IN_MILLIS =
            DEFAULT_HEARTBEAT_INTERVAL_IN_MILLIS * 10;
    public static final int DEFAULT_SHARD_TX_COMMIT_QUEUE_CAPACITY = 50000;
    public static final Timeout DEFAULT_SHARD_INITIALIZATION_TIMEOUT = new Timeout(5, TimeUnit.MINUTES);
    public static final Timeout DEFAULT_SHARD_LEADER_ELECTION_TIMEOUT = new Timeout(30, TimeUnit.SECONDS);
    public static final int DEFAULT_INITIAL_SETTLE_TIMEOUT_MULTIPLIER = 3;
    public static final boolean DEFAULT_PERSISTENT = true;
    public static final FileAkkaConfigurationReader DEFAULT_CONFIGURATION_READER = new FileAkkaConfigurationReader();
    public static final int DEFAULT_SHARD_SNAPSHOT_DATA_THRESHOLD_PERCENTAGE = 12;
    public static final int DEFAULT_SHARD_ELECTION_TIMEOUT_FACTOR = 2;
    public static final int DEFAULT_SHARD_CANDIDATE_ELECTION_TIMEOUT_DIVISOR = 1;
    public static final int DEFAULT_TX_CREATION_INITIAL_RATE_LIMIT = 100;
    public static final String UNKNOWN_DATA_STORE_TYPE = "unknown";
    public static final int DEFAULT_SHARD_BATCHED_MODIFICATION_COUNT = 1000;
    public static final long DEFAULT_SHARD_COMMIT_QUEUE_EXPIRY_TIMEOUT_IN_MS =
            TimeUnit.MILLISECONDS.convert(2, TimeUnit.MINUTES);
    public static final int DEFAULT_MAX_MESSAGE_SLICE_SIZE = 2048 * 1000; // 2MB
    public static final int DEFAULT_INITIAL_PAYLOAD_SERIALIZED_BUFFER_CAPACITY = 512;

    public static final long DEFAULT_SYNC_INDEX_THRESHOLD = 10;

    private static final Logger LOG = LoggerFactory.getLogger(DatastoreContext.class);

    private static final Set<String> GLOBAL_DATASTORE_NAMES = ConcurrentHashMap.newKeySet();

    private final DefaultConfigParamsImpl raftConfig = new DefaultConfigParamsImpl();

    private InMemoryDOMDataStoreConfigProperties dataStoreProperties;
    private FiniteDuration shardTransactionIdleTimeout = DatastoreContext.DEFAULT_SHARD_TRANSACTION_IDLE_TIMEOUT;
    private long operationTimeoutInMillis = DEFAULT_OPERATION_TIMEOUT_IN_MS;
    private String dataStoreMXBeanType;
    private int shardTransactionCommitTimeoutInSeconds = DEFAULT_SHARD_TX_COMMIT_TIMEOUT_IN_SECONDS;
    private int shardTransactionCommitQueueCapacity = DEFAULT_SHARD_TX_COMMIT_QUEUE_CAPACITY;
    private Timeout shardInitializationTimeout = DEFAULT_SHARD_INITIALIZATION_TIMEOUT;
    private Timeout shardLeaderElectionTimeout = DEFAULT_SHARD_LEADER_ELECTION_TIMEOUT;
    private int initialSettleTimeoutMultiplier = DEFAULT_INITIAL_SETTLE_TIMEOUT_MULTIPLIER;
    private boolean persistent = DEFAULT_PERSISTENT;
    private AkkaConfigurationReader configurationReader = DEFAULT_CONFIGURATION_READER;
    private long transactionCreationInitialRateLimit = DEFAULT_TX_CREATION_INITIAL_RATE_LIMIT;
    private String dataStoreName = UNKNOWN_DATA_STORE_TYPE;
    private LogicalDatastoreType logicalStoreType = LogicalDatastoreType.OPERATIONAL;
    private YangInstanceIdentifier storeRoot = YangInstanceIdentifier.empty();
    private int shardBatchedModificationCount = DEFAULT_SHARD_BATCHED_MODIFICATION_COUNT;
    private boolean writeOnlyTransactionOptimizationsEnabled = true;
    private long shardCommitQueueExpiryTimeoutInMillis = DEFAULT_SHARD_COMMIT_QUEUE_EXPIRY_TIMEOUT_IN_MS;
    private boolean useTellBasedProtocol = false;
    private boolean transactionDebugContextEnabled = false;
    private String shardManagerPersistenceId;
    private int maximumMessageSliceSize = DEFAULT_MAX_MESSAGE_SLICE_SIZE;
    private long backendAlivenessTimerInterval = AbstractClientConnection.DEFAULT_BACKEND_ALIVE_TIMEOUT_NANOS;
    private long requestTimeout = AbstractClientConnection.DEFAULT_REQUEST_TIMEOUT_NANOS;
    private long noProgressTimeout = AbstractClientConnection.DEFAULT_NO_PROGRESS_TIMEOUT_NANOS;
    private int initialPayloadSerializedBufferCapacity = DEFAULT_INITIAL_PAYLOAD_SERIALIZED_BUFFER_CAPACITY;

    public static Set<String> getGlobalDatastoreNames() {
        return GLOBAL_DATASTORE_NAMES;
    }

    DatastoreContext() {
        setShardJournalRecoveryLogBatchSize(DEFAULT_JOURNAL_RECOVERY_BATCH_SIZE);
        setSnapshotBatchCount(DEFAULT_SNAPSHOT_BATCH_COUNT);
        setHeartbeatInterval(DEFAULT_HEARTBEAT_INTERVAL_IN_MILLIS);
        setIsolatedLeaderCheckInterval(DEFAULT_ISOLATED_LEADER_CHECK_INTERVAL_IN_MILLIS);
        setSnapshotDataThresholdPercentage(DEFAULT_SHARD_SNAPSHOT_DATA_THRESHOLD_PERCENTAGE);
        setElectionTimeoutFactor(DEFAULT_SHARD_ELECTION_TIMEOUT_FACTOR);
        setCandidateElectionTimeoutDivisor(DEFAULT_SHARD_CANDIDATE_ELECTION_TIMEOUT_DIVISOR);
        setSyncIndexThreshold(DEFAULT_SYNC_INDEX_THRESHOLD);
        setMaximumMessageSliceSize(DEFAULT_MAX_MESSAGE_SLICE_SIZE);
    }

    private DatastoreContext(final DatastoreContext other) {
        this.dataStoreProperties = other.dataStoreProperties;
        this.shardTransactionIdleTimeout = other.shardTransactionIdleTimeout;
        this.operationTimeoutInMillis = other.operationTimeoutInMillis;
        this.dataStoreMXBeanType = other.dataStoreMXBeanType;
        this.shardTransactionCommitTimeoutInSeconds = other.shardTransactionCommitTimeoutInSeconds;
        this.shardTransactionCommitQueueCapacity = other.shardTransactionCommitQueueCapacity;
        this.shardInitializationTimeout = other.shardInitializationTimeout;
        this.shardLeaderElectionTimeout = other.shardLeaderElectionTimeout;
        this.initialSettleTimeoutMultiplier = other.initialSettleTimeoutMultiplier;
        this.persistent = other.persistent;
        this.configurationReader = other.configurationReader;
        this.transactionCreationInitialRateLimit = other.transactionCreationInitialRateLimit;
        this.dataStoreName = other.dataStoreName;
        this.logicalStoreType = other.logicalStoreType;
        this.storeRoot = other.storeRoot;
        this.shardBatchedModificationCount = other.shardBatchedModificationCount;
        this.writeOnlyTransactionOptimizationsEnabled = other.writeOnlyTransactionOptimizationsEnabled;
        this.shardCommitQueueExpiryTimeoutInMillis = other.shardCommitQueueExpiryTimeoutInMillis;
        this.transactionDebugContextEnabled = other.transactionDebugContextEnabled;
        this.shardManagerPersistenceId = other.shardManagerPersistenceId;
        this.useTellBasedProtocol = other.useTellBasedProtocol;
        this.backendAlivenessTimerInterval = other.backendAlivenessTimerInterval;
        this.requestTimeout = other.requestTimeout;
        this.noProgressTimeout = other.noProgressTimeout;
        this.initialPayloadSerializedBufferCapacity = other.initialPayloadSerializedBufferCapacity;

        setShardJournalRecoveryLogBatchSize(other.raftConfig.getJournalRecoveryLogBatchSize());
        setSnapshotBatchCount(other.raftConfig.getSnapshotBatchCount());
        setHeartbeatInterval(other.raftConfig.getHeartBeatInterval().toMillis());
        setIsolatedLeaderCheckInterval(other.raftConfig.getIsolatedCheckIntervalInMillis());
        setSnapshotDataThresholdPercentage(other.raftConfig.getSnapshotDataThresholdPercentage());
        setElectionTimeoutFactor(other.raftConfig.getElectionTimeoutFactor());
        setCandidateElectionTimeoutDivisor(other.raftConfig.getCandidateElectionTimeoutDivisor());
        setCustomRaftPolicyImplementation(other.raftConfig.getCustomRaftPolicyImplementationClass());
        setMaximumMessageSliceSize(other.getMaximumMessageSliceSize());
        setShardSnapshotChunkSize(other.raftConfig.getSnapshotChunkSize());
        setPeerAddressResolver(other.raftConfig.getPeerAddressResolver());
        setTempFileDirectory(other.getTempFileDirectory());
        setFileBackedStreamingThreshold(other.getFileBackedStreamingThreshold());
        setSyncIndexThreshold(other.raftConfig.getSyncIndexThreshold());
    }

    public static Builder newBuilder() {
        return new Builder(new DatastoreContext());
    }

    public static Builder newBuilderFrom(final DatastoreContext context) {
        return new Builder(new DatastoreContext(context));
    }

    public InMemoryDOMDataStoreConfigProperties getDataStoreProperties() {
        return dataStoreProperties;
    }

    public FiniteDuration getShardTransactionIdleTimeout() {
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

    /**
     * Return the multiplier of {@link #getShardLeaderElectionTimeout()} which the frontend will wait for all shards
     * on the local node to settle.
     *
     * @return Non-negative multiplier. Value of {@code 0} indicates to wait indefinitely.
     */
    public int getInitialSettleTimeoutMultiplier() {
        return initialSettleTimeoutMultiplier;
    }

    public boolean isPersistent() {
        return persistent;
    }

    public AkkaConfigurationReader getConfigurationReader() {
        return configurationReader;
    }

    public long getShardElectionTimeoutFactor() {
        return raftConfig.getElectionTimeoutFactor();
    }

    public String getDataStoreName() {
        return dataStoreName;
    }

    public LogicalDatastoreType getLogicalStoreType() {
        return logicalStoreType;
    }

    public YangInstanceIdentifier getStoreRoot() {
        return storeRoot;
    }

    public long getTransactionCreationInitialRateLimit() {
        return transactionCreationInitialRateLimit;
    }

    public String getShardManagerPersistenceId() {
        return shardManagerPersistenceId;
    }

    @Override
    public String getTempFileDirectory() {
        return raftConfig.getTempFileDirectory();
    }

    private void setTempFileDirectory(final String tempFileDirectory) {
        raftConfig.setTempFileDirectory(tempFileDirectory);
    }

    @Override
    public int getFileBackedStreamingThreshold() {
        return raftConfig.getFileBackedStreamingThreshold();
    }

    private void setFileBackedStreamingThreshold(final int fileBackedStreamingThreshold) {
        raftConfig.setFileBackedStreamingThreshold(fileBackedStreamingThreshold);
    }

    private void setPeerAddressResolver(final PeerAddressResolver resolver) {
        raftConfig.setPeerAddressResolver(resolver);
    }

    private void setHeartbeatInterval(final long shardHeartbeatIntervalInMillis) {
        raftConfig.setHeartBeatInterval(new FiniteDuration(shardHeartbeatIntervalInMillis,
                TimeUnit.MILLISECONDS));
    }

    private void setShardJournalRecoveryLogBatchSize(final int shardJournalRecoveryLogBatchSize) {
        raftConfig.setJournalRecoveryLogBatchSize(shardJournalRecoveryLogBatchSize);
    }


    private void setIsolatedLeaderCheckInterval(final long shardIsolatedLeaderCheckIntervalInMillis) {
        raftConfig.setIsolatedLeaderCheckInterval(
                new FiniteDuration(shardIsolatedLeaderCheckIntervalInMillis, TimeUnit.MILLISECONDS));
    }

    private void setElectionTimeoutFactor(final long shardElectionTimeoutFactor) {
        raftConfig.setElectionTimeoutFactor(shardElectionTimeoutFactor);
    }

    private void setCandidateElectionTimeoutDivisor(final long candidateElectionTimeoutDivisor) {
        raftConfig.setCandidateElectionTimeoutDivisor(candidateElectionTimeoutDivisor);
    }

    private void setCustomRaftPolicyImplementation(final String customRaftPolicyImplementation) {
        raftConfig.setCustomRaftPolicyImplementationClass(customRaftPolicyImplementation);
    }

    private void setSnapshotDataThresholdPercentage(final int shardSnapshotDataThresholdPercentage) {
        checkArgument(shardSnapshotDataThresholdPercentage >= 0 && shardSnapshotDataThresholdPercentage <= 100);
        raftConfig.setSnapshotDataThresholdPercentage(shardSnapshotDataThresholdPercentage);
    }

    private void setSnapshotBatchCount(final long shardSnapshotBatchCount) {
        raftConfig.setSnapshotBatchCount(shardSnapshotBatchCount);
    }

    @Deprecated
    private void setShardSnapshotChunkSize(final int shardSnapshotChunkSize) {
        // We'll honor the shardSnapshotChunkSize setting for backwards compatibility but only if it doesn't exceed
        // maximumMessageSliceSize.
        if (shardSnapshotChunkSize < maximumMessageSliceSize) {
            raftConfig.setSnapshotChunkSize(shardSnapshotChunkSize);
        }
    }

    private void setMaximumMessageSliceSize(final int maximumMessageSliceSize) {
        raftConfig.setSnapshotChunkSize(maximumMessageSliceSize);
        this.maximumMessageSliceSize = maximumMessageSliceSize;
    }

    private void setSyncIndexThreshold(final long syncIndexThreshold) {
        raftConfig.setSyncIndexThreshold(syncIndexThreshold);
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

    public boolean isUseTellBasedProtocol() {
        return useTellBasedProtocol;
    }

    @Override
    public int getMaximumMessageSliceSize() {
        return maximumMessageSliceSize;
    }

    @Override
    public long getBackendAlivenessTimerInterval() {
        return backendAlivenessTimerInterval;
    }

    @Override
    public long getRequestTimeout() {
        return requestTimeout;
    }

    @Override
    public long getNoProgressTimeout() {
        return noProgressTimeout;
    }

    public int getInitialPayloadSerializedBufferCapacity() {
        return initialPayloadSerializedBufferCapacity;
    }

    public static class Builder implements org.opendaylight.yangtools.concepts.Builder<DatastoreContext> {
        private final DatastoreContext datastoreContext;
        private int maxShardDataChangeExecutorPoolSize =
                InMemoryDOMDataStoreConfigProperties.DEFAULT_MAX_DATA_CHANGE_EXECUTOR_POOL_SIZE;
        private int maxShardDataChangeExecutorQueueSize =
                InMemoryDOMDataStoreConfigProperties.DEFAULT_MAX_DATA_CHANGE_EXECUTOR_QUEUE_SIZE;
        private int maxShardDataChangeListenerQueueSize =
                InMemoryDOMDataStoreConfigProperties.DEFAULT_MAX_DATA_CHANGE_LISTENER_QUEUE_SIZE;
        private int maxShardDataStoreExecutorQueueSize =
                InMemoryDOMDataStoreConfigProperties.DEFAULT_MAX_DATA_STORE_EXECUTOR_QUEUE_SIZE;

        Builder(final DatastoreContext datastoreContext) {
            this.datastoreContext = datastoreContext;

            if (datastoreContext.getDataStoreProperties() != null) {
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

        public Builder boundedMailboxCapacity(final int boundedMailboxCapacity) {
            // TODO - this is defined in the yang DataStoreProperties but not currently used.
            return this;
        }

        public Builder enableMetricCapture(final boolean enableMetricCapture) {
            // TODO - this is defined in the yang DataStoreProperties but not currently used.
            return this;
        }


        public Builder shardTransactionIdleTimeout(final long timeout, final TimeUnit unit) {
            datastoreContext.shardTransactionIdleTimeout = FiniteDuration.create(timeout, unit);
            return this;
        }

        public Builder shardTransactionIdleTimeoutInMinutes(final long timeout) {
            return shardTransactionIdleTimeout(timeout, TimeUnit.MINUTES);
        }

        public Builder operationTimeoutInSeconds(final int operationTimeoutInSeconds) {
            datastoreContext.operationTimeoutInMillis = TimeUnit.SECONDS.toMillis(operationTimeoutInSeconds);
            return this;
        }

        public Builder operationTimeoutInMillis(final long operationTimeoutInMillis) {
            datastoreContext.operationTimeoutInMillis = operationTimeoutInMillis;
            return this;
        }

        public Builder dataStoreMXBeanType(final String dataStoreMXBeanType) {
            datastoreContext.dataStoreMXBeanType = dataStoreMXBeanType;
            return this;
        }

        public Builder shardTransactionCommitTimeoutInSeconds(final int shardTransactionCommitTimeoutInSeconds) {
            datastoreContext.shardTransactionCommitTimeoutInSeconds = shardTransactionCommitTimeoutInSeconds;
            return this;
        }

        public Builder shardJournalRecoveryLogBatchSize(final int shardJournalRecoveryLogBatchSize) {
            datastoreContext.setShardJournalRecoveryLogBatchSize(shardJournalRecoveryLogBatchSize);
            return this;
        }

        public Builder shardSnapshotBatchCount(final int shardSnapshotBatchCount) {
            datastoreContext.setSnapshotBatchCount(shardSnapshotBatchCount);
            return this;
        }

        public Builder shardSnapshotDataThresholdPercentage(final int shardSnapshotDataThresholdPercentage) {
            datastoreContext.setSnapshotDataThresholdPercentage(shardSnapshotDataThresholdPercentage);
            return this;
        }

        public Builder shardHeartbeatIntervalInMillis(final int shardHeartbeatIntervalInMillis) {
            datastoreContext.setHeartbeatInterval(shardHeartbeatIntervalInMillis);
            return this;
        }

        public Builder shardTransactionCommitQueueCapacity(final int shardTransactionCommitQueueCapacity) {
            datastoreContext.shardTransactionCommitQueueCapacity = shardTransactionCommitQueueCapacity;
            return this;
        }

        public Builder shardInitializationTimeout(final long timeout, final TimeUnit unit) {
            datastoreContext.shardInitializationTimeout = new Timeout(timeout, unit);
            return this;
        }

        public Builder shardInitializationTimeoutInSeconds(final long timeout) {
            return shardInitializationTimeout(timeout, TimeUnit.SECONDS);
        }

        public Builder shardLeaderElectionTimeout(final long timeout, final TimeUnit unit) {
            datastoreContext.shardLeaderElectionTimeout = new Timeout(timeout, unit);
            return this;
        }

        public Builder shardLeaderElectionTimeoutMultiplier(final int multiplier) {
            checkArgument(multiplier >= 0);
            datastoreContext.initialSettleTimeoutMultiplier = multiplier;
            return this;
        }

        public Builder shardLeaderElectionTimeoutInSeconds(final long timeout) {
            return shardLeaderElectionTimeout(timeout, TimeUnit.SECONDS);
        }

        public Builder configurationReader(final AkkaConfigurationReader configurationReader) {
            datastoreContext.configurationReader = configurationReader;
            return this;
        }

        public Builder persistent(final boolean persistent) {
            datastoreContext.persistent = persistent;
            return this;
        }

        public Builder shardIsolatedLeaderCheckIntervalInMillis(final int shardIsolatedLeaderCheckIntervalInMillis) {
            datastoreContext.setIsolatedLeaderCheckInterval(shardIsolatedLeaderCheckIntervalInMillis);
            return this;
        }

        public Builder shardElectionTimeoutFactor(final long shardElectionTimeoutFactor) {
            datastoreContext.setElectionTimeoutFactor(shardElectionTimeoutFactor);
            return this;
        }

        public Builder shardCandidateElectionTimeoutDivisor(final long candidateElectionTimeoutDivisor) {
            datastoreContext.setCandidateElectionTimeoutDivisor(candidateElectionTimeoutDivisor);
            return this;
        }

        public Builder transactionCreationInitialRateLimit(final long initialRateLimit) {
            datastoreContext.transactionCreationInitialRateLimit = initialRateLimit;
            return this;
        }

        public Builder logicalStoreType(final LogicalDatastoreType logicalStoreType) {
            datastoreContext.logicalStoreType = requireNonNull(logicalStoreType);

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

        public Builder storeRoot(final YangInstanceIdentifier storeRoot) {
            datastoreContext.storeRoot = storeRoot;
            return this;
        }

        public Builder dataStoreName(final String dataStoreName) {
            datastoreContext.dataStoreName = requireNonNull(dataStoreName);
            datastoreContext.dataStoreMXBeanType = "Distributed" + WordUtils.capitalize(dataStoreName) + "Datastore";
            return this;
        }

        public Builder shardBatchedModificationCount(final int shardBatchedModificationCount) {
            datastoreContext.shardBatchedModificationCount = shardBatchedModificationCount;
            return this;
        }

        public Builder writeOnlyTransactionOptimizationsEnabled(final boolean value) {
            datastoreContext.writeOnlyTransactionOptimizationsEnabled = value;
            return this;
        }

        public Builder shardCommitQueueExpiryTimeoutInMillis(final long value) {
            datastoreContext.shardCommitQueueExpiryTimeoutInMillis = value;
            return this;
        }

        public Builder shardCommitQueueExpiryTimeoutInSeconds(final long value) {
            datastoreContext.shardCommitQueueExpiryTimeoutInMillis = TimeUnit.MILLISECONDS.convert(
                    value, TimeUnit.SECONDS);
            return this;
        }

        public Builder transactionDebugContextEnabled(final boolean value) {
            datastoreContext.transactionDebugContextEnabled = value;
            return this;
        }

        public Builder maxShardDataChangeExecutorPoolSize(final int newMaxShardDataChangeExecutorPoolSize) {
            this.maxShardDataChangeExecutorPoolSize = newMaxShardDataChangeExecutorPoolSize;
            return this;
        }

        public Builder maxShardDataChangeExecutorQueueSize(final int newMaxShardDataChangeExecutorQueueSize) {
            this.maxShardDataChangeExecutorQueueSize = newMaxShardDataChangeExecutorQueueSize;
            return this;
        }

        public Builder maxShardDataChangeListenerQueueSize(final int newMaxShardDataChangeListenerQueueSize) {
            this.maxShardDataChangeListenerQueueSize = newMaxShardDataChangeListenerQueueSize;
            return this;
        }

        public Builder maxShardDataStoreExecutorQueueSize(final int newMaxShardDataStoreExecutorQueueSize) {
            this.maxShardDataStoreExecutorQueueSize = newMaxShardDataStoreExecutorQueueSize;
            return this;
        }

        public Builder useTellBasedProtocol(final boolean value) {
            datastoreContext.useTellBasedProtocol = value;
            return this;
        }

        /**
         * For unit tests only.
         */
        @VisibleForTesting
        public Builder shardManagerPersistenceId(final String id) {
            datastoreContext.shardManagerPersistenceId = id;
            return this;
        }

        public Builder customRaftPolicyImplementation(final String customRaftPolicyImplementation) {
            datastoreContext.setCustomRaftPolicyImplementation(customRaftPolicyImplementation);
            return this;
        }

        @Deprecated
        public Builder shardSnapshotChunkSize(final int shardSnapshotChunkSize) {
            LOG.warn("The shard-snapshot-chunk-size configuration parameter is deprecated - "
                    + "use maximum-message-slice-size instead");
            datastoreContext.setShardSnapshotChunkSize(shardSnapshotChunkSize);
            return this;
        }

        public Builder maximumMessageSliceSize(final int maximumMessageSliceSize) {
            datastoreContext.setMaximumMessageSliceSize(maximumMessageSliceSize);
            return this;
        }

        public Builder shardPeerAddressResolver(final PeerAddressResolver resolver) {
            datastoreContext.setPeerAddressResolver(resolver);
            return this;
        }

        public Builder tempFileDirectory(final String tempFileDirectory) {
            datastoreContext.setTempFileDirectory(tempFileDirectory);
            return this;
        }

        public Builder fileBackedStreamingThresholdInMegabytes(final int fileBackedStreamingThreshold) {
            datastoreContext.setFileBackedStreamingThreshold(fileBackedStreamingThreshold * ConfigParams.MEGABYTE);
            return this;
        }

        public Builder syncIndexThreshold(final long syncIndexThreshold) {
            datastoreContext.setSyncIndexThreshold(syncIndexThreshold);
            return this;
        }

        public Builder backendAlivenessTimerIntervalInSeconds(final long interval) {
            datastoreContext.backendAlivenessTimerInterval = TimeUnit.SECONDS.toNanos(interval);
            return this;
        }

        public Builder frontendRequestTimeoutInSeconds(final long timeout) {
            datastoreContext.requestTimeout = TimeUnit.SECONDS.toNanos(timeout);
            return this;
        }

        public Builder frontendNoProgressTimeoutInSeconds(final long timeout) {
            datastoreContext.noProgressTimeout = TimeUnit.SECONDS.toNanos(timeout);
            return this;
        }

        public Builder initialPayloadSerializedBufferCapacity(final int capacity) {
            datastoreContext.initialPayloadSerializedBufferCapacity = capacity;
            return this;
        }

        @Override
        public DatastoreContext build() {
            datastoreContext.dataStoreProperties = InMemoryDOMDataStoreConfigProperties.builder()
                    .maxDataChangeExecutorPoolSize(maxShardDataChangeExecutorPoolSize)
                    .maxDataChangeExecutorQueueSize(maxShardDataChangeExecutorQueueSize)
                    .maxDataChangeListenerQueueSize(maxShardDataChangeListenerQueueSize)
                    .maxDataStoreExecutorQueueSize(maxShardDataStoreExecutorQueueSize)
                    .build();

            if (datastoreContext.dataStoreName != null) {
                GLOBAL_DATASTORE_NAMES.add(datastoreContext.dataStoreName);
            }

            return datastoreContext;
        }
    }
}
