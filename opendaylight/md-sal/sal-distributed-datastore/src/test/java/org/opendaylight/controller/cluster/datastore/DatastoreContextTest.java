package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_CONFIGURATION_READER;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_HEARTBEAT_INTERVAL_IN_MILLIS;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_ISOLATED_LEADER_CHECK_INTERVAL_IN_MILLIS;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_JOURNAL_RECOVERY_BATCH_SIZE;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_OPERATION_TIMEOUT_IN_SECONDS;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_PERSISTENT;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_SHARD_BATCHED_MODIFICATION_COUNT;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_SHARD_ELECTION_TIMEOUT_FACTOR;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_SHARD_INITIALIZATION_TIMEOUT;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_SHARD_LEADER_ELECTION_TIMEOUT;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_SHARD_SNAPSHOT_DATA_THRESHOLD_PERCENTAGE;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_SHARD_TRANSACTION_IDLE_TIMEOUT;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_SHARD_TX_COMMIT_QUEUE_CAPACITY;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_SHARD_TX_COMMIT_TIMEOUT_IN_SECONDS;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_SNAPSHOT_BATCH_COUNT;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_TX_CREATION_INITIAL_RATE_LIMIT;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStoreConfigProperties;

public class DatastoreContextTest {

    @Test
    public void testNewBuilderWithDefaultSettings() {
        DatastoreContext context = DatastoreContext.newBuilder().build();

        assertEquals(DEFAULT_SHARD_TRANSACTION_IDLE_TIMEOUT, context.getShardTransactionIdleTimeout());
        assertEquals(DEFAULT_OPERATION_TIMEOUT_IN_SECONDS, context.getOperationTimeoutInSeconds());
        assertEquals(DEFAULT_SHARD_TX_COMMIT_TIMEOUT_IN_SECONDS, context.getShardTransactionCommitTimeoutInSeconds());
        assertEquals(DEFAULT_JOURNAL_RECOVERY_BATCH_SIZE, context.getShardRaftConfig().getJournalRecoveryLogBatchSize());
        assertEquals(DEFAULT_SNAPSHOT_BATCH_COUNT, context.getShardRaftConfig().getSnapshotBatchCount());
        assertEquals(DEFAULT_HEARTBEAT_INTERVAL_IN_MILLIS, context.getShardRaftConfig().getHeartBeatInterval().length());
        assertEquals(DEFAULT_SHARD_TX_COMMIT_QUEUE_CAPACITY, context.getShardTransactionCommitQueueCapacity());
        assertEquals(DEFAULT_SHARD_INITIALIZATION_TIMEOUT.duration().toMillis(),
                context.getShardInitializationTimeout().duration().toMillis());
        assertEquals(DEFAULT_SHARD_LEADER_ELECTION_TIMEOUT.duration().toMillis(),
                context.getShardLeaderElectionTimeout().duration().toMillis());
        assertEquals(DEFAULT_PERSISTENT, context.isPersistent());
        assertEquals(DEFAULT_CONFIGURATION_READER, context.getConfigurationReader());
        assertEquals(DEFAULT_ISOLATED_LEADER_CHECK_INTERVAL_IN_MILLIS, context.getShardRaftConfig().getIsolatedCheckIntervalInMillis());
        assertEquals(DEFAULT_SHARD_SNAPSHOT_DATA_THRESHOLD_PERCENTAGE, context.getShardRaftConfig().getSnapshotDataThresholdPercentage());
        assertEquals(DEFAULT_SHARD_ELECTION_TIMEOUT_FACTOR, context.getShardRaftConfig().getElectionTimeoutFactor());
        assertEquals(DEFAULT_TX_CREATION_INITIAL_RATE_LIMIT, context.getTransactionCreationInitialRateLimit());
        assertEquals(DatastoreContext.DEFAULT_SHARD_BATCHED_MODIFICATION_COUNT, context.getShardBatchedModificationCount());
        assertEquals(InMemoryDOMDataStoreConfigProperties.DEFAULT_MAX_DATA_CHANGE_EXECUTOR_POOL_SIZE,
                context.getDataStoreProperties().getMaxDataChangeExecutorPoolSize());
        assertEquals(InMemoryDOMDataStoreConfigProperties.DEFAULT_MAX_DATA_CHANGE_EXECUTOR_QUEUE_SIZE,
                context.getDataStoreProperties().getMaxDataChangeExecutorQueueSize());
        assertEquals(InMemoryDOMDataStoreConfigProperties.DEFAULT_MAX_DATA_CHANGE_LISTENER_QUEUE_SIZE,
                context.getDataStoreProperties().getMaxDataChangeListenerQueueSize());
        assertEquals(InMemoryDOMDataStoreConfigProperties.DEFAULT_MAX_DATA_STORE_EXECUTOR_QUEUE_SIZE,
                context.getDataStoreProperties().getMaxDataStoreExecutorQueueSize());
    }

