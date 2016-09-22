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
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_OPERATION_TIMEOUT_IN_MS;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_SHARD_INITIALIZATION_TIMEOUT;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_SHARD_SNAPSHOT_DATA_THRESHOLD_PERCENTAGE;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_SHARD_TRANSACTION_IDLE_TIMEOUT;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_SHARD_TX_COMMIT_TIMEOUT_IN_SECONDS;
import java.util.Dictionary;
import java.util.Hashtable;
import org.junit.Test;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStoreConfigProperties;

/**
 * Unit tests for DatastoreContextIntrospector.
 *
 * @author Thomas Pantelis
 */
public class DatastoreContextIntrospectorTest {

    @Test
    public void testUpdate() {
        DatastoreContext context = DatastoreContext.newBuilder().
                logicalStoreType(LogicalDatastoreType.OPERATIONAL).build();
        DatastoreContextIntrospector introspector = new DatastoreContextIntrospector(context );

        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("shard-transaction-idle-timeout-in-minutes", "31");
        properties.put("operation-timeout-in-seconds", "26");
        properties.put("shard-transaction-commit-timeout-in-seconds", "100");
        properties.put("shard-journal-recovery-log-batch-size", "199");
        properties.put("shard-snapshot-batch-count", "212");
        properties.put("shard-heartbeat-interval-in-millis", "101");
        properties.put("shard-transaction-commit-queue-capacity", "567");
        properties.put("shard-initialization-timeout-in-seconds", "82");
        properties.put("shard-leader-election-timeout-in-seconds", "66");
        properties.put("shard-isolated-leader-check-interval-in-millis", "123");
        properties.put("shard-snapshot-data-threshold-percentage", "100");
        properties.put("shard-election-timeout-factor", "21");
        properties.put("shard-batched-modification-count", "901");
        properties.put("transactionCreationInitialRateLimit", "200");
        properties.put("MaxShardDataChangeExecutorPoolSize", "41");
        properties.put("Max-Shard-Data-Change Executor-Queue Size", "1111");
        properties.put(" max shard data change listener queue size", "2222");
        properties.put("mAx-shaRd-data-STORE-executor-quEUe-size", "3333");
        properties.put("persistent", "false");

        boolean updated = introspector.update(properties);
        assertEquals("updated", true, updated);
        context = introspector.getContext();

        assertEquals(31, context.getShardTransactionIdleTimeout().toMinutes());
        assertEquals(26000, context.getOperationTimeoutInMillis());
        assertEquals(100, context.getShardTransactionCommitTimeoutInSeconds());
        assertEquals(199, context.getShardRaftConfig().getJournalRecoveryLogBatchSize());
        assertEquals(212, context.getShardRaftConfig().getSnapshotBatchCount());
        assertEquals(101, context.getShardRaftConfig().getHeartBeatInterval().length());
        assertEquals(567, context.getShardTransactionCommitQueueCapacity());
        assertEquals(82, context.getShardInitializationTimeout().duration().toSeconds());
        assertEquals(66, context.getShardLeaderElectionTimeout().duration().toSeconds());
        assertEquals(123, context.getShardRaftConfig().getIsolatedCheckIntervalInMillis());
        assertEquals(100, context.getShardRaftConfig().getSnapshotDataThresholdPercentage());
        assertEquals(21, context.getShardRaftConfig().getElectionTimeoutFactor());
        assertEquals(901, context.getShardBatchedModificationCount());
        assertEquals(200, context.getTransactionCreationInitialRateLimit());
        assertEquals(41, context.getDataStoreProperties().getMaxDataChangeExecutorPoolSize());
        assertEquals(1111, context.getDataStoreProperties().getMaxDataChangeExecutorQueueSize());
        assertEquals(2222, context.getDataStoreProperties().getMaxDataChangeListenerQueueSize());
        assertEquals(3333, context.getDataStoreProperties().getMaxDataStoreExecutorQueueSize());
        assertEquals(false, context.isPersistent());

        properties.put("shard-transaction-idle-timeout-in-minutes", "32");
        properties.put("operation-timeout-in-seconds", "27");
        properties.put("shard-heartbeat-interval-in-millis", "102");
        properties.put("shard-election-timeout-factor", "22");
        properties.put("max-shard-data-change-executor-pool-size", "42");
        properties.put("max-shard-data-store-executor-queue-size", "4444");
        properties.put("persistent", "true");

        updated = introspector.update(properties);
        assertEquals("updated", true, updated);
        context = introspector.getContext();

        assertEquals(32, context.getShardTransactionIdleTimeout().toMinutes());
        assertEquals(27000, context.getOperationTimeoutInMillis());
        assertEquals(100, context.getShardTransactionCommitTimeoutInSeconds());
        assertEquals(199, context.getShardRaftConfig().getJournalRecoveryLogBatchSize());
        assertEquals(212, context.getShardRaftConfig().getSnapshotBatchCount());
        assertEquals(102, context.getShardRaftConfig().getHeartBeatInterval().length());
        assertEquals(567, context.getShardTransactionCommitQueueCapacity());
        assertEquals(82, context.getShardInitializationTimeout().duration().toSeconds());
        assertEquals(66, context.getShardLeaderElectionTimeout().duration().toSeconds());
        assertEquals(123, context.getShardRaftConfig().getIsolatedCheckIntervalInMillis());
        assertEquals(100, context.getShardRaftConfig().getSnapshotDataThresholdPercentage());
        assertEquals(22, context.getShardRaftConfig().getElectionTimeoutFactor());
        assertEquals(200, context.getTransactionCreationInitialRateLimit());
        assertEquals(42, context.getDataStoreProperties().getMaxDataChangeExecutorPoolSize());
        assertEquals(1111, context.getDataStoreProperties().getMaxDataChangeExecutorQueueSize());
        assertEquals(2222, context.getDataStoreProperties().getMaxDataChangeListenerQueueSize());
        assertEquals(4444, context.getDataStoreProperties().getMaxDataStoreExecutorQueueSize());
        assertEquals(true, context.isPersistent());

        updated = introspector.update(null);
        assertEquals("updated", false, updated);

        updated = introspector.update(new Hashtable<>());
        assertEquals("updated", false, updated);
    }


