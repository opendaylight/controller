package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

public class DatastoreContextTest {

    private DatastoreContext.Builder builder;

    @Before
    public void setUp(){
        builder = new DatastoreContext.Builder();
    }

    @Test
    public void testDefaults(){
        DatastoreContext build = builder.build();

        assertEquals(DatastoreContext.DEFAULT_SHARD_TRANSACTION_IDLE_TIMEOUT , build.getShardTransactionIdleTimeout());
        assertEquals(DatastoreContext.DEFAULT_OPERATION_TIMEOUT_IN_SECONDS, build.getOperationTimeoutInSeconds());
        assertEquals(DatastoreContext.DEFAULT_SHARD_TX_COMMIT_TIMEOUT_IN_SECONDS, build.getShardTransactionCommitTimeoutInSeconds());
        assertEquals(DatastoreContext.DEFAULT_JOURNAL_RECOVERY_BATCH_SIZE, build.getShardRaftConfig().getJournalRecoveryLogBatchSize());
        assertEquals(DatastoreContext.DEFAULT_SNAPSHOT_BATCH_COUNT, build.getShardRaftConfig().getSnapshotBatchCount());
        assertEquals(DatastoreContext.DEFAULT_HEARTBEAT_INTERVAL_IN_MILLIS, build.getShardRaftConfig().getHeartBeatInterval().length());
        assertEquals(DatastoreContext.DEFAULT_SHARD_TX_COMMIT_QUEUE_CAPACITY, build.getShardTransactionCommitQueueCapacity());
        assertEquals(DatastoreContext.DEFAULT_SHARD_INITIALIZATION_TIMEOUT, build.getShardInitializationTimeout());
        assertEquals(DatastoreContext.DEFAULT_SHARD_LEADER_ELECTION_TIMEOUT, build.getShardLeaderElectionTimeout());
        assertEquals(DatastoreContext.DEFAULT_PERSISTENT, build.isPersistent());
        assertEquals(DatastoreContext.DEFAULT_CONFIGURATION_READER, build.getConfigurationReader());
        assertEquals(DatastoreContext.DEFAULT_ISOLATED_LEADER_CHECK_INTERVAL_IN_MILLIS, build.getShardRaftConfig().getIsolatedCheckInterval().length());
        assertEquals(DatastoreContext.DEFAULT_SHARD_SNAPSHOT_DATA_THRESHOLD_PERCENTAGE, build.getShardRaftConfig().getSnapshotDataThresholdPercentage());
        assertEquals(DatastoreContext.DEFAULT_SHARD_ELECTION_TIMEOUT_FACTOR, build.getShardRaftConfig().getElectionTimeoutFactor());
        assertEquals(DatastoreContext.DEFAULT_TX_CREATION_INITIAL_RATE_LIMIT, build.getTransactionCreationInitialRateLimit());
        assertEquals(DatastoreContext.DEFAULT_SHARD_BATCHED_MODIFICATION_COUNT, build.getShardBatchedModificationCount());
    }

}