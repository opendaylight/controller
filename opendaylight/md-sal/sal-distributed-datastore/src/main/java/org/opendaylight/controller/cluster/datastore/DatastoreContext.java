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

import com.google.common.annotations.VisibleForTesting;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.apache.commons.text.WordUtils;
import org.apache.pekko.util.Timeout;
import org.opendaylight.controller.cluster.access.client.AbstractClientConnection;
import org.opendaylight.controller.cluster.access.client.ClientActorConfig;
import org.opendaylight.controller.cluster.common.actor.AkkaConfigurationReader;
import org.opendaylight.controller.cluster.common.actor.FileAkkaConfigurationReader;
import org.opendaylight.controller.cluster.raft.ConfigParams;
import org.opendaylight.controller.cluster.raft.DefaultConfigParamsImpl;
import org.opendaylight.controller.cluster.raft.PeerAddressResolver;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.raft.spi.CompressionType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.distributed.datastore.provider.rev250130.DataStoreProperties.ExportOnRecovery;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Contains contextual data for a data store.
 *
 * @author Thomas Pantelis
 */
// Non-final for mocking
public class DatastoreContext implements ClientActorConfig {
    public static final String METRICS_DOMAIN = "org.opendaylight.controller.cluster.datastore";

    public static final Duration DEFAULT_SHARD_TRANSACTION_IDLE_TIMEOUT = Duration.ofMinutes(10);
    public static final int DEFAULT_OPERATION_TIMEOUT_IN_MS = 5000;
    public static final int DEFAULT_SHARD_TX_COMMIT_TIMEOUT_IN_SECONDS = 30;
    public static final int DEFAULT_JOURNAL_RECOVERY_BATCH_SIZE = 1;
    public static final int DEFAULT_SNAPSHOT_BATCH_COUNT = 20000;
    public static final int DEFAULT_RECOVERY_SNAPSHOT_INTERVAL_SECONDS = 0;
    public static final int DEFAULT_HEARTBEAT_INTERVAL_IN_MILLIS = 500;
    public static final int DEFAULT_ISOLATED_LEADER_CHECK_INTERVAL_IN_MILLIS =
            DEFAULT_HEARTBEAT_INTERVAL_IN_MILLIS * 10;
    public static final int DEFAULT_SHARD_TX_COMMIT_QUEUE_CAPACITY = 50000;
    public static final Timeout DEFAULT_SHARD_INITIALIZATION_TIMEOUT = new Timeout(5, TimeUnit.MINUTES);
    public static final Timeout DEFAULT_SHARD_LEADER_ELECTION_TIMEOUT = new Timeout(30, TimeUnit.SECONDS);
    public static final int DEFAULT_INITIAL_SETTLE_TIMEOUT_MULTIPLIER = 3;
    public static final boolean DEFAULT_PERSISTENT = true;
    public static final boolean DEFAULT_SNAPSHOT_ON_ROOT_OVERWRITE = false;
    public static final FileAkkaConfigurationReader DEFAULT_CONFIGURATION_READER = new FileAkkaConfigurationReader();
    public static final int DEFAULT_SHARD_SNAPSHOT_DATA_THRESHOLD_PERCENTAGE = 12;
    public static final int DEFAULT_SHARD_SNAPSHOT_DATA_THRESHOLD = 0;
    public static final int DEFAULT_SHARD_ELECTION_TIMEOUT_FACTOR = 2;
    public static final int DEFAULT_SHARD_CANDIDATE_ELECTION_TIMEOUT_DIVISOR = 1;
    public static final String UNKNOWN_DATA_STORE_TYPE = "unknown";
    public static final int DEFAULT_SHARD_BATCHED_MODIFICATION_COUNT = 1000;
    public static final long DEFAULT_SHARD_COMMIT_QUEUE_EXPIRY_TIMEOUT_IN_MS =
            TimeUnit.MILLISECONDS.convert(2, TimeUnit.MINUTES);
    public static final int DEFAULT_MAX_MESSAGE_SLICE_SIZE = 480 * 1024; // 480KiB
    public static final int DEFAULT_INITIAL_PAYLOAD_SERIALIZED_BUFFER_CAPACITY = 512;
    public static final ExportOnRecovery DEFAULT_EXPORT_ON_RECOVERY = ExportOnRecovery.Off;
    public static final String DEFAULT_RECOVERY_EXPORT_BASE_DIR = "persistence-export";

    public static final long DEFAULT_SYNC_INDEX_THRESHOLD = 10;

