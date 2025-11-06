/*
 * Copyright (c) 2014, 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_CONFIGURATION_READER;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_HEARTBEAT_INTERVAL_IN_MILLIS;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_INITIAL_PAYLOAD_SERIALIZED_BUFFER_CAPACITY;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_INITIAL_SETTLE_TIMEOUT_MULTIPLIER;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_ISOLATED_LEADER_CHECK_INTERVAL_IN_MILLIS;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_JOURNAL_RECOVERY_BATCH_SIZE;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_MAX_MESSAGE_SLICE_SIZE;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_OPERATION_TIMEOUT_IN_MS;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_PERSISTENT;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_RECOVERY_EXPORT_BASE_DIR;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_RECOVERY_SNAPSHOT_INTERVAL_SECONDS;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_SHARD_BATCHED_MODIFICATION_COUNT;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_SHARD_ELECTION_TIMEOUT_FACTOR;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_SHARD_INITIALIZATION_TIMEOUT;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_SHARD_LEADER_ELECTION_TIMEOUT;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_SHARD_SNAPSHOT_DATA_THRESHOLD;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_SHARD_SNAPSHOT_DATA_THRESHOLD_PERCENTAGE;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_SHARD_TRANSACTION_IDLE_TIMEOUT;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_SHARD_TX_COMMIT_QUEUE_CAPACITY;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_SHARD_TX_COMMIT_TIMEOUT_IN_SECONDS;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_SNAPSHOT_BATCH_COUNT;

import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.distributed.datastore.provider.rev250130.DataStoreProperties.ExportOnRecovery;

public class DatastoreContextTest {

    @Test
    public void testNewBuilderWithDefaultSettings() {
        DatastoreContext context = DatastoreContext.newBuilder().build();

        assertEquals(DEFAULT_SHARD_TRANSACTION_IDLE_TIMEOUT, context.getShardTransactionIdleTimeout());
        assertEquals(DEFAULT_OPERATION_TIMEOUT_IN_MS, context.getOperationTimeoutInMillis());
        assertEquals(DEFAULT_SHARD_TX_COMMIT_TIMEOUT_IN_SECONDS, context.getShardTransactionCommitTimeoutInSeconds());
        assertEquals(DEFAULT_JOURNAL_RECOVERY_BATCH_SIZE,
                context.getShardRaftConfig().getJournalRecoveryLogBatchSize());
        assertEquals(DEFAULT_SNAPSHOT_BATCH_COUNT, context.getShardRaftConfig().getSnapshotBatchCount());
        assertEquals(DEFAULT_HEARTBEAT_INTERVAL_IN_MILLIS,
                context.getShardRaftConfig().getHeartBeatInterval().toMillis());
        assertEquals(DEFAULT_SHARD_TX_COMMIT_QUEUE_CAPACITY, context.getShardTransactionCommitQueueCapacity());
        assertEquals(DEFAULT_SHARD_INITIALIZATION_TIMEOUT.duration().toMillis(),
                context.getShardInitializationTimeout().duration().toMillis());
        assertEquals(DEFAULT_SHARD_LEADER_ELECTION_TIMEOUT.toMillis(),
                context.getShardLeaderElectionTimeout().toMillis());
        assertEquals(DEFAULT_INITIAL_SETTLE_TIMEOUT_MULTIPLIER,
                context.getInitialSettleTimeoutMultiplier());
        assertEquals(DEFAULT_PERSISTENT, context.isPersistent());
        assertEquals(DEFAULT_CONFIGURATION_READER, context.getConfigurationReader());
        assertEquals(DEFAULT_ISOLATED_LEADER_CHECK_INTERVAL_IN_MILLIS,
                context.getShardRaftConfig().getIsolatedCheckIntervalInMillis());
        assertEquals(DEFAULT_SHARD_SNAPSHOT_DATA_THRESHOLD_PERCENTAGE,
                context.getShardRaftConfig().getSnapshotDataThresholdPercentage());
        assertEquals(DEFAULT_SHARD_SNAPSHOT_DATA_THRESHOLD,
                context.getShardRaftConfig().getSnapshotDataThreshold());
        assertEquals(DEFAULT_SHARD_ELECTION_TIMEOUT_FACTOR, context.getShardRaftConfig().getElectionTimeoutFactor());
        assertEquals(DatastoreContext.DEFAULT_SHARD_BATCHED_MODIFICATION_COUNT,
                context.getShardBatchedModificationCount());
        assertEquals(DEFAULT_MAX_MESSAGE_SLICE_SIZE, context.getMaximumMessageSliceSize());
        assertEquals(DEFAULT_RECOVERY_EXPORT_BASE_DIR, context.getRecoveryExportBaseDir());
    }

    @Test
    public void testNewBuilderWithCustomSettings() {
        DatastoreContext.Builder builder = DatastoreContext.newBuilder();

        builder.shardTransactionIdleTimeout(DEFAULT_SHARD_TRANSACTION_IDLE_TIMEOUT.toMillis() + 1,
                TimeUnit.MILLISECONDS);
        builder.operationTimeoutInSeconds((int) (TimeUnit.MILLISECONDS.toSeconds(DEFAULT_OPERATION_TIMEOUT_IN_MS) + 1));
        builder.shardTransactionCommitTimeoutInSeconds(DEFAULT_SHARD_TX_COMMIT_TIMEOUT_IN_SECONDS + 1);
        builder.shardJournalRecoveryLogBatchSize(DEFAULT_JOURNAL_RECOVERY_BATCH_SIZE + 1);
        builder.shardSnapshotBatchCount(DEFAULT_SNAPSHOT_BATCH_COUNT + 1);
        builder.recoverySnapshotIntervalSeconds(DEFAULT_RECOVERY_SNAPSHOT_INTERVAL_SECONDS + 1);
        builder.shardHeartbeatIntervalInMillis(DEFAULT_HEARTBEAT_INTERVAL_IN_MILLIS + 1);
        builder.shardTransactionCommitQueueCapacity(DEFAULT_SHARD_TX_COMMIT_QUEUE_CAPACITY + 1);
        builder.shardInitializationTimeout(DEFAULT_SHARD_INITIALIZATION_TIMEOUT
                .duration().toMillis() + 1, TimeUnit.MILLISECONDS);
        builder.shardInitializationTimeout(DEFAULT_SHARD_INITIALIZATION_TIMEOUT.duration().toMillis() + 1,
                TimeUnit.MILLISECONDS);
        builder.shardLeaderElectionTimeout(DEFAULT_SHARD_LEADER_ELECTION_TIMEOUT.toMillis() + 1, TimeUnit.MILLISECONDS);
        builder.initialSettleTimeoutMultiplier(DEFAULT_INITIAL_SETTLE_TIMEOUT_MULTIPLIER + 1);
        builder.persistent(!DEFAULT_PERSISTENT);
        builder.shardIsolatedLeaderCheckIntervalInMillis(DEFAULT_ISOLATED_LEADER_CHECK_INTERVAL_IN_MILLIS + 1);
        builder.shardSnapshotDataThresholdPercentage(DEFAULT_SHARD_SNAPSHOT_DATA_THRESHOLD_PERCENTAGE + 1);
        builder.shardSnapshotDataThreshold(DEFAULT_SHARD_SNAPSHOT_DATA_THRESHOLD + 1);
        builder.shardElectionTimeoutFactor(DEFAULT_SHARD_ELECTION_TIMEOUT_FACTOR + 1);
        builder.shardBatchedModificationCount(DEFAULT_SHARD_BATCHED_MODIFICATION_COUNT + 1);
        builder.maximumMessageSliceSize(DEFAULT_MAX_MESSAGE_SLICE_SIZE + 1);
        builder.initialPayloadSerializedBufferCapacity(DEFAULT_INITIAL_PAYLOAD_SERIALIZED_BUFFER_CAPACITY + 1);
        builder.exportOnRecovery(ExportOnRecovery.Json);
        builder.recoveryExportBaseDir(DEFAULT_RECOVERY_EXPORT_BASE_DIR + "-new");

        DatastoreContext context = builder.build();

        verifyCustomSettings(context);

        builder = DatastoreContext.newBuilderFrom(context);

        DatastoreContext newContext = builder.build();

        verifyCustomSettings(newContext);

        Assert.assertNotSame(context, newContext);
    }

    private static void verifyCustomSettings(final DatastoreContext context) {
        assertEquals(DEFAULT_SHARD_TRANSACTION_IDLE_TIMEOUT.toMillis() + 1,
                context.getShardTransactionIdleTimeout().toMillis());
        assertEquals(TimeUnit.MILLISECONDS.toSeconds(DEFAULT_OPERATION_TIMEOUT_IN_MS) + 1,
                TimeUnit.MILLISECONDS.toSeconds(context.getOperationTimeoutInMillis()));
        assertEquals(DEFAULT_SHARD_TX_COMMIT_TIMEOUT_IN_SECONDS + 1,
                context.getShardTransactionCommitTimeoutInSeconds());
        assertEquals(DEFAULT_JOURNAL_RECOVERY_BATCH_SIZE + 1,
                context.getShardRaftConfig().getJournalRecoveryLogBatchSize());
        assertEquals(DEFAULT_SNAPSHOT_BATCH_COUNT + 1, context.getShardRaftConfig().getSnapshotBatchCount());
        assertEquals(DEFAULT_RECOVERY_SNAPSHOT_INTERVAL_SECONDS + 1,
                context.getShardRaftConfig().getRecoverySnapshotIntervalSeconds());
        assertEquals(DEFAULT_HEARTBEAT_INTERVAL_IN_MILLIS + 1,
                context.getShardRaftConfig().getHeartBeatInterval().toMillis());
        assertEquals(DEFAULT_SHARD_TX_COMMIT_QUEUE_CAPACITY + 1, context.getShardTransactionCommitQueueCapacity());
        assertEquals(DEFAULT_SHARD_INITIALIZATION_TIMEOUT.duration().toMillis() + 1,
                context.getShardInitializationTimeout().duration().toMillis());
        assertEquals(DEFAULT_SHARD_LEADER_ELECTION_TIMEOUT.toMillis() + 1,
                context.getShardLeaderElectionTimeout().toMillis());
        assertEquals(DEFAULT_INITIAL_SETTLE_TIMEOUT_MULTIPLIER + 1,
                context.getInitialSettleTimeoutMultiplier());
        assertEquals(!DEFAULT_PERSISTENT, context.isPersistent());
        assertEquals(DEFAULT_CONFIGURATION_READER, context.getConfigurationReader());
        assertEquals(DEFAULT_ISOLATED_LEADER_CHECK_INTERVAL_IN_MILLIS + 1,
                context.getShardRaftConfig().getIsolatedCheckIntervalInMillis());
        assertEquals(DEFAULT_SHARD_SNAPSHOT_DATA_THRESHOLD_PERCENTAGE + 1,
                context.getShardRaftConfig().getSnapshotDataThresholdPercentage());
        assertEquals(DEFAULT_SHARD_SNAPSHOT_DATA_THRESHOLD + 1,
                context.getShardRaftConfig().getSnapshotDataThreshold());
        assertEquals(DEFAULT_SHARD_ELECTION_TIMEOUT_FACTOR + 1,
                context.getShardRaftConfig().getElectionTimeoutFactor());
        assertEquals(DatastoreContext.DEFAULT_SHARD_BATCHED_MODIFICATION_COUNT + 1,
                context.getShardBatchedModificationCount());
        assertEquals(DEFAULT_MAX_MESSAGE_SLICE_SIZE + 1, context.getMaximumMessageSliceSize());
        assertEquals(DEFAULT_INITIAL_PAYLOAD_SERIALIZED_BUFFER_CAPACITY + 1,
                context.getInitialPayloadSerializedBufferCapacity());
        assertEquals(DEFAULT_RECOVERY_EXPORT_BASE_DIR + "-new",
                context.getRecoveryExportBaseDir());
        assertEquals(ExportOnRecovery.Json, context.getExportOnRecovery());
    }
}
