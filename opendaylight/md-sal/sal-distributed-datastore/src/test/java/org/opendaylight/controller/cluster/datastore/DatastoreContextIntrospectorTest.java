/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_HEARTBEAT_INTERVAL_IN_MILLIS;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_OPERATION_TIMEOUT_IN_SECONDS;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_SHARD_INITIALIZATION_TIMEOUT;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_SHARD_SNAPSHOT_DATA_THRESHOLD_PERCENTAGE;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_SHARD_TRANSACTION_IDLE_TIMEOUT;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_SHARD_TX_COMMIT_TIMEOUT_IN_SECONDS;
import java.util.Dictionary;
import java.util.Hashtable;
import org.junit.Test;

/**
 * Unit tests for DatastoreContextIntrospector.
 *
 * @author Thomas Pantelis
 */
public class DatastoreContextIntrospectorTest {

    @Test
    public void testUpdate() {
        DatastoreContext context = DatastoreContext.newBuilder().dataStoreType("operational").build();
        DatastoreContextIntrospector introspector = new DatastoreContextIntrospector(context );

        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("shardTransactionIdleTimeoutInMinutes", "30");
        properties.put("operationTimeoutInSeconds", "20");
        properties.put("shardTransactionCommitTimeoutInSeconds", "100");
        properties.put("shardJournalRecoveryLogBatchSize", "199");
        properties.put("shardSnapshotBatchCount", "212");
        properties.put("shardHeartbeatIntervalInMillis", "101");
        properties.put("shardTransactionCommitQueueCapacity", "567");
        properties.put("shardInitializationTimeoutInSeconds", "82");
        properties.put("shardLeaderElectionTimeoutInSeconds", "66");
        properties.put("shardIsolatedLeaderCheckIntervalInMillis", "123");
        properties.put("shardSnapshotDataThresholdPercentage", "100");
        properties.put("shardElectionTimeoutFactor", "21");
        properties.put("shardIsolatedLeaderCheckIntervalInMillis", "123");
        properties.put("transactionCreationInitialRateLimit", "200");
        properties.put("operationalPersistent", "false");

        introspector.update(properties);

        assertEquals(30, context.getShardTransactionIdleTimeout().toMinutes());
        assertEquals(20, context.getOperationTimeoutInSeconds());
        assertEquals(100, context.getShardTransactionCommitTimeoutInSeconds());
        assertEquals(199, context.getShardRaftConfig().getJournalRecoveryLogBatchSize());
        assertEquals(212, context.getShardRaftConfig().getSnapshotBatchCount());
        assertEquals(101, context.getShardRaftConfig().getHeartBeatInterval().length());
        assertEquals(567, context.getShardTransactionCommitQueueCapacity());
        assertEquals(82, context.getShardInitializationTimeout().duration().toSeconds());
        assertEquals(66, context.getShardLeaderElectionTimeout().duration().toSeconds());
        assertEquals(123, context.getShardRaftConfig().getIsolatedCheckInterval().length());
        assertEquals(100, context.getShardRaftConfig().getSnapshotDataThresholdPercentage());
        assertEquals(21, context.getShardRaftConfig().getElectionTimeoutFactor());
        assertEquals(200, context.getTransactionCreationInitialRateLimit());
        assertEquals(false, context.isPersistent());
    }


    @Test
    public void testUpdateWithInvalidValues() {
        final DatastoreContext context = DatastoreContext.newBuilder().dataStoreType("operational").build();
        DatastoreContextIntrospector introspector = new DatastoreContextIntrospector(context );

        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("shardTransactionIdleTimeoutInMinutes", "0"); // bad - must be > 0
        properties.put("shardJournalRecoveryLogBatchSize", "199");
        properties.put("shardTransactionCommitTimeoutInSeconds", "bogus"); // bad - NaN
        properties.put("shardSnapshotBatchCount", "212"); // good
        properties.put("operationTimeoutInSeconds", "4"); // bad - must be >= 5
        properties.put("shardHeartbeatIntervalInMillis", "99"); // bad - must be >= 100
        properties.put("shardTransactionCommitQueueCapacity", "567"); // good
        properties.put("shardSnapshotDataThresholdPercentage", "101"); // bad - must be 0-100
        properties.put("shardInitializationTimeoutInSeconds", "-1"); // bad - must be > 0
        properties.put("unkoownProperty", "1"); // bad - invalid property name

        introspector.update(properties);

        assertEquals(DEFAULT_SHARD_TRANSACTION_IDLE_TIMEOUT, context.getShardTransactionIdleTimeout());
        assertEquals(199, context.getShardRaftConfig().getJournalRecoveryLogBatchSize());
        assertEquals(DEFAULT_SHARD_TX_COMMIT_TIMEOUT_IN_SECONDS, context.getShardTransactionCommitTimeoutInSeconds());
        assertEquals(212, context.getShardRaftConfig().getSnapshotBatchCount());
        assertEquals(DEFAULT_OPERATION_TIMEOUT_IN_SECONDS, context.getOperationTimeoutInSeconds());
        assertEquals(DEFAULT_HEARTBEAT_INTERVAL_IN_MILLIS, context.getShardRaftConfig().getHeartBeatInterval().length());
        assertEquals(567, context.getShardTransactionCommitQueueCapacity());
        assertEquals(DEFAULT_SHARD_SNAPSHOT_DATA_THRESHOLD_PERCENTAGE,
                context.getShardRaftConfig().getSnapshotDataThresholdPercentage());
        assertEquals(DEFAULT_SHARD_INITIALIZATION_TIMEOUT, context.getShardInitializationTimeout());
    }
}