    @Test
    public void testUpdateWithInvalidValues() {
        DatastoreContext context = DatastoreContext.newBuilder().
                logicalStoreType(LogicalDatastoreType.OPERATIONAL).build();
        DatastoreContextIntrospector introspector = new DatastoreContextIntrospector(context );

        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("shard-transaction-idle-timeout-in-minutes", "0"); // bad - must be > 0
        properties.put("shard-journal-recovery-log-batch-size", "199");
        properties.put("shard-transaction-commit-timeout-in-seconds", "bogus"); // bad - NaN
        properties.put("shard-snapshot-batch-count", "212"); // good
        properties.put("operation-timeout-in-seconds", "4"); // bad - must be >= 5
        properties.put("shard-heartbeat-interval-in-millis", "99"); // bad - must be >= 100
        properties.put("shard-transaction-commit-queue-capacity", "567"); // good
        properties.put("shard-snapshot-data-threshold-percentage", "101"); // bad - must be 0-100
        properties.put("shard-initialization-timeout-in-seconds", "-1"); // bad - must be > 0
        properties.put("max-shard-data-change-executor-pool-size", "bogus"); // bad - NaN
        properties.put("unknownProperty", "1"); // bad - invalid property name

        boolean updated = introspector.update(properties);
        assertEquals("updated", true, updated);
        context = introspector.getContext();

        assertEquals(DEFAULT_SHARD_TRANSACTION_IDLE_TIMEOUT, context.getShardTransactionIdleTimeout());
        assertEquals(199, context.getShardRaftConfig().getJournalRecoveryLogBatchSize());
        assertEquals(DEFAULT_SHARD_TX_COMMIT_TIMEOUT_IN_SECONDS, context.getShardTransactionCommitTimeoutInSeconds());
        assertEquals(212, context.getShardRaftConfig().getSnapshotBatchCount());
        assertEquals(DEFAULT_OPERATION_TIMEOUT_IN_MS, context.getOperationTimeoutInMillis());
        assertEquals(DEFAULT_HEARTBEAT_INTERVAL_IN_MILLIS, context.getShardRaftConfig().getHeartBeatInterval().length());
        assertEquals(567, context.getShardTransactionCommitQueueCapacity());
        assertEquals(DEFAULT_SHARD_SNAPSHOT_DATA_THRESHOLD_PERCENTAGE,
                context.getShardRaftConfig().getSnapshotDataThresholdPercentage());
        assertEquals(DEFAULT_SHARD_INITIALIZATION_TIMEOUT, context.getShardInitializationTimeout());
        assertEquals(InMemoryDOMDataStoreConfigProperties.DEFAULT_MAX_DATA_CHANGE_EXECUTOR_POOL_SIZE,
                context.getDataStoreProperties().getMaxDataChangeExecutorPoolSize());
    }