    @Test
    public void testNewBuilderWithCustomSettings() {
        DatastoreContext.Builder builder = DatastoreContext.newBuilder();

        builder.shardTransactionIdleTimeout(DEFAULT_SHARD_TRANSACTION_IDLE_TIMEOUT.toMillis() + 1,
                TimeUnit.MILLISECONDS);
        builder.operationTimeoutInSeconds(DEFAULT_OPERATION_TIMEOUT_IN_SECONDS + 1);
        builder.shardTransactionCommitTimeoutInSeconds(DEFAULT_SHARD_TX_COMMIT_TIMEOUT_IN_SECONDS + 1);
        builder.shardJournalRecoveryLogBatchSize(DEFAULT_JOURNAL_RECOVERY_BATCH_SIZE + 1);
        builder.shardSnapshotBatchCount(DEFAULT_SNAPSHOT_BATCH_COUNT + 1);
        builder.shardHeartbeatIntervalInMillis(DEFAULT_HEARTBEAT_INTERVAL_IN_MILLIS + 1);
        builder.shardTransactionCommitQueueCapacity(DEFAULT_SHARD_TX_COMMIT_QUEUE_CAPACITY + 1);
        builder.shardInitializationTimeout(DEFAULT_SHARD_INITIALIZATION_TIMEOUT.
                duration().toMillis() + 1, TimeUnit.MILLISECONDS);
        builder.shardInitializationTimeout(DEFAULT_SHARD_INITIALIZATION_TIMEOUT.duration().toMillis() + 1,
                TimeUnit.MILLISECONDS);
        builder.shardLeaderElectionTimeout(DEFAULT_SHARD_LEADER_ELECTION_TIMEOUT.duration().toMillis() + 1,
                TimeUnit.MILLISECONDS);
        builder.persistent(!DEFAULT_PERSISTENT);
        builder.shardIsolatedLeaderCheckIntervalInMillis(DEFAULT_ISOLATED_LEADER_CHECK_INTERVAL_IN_MILLIS + 1);
        builder.shardSnapshotDataThresholdPercentage(DEFAULT_SHARD_SNAPSHOT_DATA_THRESHOLD_PERCENTAGE + 1);
        builder.shardElectionTimeoutFactor(DEFAULT_SHARD_ELECTION_TIMEOUT_FACTOR + 1);
        builder.transactionCreationInitialRateLimit(DEFAULT_TX_CREATION_INITIAL_RATE_LIMIT + 1);
        builder.shardBatchedModificationCount(DEFAULT_SHARD_BATCHED_MODIFICATION_COUNT + 1);
        builder.maxShardDataChangeExecutorPoolSize(
                InMemoryDOMDataStoreConfigProperties.DEFAULT_MAX_DATA_CHANGE_EXECUTOR_POOL_SIZE + 1);
        builder.maxShardDataChangeExecutorQueueSize(
                InMemoryDOMDataStoreConfigProperties.DEFAULT_MAX_DATA_CHANGE_EXECUTOR_QUEUE_SIZE + 1);
        builder.maxShardDataChangeListenerQueueSize(
                InMemoryDOMDataStoreConfigProperties.DEFAULT_MAX_DATA_CHANGE_LISTENER_QUEUE_SIZE + 1);
        builder.maxShardDataStoreExecutorQueueSize(
                InMemoryDOMDataStoreConfigProperties.DEFAULT_MAX_DATA_STORE_EXECUTOR_QUEUE_SIZE + 1);

        DatastoreContext context = builder.build();

        verifyCustomSettings(context);

        builder = DatastoreContext.newBuilderFrom(context);

        DatastoreContext newContext = builder.build();

        verifyCustomSettings(newContext);

        Assert.assertNotSame(context, newContext);
    }