    private final DefaultConfigParamsImpl raftConfig = new DefaultConfigParamsImpl();

    private Duration shardTransactionIdleTimeout = DatastoreContext.DEFAULT_SHARD_TRANSACTION_IDLE_TIMEOUT;
    private long operationTimeoutInMillis = DEFAULT_OPERATION_TIMEOUT_IN_MS;
    private String dataStoreMXBeanType;
    private int shardTransactionCommitTimeoutInSeconds = DEFAULT_SHARD_TX_COMMIT_TIMEOUT_IN_SECONDS;
    private int shardTransactionCommitQueueCapacity = DEFAULT_SHARD_TX_COMMIT_QUEUE_CAPACITY;
    private Timeout shardInitializationTimeout = DEFAULT_SHARD_INITIALIZATION_TIMEOUT;
    private Timeout shardLeaderElectionTimeout = DEFAULT_SHARD_LEADER_ELECTION_TIMEOUT;
    private int initialSettleTimeoutMultiplier = DEFAULT_INITIAL_SETTLE_TIMEOUT_MULTIPLIER;
    private boolean persistent = DEFAULT_PERSISTENT;
    private boolean snapshotOnRootOverwrite = DEFAULT_SNAPSHOT_ON_ROOT_OVERWRITE;
    private AkkaConfigurationReader configurationReader = DEFAULT_CONFIGURATION_READER;
    private String dataStoreName = UNKNOWN_DATA_STORE_TYPE;
    private LogicalDatastoreType logicalStoreType = LogicalDatastoreType.OPERATIONAL;
    private YangInstanceIdentifier storeRoot = YangInstanceIdentifier.of();
    private int shardBatchedModificationCount = DEFAULT_SHARD_BATCHED_MODIFICATION_COUNT;
    private boolean writeOnlyTransactionOptimizationsEnabled = true;
    private long shardCommitQueueExpiryTimeoutInMillis = DEFAULT_SHARD_COMMIT_QUEUE_EXPIRY_TIMEOUT_IN_MS;
    private boolean transactionDebugContextEnabled = false;
    private String shardManagerPersistenceId;
    private int maximumMessageSliceSize = DEFAULT_MAX_MESSAGE_SLICE_SIZE;
    private long backendAlivenessTimerInterval = AbstractClientConnection.DEFAULT_BACKEND_ALIVE_TIMEOUT_NANOS;
    private long requestTimeout = AbstractClientConnection.DEFAULT_REQUEST_TIMEOUT_NANOS;
    private long noProgressTimeout = AbstractClientConnection.DEFAULT_NO_PROGRESS_TIMEOUT_NANOS;
    private int initialPayloadSerializedBufferCapacity = DEFAULT_INITIAL_PAYLOAD_SERIALIZED_BUFFER_CAPACITY;
    private boolean useLz4Compression = false;
    private ExportOnRecovery exportOnRecovery = DEFAULT_EXPORT_ON_RECOVERY;
    private String recoveryExportBaseDir = DEFAULT_RECOVERY_EXPORT_BASE_DIR;

    DatastoreContext() {
        setShardJournalRecoveryLogBatchSize(DEFAULT_JOURNAL_RECOVERY_BATCH_SIZE);
        setSnapshotBatchCount(DEFAULT_SNAPSHOT_BATCH_COUNT);
        setRecoverySnapshotIntervalSeconds(DEFAULT_RECOVERY_SNAPSHOT_INTERVAL_SECONDS);
        setHeartbeatInterval(DEFAULT_HEARTBEAT_INTERVAL_IN_MILLIS);
        setIsolatedLeaderCheckInterval(DEFAULT_ISOLATED_LEADER_CHECK_INTERVAL_IN_MILLIS);
        setSnapshotDataThresholdPercentage(DEFAULT_SHARD_SNAPSHOT_DATA_THRESHOLD_PERCENTAGE);
        setSnapshotDataThreshold(DEFAULT_SHARD_SNAPSHOT_DATA_THRESHOLD);
        setElectionTimeoutFactor(DEFAULT_SHARD_ELECTION_TIMEOUT_FACTOR);
        setCandidateElectionTimeoutDivisor(DEFAULT_SHARD_CANDIDATE_ELECTION_TIMEOUT_DIVISOR);
        setSyncIndexThreshold(DEFAULT_SYNC_INDEX_THRESHOLD);
        setMaximumMessageSliceSize(DEFAULT_MAX_MESSAGE_SLICE_SIZE);
    }