    @Test
    public void testUpdateWithDatastoreTypeSpecificProperties() {
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("shard-transaction-idle-timeout-in-minutes", "22"); // global setting
        properties.put("operational.shard-transaction-idle-timeout-in-minutes", "33"); // operational override
        properties.put("config.shard-transaction-idle-timeout-in-minutes", "44"); // config override

        properties.put("max-shard-data-change-executor-pool-size", "222"); // global setting
        properties.put("operational.max-shard-data-change-executor-pool-size", "333"); // operational override
        properties.put("config.max-shard-data-change-executor-pool-size", "444"); // config override

        properties.put("persistent", "false"); // global setting
        properties.put("operational.Persistent", "true"); // operational override

        DatastoreContext operContext = DatastoreContext.newBuilder().
                logicalStoreType(LogicalDatastoreType.OPERATIONAL).build();
        DatastoreContextIntrospector operIntrospector = new DatastoreContextIntrospector(operContext);
        boolean updated = operIntrospector.update(properties);
        assertEquals("updated", true, updated);
        operContext = operIntrospector.getContext();

        assertEquals(33, operContext.getShardTransactionIdleTimeout().toMinutes());
        assertEquals(true, operContext.isPersistent());
        assertEquals(333, operContext.getDataStoreProperties().getMaxDataChangeExecutorPoolSize());

        DatastoreContext configContext = DatastoreContext.newBuilder()
                .logicalStoreType(LogicalDatastoreType.CONFIGURATION).build();
        DatastoreContextIntrospector configIntrospector = new DatastoreContextIntrospector(configContext);
        updated = configIntrospector.update(properties);
        assertEquals("updated", true, updated);
        configContext = configIntrospector.getContext();

        assertEquals(44, configContext.getShardTransactionIdleTimeout().toMinutes());
        assertEquals(false, configContext.isPersistent());
        assertEquals(444, configContext.getDataStoreProperties().getMaxDataChangeExecutorPoolSize());
    }

    @Test
    public void testGetDatastoreContextForShard() {
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("shard-transaction-idle-timeout-in-minutes", "22"); // global setting
        properties.put("operational.shard-transaction-idle-timeout-in-minutes", "33"); // operational override
        properties.put("config.shard-transaction-idle-timeout-in-minutes", "44"); // config override
        properties.put("topology.shard-transaction-idle-timeout-in-minutes", "55"); // global shard override

        DatastoreContext operContext = DatastoreContext.newBuilder().
                logicalStoreType(LogicalDatastoreType.OPERATIONAL).build();
        DatastoreContextIntrospector operIntrospector = new DatastoreContextIntrospector(operContext);

        DatastoreContext shardContext = operIntrospector.newContextFactory().getShardDatastoreContext("topology");
        assertEquals(10, shardContext.getShardTransactionIdleTimeout().toMinutes());

        operIntrospector.update(properties);
        operContext = operIntrospector.getContext();
        assertEquals(33, operContext.getShardTransactionIdleTimeout().toMinutes());

        shardContext = operIntrospector.newContextFactory().getShardDatastoreContext("topology");
        assertEquals(55, shardContext.getShardTransactionIdleTimeout().toMinutes());

        DatastoreContext configContext = DatastoreContext.newBuilder().
                logicalStoreType(LogicalDatastoreType.CONFIGURATION).build();
        DatastoreContextIntrospector configIntrospector = new DatastoreContextIntrospector(configContext);
        configIntrospector.update(properties);
        configContext = configIntrospector.getContext();
        assertEquals(44, configContext.getShardTransactionIdleTimeout().toMinutes());

        shardContext = configIntrospector.newContextFactory().getShardDatastoreContext("topology");
        assertEquals(55, shardContext.getShardTransactionIdleTimeout().toMinutes());

        properties.put("operational.topology.shard-transaction-idle-timeout-in-minutes", "66"); // operational shard override
        properties.put("config.topology.shard-transaction-idle-timeout-in-minutes", "77"); // config shard override

        operIntrospector.update(properties);
        shardContext = operIntrospector.newContextFactory().getShardDatastoreContext("topology");
        assertEquals(66, shardContext.getShardTransactionIdleTimeout().toMinutes());

        configIntrospector.update(properties);
        shardContext = configIntrospector.newContextFactory().getShardDatastoreContext("topology");
        assertEquals(77, shardContext.getShardTransactionIdleTimeout().toMinutes());

        shardContext = configIntrospector.newContextFactory().getShardDatastoreContext("default");
        assertEquals(44, shardContext.getShardTransactionIdleTimeout().toMinutes());
    }
}