    private void verifyCustomSettings(DatastoreContext context) {
        assertEquals(DEFAULT_SHARD_TRANSACTION_IDLE_TIMEOUT.toMillis() + 1,
                context.getShardTransactionIdleTimeout().toMillis());
        assertEquals(DEFAULT_OPERATION_TIMEOUT_IN_SECONDS + 1, context.getOperationTimeoutInSeconds());
        assertEquals(DEFAULT_SHARD_TX_COMMIT_TIMEOUT_IN_SECONDS + 1,
                context.getShardTransactionCommitTimeoutInSeconds());
        assertEquals(DEFAULT_JOURNAL_RECOVERY_BATCH_SIZE + 1,
                context.getShardRaftConfig().getJournalRecoveryLogBatchSize());
        assertEquals(DEFAULT_SNAPSHOT_BATCH_COUNT + 1, context.getShardRaftConfig().getSnapshotBatchCount());
        assertEquals(DEFAULT_HEARTBEAT_INTERVAL_IN_MILLIS + 1,
                context.getShardRaftConfig().getHeartBeatInterval().length());
        assertEquals(DEFAULT_SHARD_TX_COMMIT_QUEUE_CAPACITY + 1, context.getShardTransactionCommitQueueCapacity());
        assertEquals(DEFAULT_SHARD_INITIALIZATION_TIMEOUT.duration().toMillis() + 1,
                context.getShardInitializationTimeout().duration().toMillis());
        assertEquals(DEFAULT_SHARD_LEADER_ELECTION_TIMEOUT.duration().toMillis() + 1,
                context.getShardLeaderElectionTimeout().duration().toMillis());
        assertEquals(!DEFAULT_PERSISTENT, context.isPersistent());
        assertEquals(DEFAULT_CONFIGURATION_READER, context.getConfigurationReader());
        assertEquals(DEFAULT_ISOLATED_LEADER_CHECK_INTERVAL_IN_MILLIS + 1,
                context.getShardRaftConfig().getIsolatedCheckIntervalInMillis());
        assertEquals(DEFAULT_SHARD_SNAPSHOT_DATA_THRESHOLD_PERCENTAGE + 1,
                context.getShardRaftConfig().getSnapshotDataThresholdPercentage());
        assertEquals(DEFAULT_SHARD_ELECTION_TIMEOUT_FACTOR + 1, context.getShardRaftConfig().getElectionTimeoutFactor());
        assertEquals(DEFAULT_TX_CREATION_INITIAL_RATE_LIMIT + 1, context.getTransactionCreationInitialRateLimit());
        assertEquals(DatastoreContext.DEFAULT_SHARD_BATCHED_MODIFICATION_COUNT + 1,
                context.getShardBatchedModificationCount());
        assertEquals(InMemoryDOMDataStoreConfigProperties.DEFAULT_MAX_DATA_CHANGE_EXECUTOR_POOL_SIZE + 1,
                context.getDataStoreProperties().getMaxDataChangeExecutorPoolSize());
        assertEquals(InMemoryDOMDataStoreConfigProperties.DEFAULT_MAX_DATA_CHANGE_EXECUTOR_QUEUE_SIZE + 1,
                context.getDataStoreProperties().getMaxDataChangeExecutorQueueSize());
        assertEquals(InMemoryDOMDataStoreConfigProperties.DEFAULT_MAX_DATA_CHANGE_LISTENER_QUEUE_SIZE + 1,
                context.getDataStoreProperties().getMaxDataChangeListenerQueueSize());
        assertEquals(InMemoryDOMDataStoreConfigProperties.DEFAULT_MAX_DATA_STORE_EXECUTOR_QUEUE_SIZE + 1,
                context.getDataStoreProperties().getMaxDataStoreExecutorQueueSize());
    }
}