    private DatastoreContext(final DatastoreContext other) {
        shardTransactionIdleTimeout = other.shardTransactionIdleTimeout;
        operationTimeoutInMillis = other.operationTimeoutInMillis;
        dataStoreMXBeanType = other.dataStoreMXBeanType;
        shardTransactionCommitTimeoutInSeconds = other.shardTransactionCommitTimeoutInSeconds;
        shardTransactionCommitQueueCapacity = other.shardTransactionCommitQueueCapacity;
        shardInitializationTimeout = other.shardInitializationTimeout;
        shardLeaderElectionTimeout = other.shardLeaderElectionTimeout;
        initialSettleTimeoutMultiplier = other.initialSettleTimeoutMultiplier;
        persistent = other.persistent;
        snapshotOnRootOverwrite = other.snapshotOnRootOverwrite;
        configurationReader = other.configurationReader;
        dataStoreName = other.dataStoreName;
        logicalStoreType = other.logicalStoreType;
        storeRoot = other.storeRoot;
        shardBatchedModificationCount = other.shardBatchedModificationCount;
        writeOnlyTransactionOptimizationsEnabled = other.writeOnlyTransactionOptimizationsEnabled;
        shardCommitQueueExpiryTimeoutInMillis = other.shardCommitQueueExpiryTimeoutInMillis;
        transactionDebugContextEnabled = other.transactionDebugContextEnabled;
        shardManagerPersistenceId = other.shardManagerPersistenceId;
        backendAlivenessTimerInterval = other.backendAlivenessTimerInterval;
        requestTimeout = other.requestTimeout;
        noProgressTimeout = other.noProgressTimeout;
        initialPayloadSerializedBufferCapacity = other.initialPayloadSerializedBufferCapacity;
        useLz4Compression = other.useLz4Compression;
        raftConfig.setPreferredCompression(useLz4Compression ? CompressionType.LZ4 : CompressionType.NONE);
        exportOnRecovery = other.exportOnRecovery;
        recoveryExportBaseDir = other.recoveryExportBaseDir;

        setShardJournalRecoveryLogBatchSize(other.raftConfig.getJournalRecoveryLogBatchSize());
        setSnapshotBatchCount(other.raftConfig.getSnapshotBatchCount());
        setRecoverySnapshotIntervalSeconds(other.raftConfig.getRecoverySnapshotIntervalSeconds());
        setHeartbeatInterval(other.raftConfig.getHeartBeatInterval().toMillis());
        setIsolatedLeaderCheckInterval(other.raftConfig.getIsolatedCheckIntervalInMillis());
        setSnapshotDataThresholdPercentage(other.raftConfig.getSnapshotDataThresholdPercentage());
        setSnapshotDataThreshold(other.raftConfig.getSnapshotDataThreshold());
        setElectionTimeoutFactor(other.raftConfig.getElectionTimeoutFactor());
        setCandidateElectionTimeoutDivisor(other.raftConfig.getCandidateElectionTimeoutDivisor());
        setCustomRaftPolicyImplementation(other.raftConfig.getCustomRaftPolicyImplementationClass());
        setMaximumMessageSliceSize(other.getMaximumMessageSliceSize());
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

    public boolean isSnapshotOnRootOverwrite() {
        return snapshotOnRootOverwrite;
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

    public String getShardManagerPersistenceId() {
        return shardManagerPersistenceId;
    }

    @Override
    public Path getTempFileDirectory() {
        return raftConfig.getTempFileDirectory();
    }

    private void setTempFileDirectory(final Path tempFileDirectory) {
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
        raftConfig.setHeartBeatInterval(Duration.ofMillis(shardHeartbeatIntervalInMillis));
    }

    private void setShardJournalRecoveryLogBatchSize(final int shardJournalRecoveryLogBatchSize) {
        raftConfig.setJournalRecoveryLogBatchSize(shardJournalRecoveryLogBatchSize);
    }


    private void setIsolatedLeaderCheckInterval(final long shardIsolatedLeaderCheckIntervalInMillis) {
        raftConfig.setIsolatedLeaderCheckInterval(Duration.ofMillis(shardIsolatedLeaderCheckIntervalInMillis));
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

    private void setSnapshotDataThreshold(final int shardSnapshotDataThreshold) {
        checkArgument(shardSnapshotDataThreshold >= 0);
        raftConfig.setSnapshotDataThreshold(shardSnapshotDataThreshold);
    }

    private void setSnapshotBatchCount(final long shardSnapshotBatchCount) {
        raftConfig.setSnapshotBatchCount(shardSnapshotBatchCount);
    }

    /**
     * Set the interval in seconds after which a snapshot should be taken during the recovery process.
     * 0 means don't take snapshots
     */
    private void setRecoverySnapshotIntervalSeconds(final int recoverySnapshotInterval) {
        raftConfig.setRecoverySnapshotIntervalSeconds(recoverySnapshotInterval);
    }

    private void setMaximumMessageSliceSize(final int maximumMessageSliceSize) {
        raftConfig.setMaximumMessageSliceSize(maximumMessageSliceSize);
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

    public boolean isUseLz4Compression() {
        return useLz4Compression;
    }

    public ExportOnRecovery getExportOnRecovery() {
        return exportOnRecovery;
    }

    public String getRecoveryExportBaseDir() {
        return recoveryExportBaseDir;
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

    public static class Builder {
        private final DatastoreContext datastoreContext;

        Builder(final DatastoreContext datastoreContext) {
            this.datastoreContext = datastoreContext;
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
            datastoreContext.shardTransactionIdleTimeout = Duration.of(timeout, unit.toChronoUnit());
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

        public Builder recoverySnapshotIntervalSeconds(final int recoverySnapshotIntervalSeconds) {
            checkArgument(recoverySnapshotIntervalSeconds >= 0);
            datastoreContext.setRecoverySnapshotIntervalSeconds(recoverySnapshotIntervalSeconds);
            return this;
        }

        public Builder shardSnapshotDataThresholdPercentage(final int shardSnapshotDataThresholdPercentage) {
            datastoreContext.setSnapshotDataThresholdPercentage(shardSnapshotDataThresholdPercentage);
            return this;
        }

        public Builder shardSnapshotDataThreshold(final int shardSnapshotDataThreshold) {
            datastoreContext.setSnapshotDataThreshold(shardSnapshotDataThreshold);
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

        public Builder initialSettleTimeoutMultiplier(final int multiplier) {
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

        public Builder snapshotOnRootOverwrite(final boolean snapshotOnRootOverwrite) {
            datastoreContext.snapshotOnRootOverwrite = snapshotOnRootOverwrite;
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

        @Deprecated(since = "11.0.0", forRemoval = true)
        public Builder transactionCreationInitialRateLimit(final long initialRateLimit) {
            // no-op
            return this;
        }

        public Builder logicalStoreType(final LogicalDatastoreType logicalStoreType) {
            datastoreContext.logicalStoreType = requireNonNull(logicalStoreType);

            // Retain compatible naming
            dataStoreName(switch (logicalStoreType) {
                case CONFIGURATION -> "config";
                case OPERATIONAL -> "operational";
            });

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
            datastoreContext.shardCommitQueueExpiryTimeoutInMillis = TimeUnit.SECONDS.toMillis(value);
            return this;
        }

        public Builder transactionDebugContextEnabled(final boolean value) {
            datastoreContext.transactionDebugContextEnabled = value;
            return this;
        }

        public Builder useLz4Compression(final boolean value) {
            datastoreContext.useLz4Compression = value;
            datastoreContext.raftConfig.setPreferredCompression(
                value ? CompressionType.LZ4 : CompressionType.NONE);
            return this;
        }

        public Builder exportOnRecovery(final ExportOnRecovery value) {
            datastoreContext.exportOnRecovery = value;
            return this;
        }

        public Builder recoveryExportBaseDir(final String value) {
            datastoreContext.recoveryExportBaseDir = value;
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

        public Builder maximumMessageSliceSize(final int maximumMessageSliceSize) {
            datastoreContext.setMaximumMessageSliceSize(maximumMessageSliceSize);
            return this;
        }

        public Builder shardPeerAddressResolver(final PeerAddressResolver resolver) {
            datastoreContext.setPeerAddressResolver(resolver);
            return this;
        }

        @Deprecated
        public Builder tempFileDirectory(final String tempFileDirectory) {
            return tempFileDirectory(Path.of(tempFileDirectory));
        }

        public Builder tempFileDirectory(final Path tempFileDirectory) {
            datastoreContext.setTempFileDirectory(tempFileDirectory);
            return this;
        }

        public Builder fileBackedStreamingThresholdInMegabytes(final int fileBackedStreamingThreshold) {
            datastoreContext.setFileBackedStreamingThreshold(fileBackedStreamingThreshold * 1_048_576);
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

        public DatastoreContext build() {
            return datastoreContext;
        }
    }
}
