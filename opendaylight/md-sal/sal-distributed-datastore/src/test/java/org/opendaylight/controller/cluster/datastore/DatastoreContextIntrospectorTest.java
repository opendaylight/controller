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
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStoreConfigProperties;

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
        properties.put("shardTransactionIdleTimeoutInMinutes", "31");
        properties.put("operationTimeoutInSeconds", "26");
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
        properties.put("maxShardDataChangeExecutorPoolSize", "41");
        properties.put("maxShardDataChangeExecutorQueueSize", "1111");
        properties.put("maxShardDataChangeListenerQueueSize", "2222");
        properties.put("maxShardDataStoreExecutorQueueSize", "3333");
        properties.put("persistent", "false");

        boolean updated = introspector.update(properties);
        assertEquals("updated", true, updated);
        context = introspector.getContext();

        assertEquals(31, context.getShardTransactionIdleTimeout().toMinutes());
        assertEquals(26, context.getOperationTimeoutInSeconds());
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
        assertEquals(41, context.getDataStoreProperties().getMaxDataChangeExecutorPoolSize());
        assertEquals(1111, context.getDataStoreProperties().getMaxDataChangeExecutorQueueSize());
        assertEquals(2222, context.getDataStoreProperties().getMaxDataChangeListenerQueueSize());
        assertEquals(3333, context.getDataStoreProperties().getMaxDataStoreExecutorQueueSize());
        assertEquals(false, context.isPersistent());

        properties.put("shardTransactionIdleTimeoutInMinutes", "32");
        properties.put("operationTimeoutInSeconds", "27");
        properties.put("shardHeartbeatIntervalInMillis", "102");
        properties.put("shardElectionTimeoutFactor", "22");
        properties.put("maxShardDataChangeExecutorPoolSize", "42");
        properties.put("maxShardDataStoreExecutorQueueSize", "4444");
        properties.put("persistent", "true");

        updated = introspector.update(properties);
        assertEquals("updated", true, updated);
        context = introspector.getContext();

        assertEquals(32, context.getShardTransactionIdleTimeout().toMinutes());
        assertEquals(27, context.getOperationTimeoutInSeconds());
        assertEquals(100, context.getShardTransactionCommitTimeoutInSeconds());
        assertEquals(199, context.getShardRaftConfig().getJournalRecoveryLogBatchSize());
        assertEquals(212, context.getShardRaftConfig().getSnapshotBatchCount());
        assertEquals(102, context.getShardRaftConfig().getHeartBeatInterval().length());
        assertEquals(567, context.getShardTransactionCommitQueueCapacity());
        assertEquals(82, context.getShardInitializationTimeout().duration().toSeconds());
        assertEquals(66, context.getShardLeaderElectionTimeout().duration().toSeconds());
        assertEquals(123, context.getShardRaftConfig().getIsolatedCheckInterval().length());
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

        updated = introspector.update(new Hashtable<String, Object>());
        assertEquals("updated", false, updated);
    }


    @Test
    public void testUpdateWithInvalidValues() {
        DatastoreContext context = DatastoreContext.newBuilder().dataStoreType("operational").build();
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
        properties.put("maxShardDataChangeExecutorPoolSize", "bogus"); // bad - NaN
        properties.put("unkoownProperty", "1"); // bad - invalid property name

        boolean updated = introspector.update(properties);
        assertEquals("updated", true, updated);
        context = introspector.getContext();

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
        assertEquals(InMemoryDOMDataStoreConfigProperties.DEFAULT_MAX_DATA_CHANGE_EXECUTOR_POOL_SIZE,
                context.getDataStoreProperties().getMaxDataChangeExecutorPoolSize());
    }

    @Test
    public void testUpdateWithDatastoreTypeSpecificProperties() {
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("shardTransactionIdleTimeoutInMinutes", "22"); // global setting
        properties.put("operationalShardTransactionIdleTimeoutInMinutes", "33"); // operational override
        properties.put("configShardTransactionIdleTimeoutInMinutes", "44"); // config override

        properties.put("maxShardDataChangeExecutorPoolSize", "222"); // global setting
        properties.put("operationalMaxShardDataChangeExecutorPoolSize", "333"); // operational override
        properties.put("configMaxShardDataChangeExecutorPoolSize", "444"); // config override

        properties.put("persistent", "false"); // global setting
        properties.put("operationalPersistent", "true"); // operational override

        DatastoreContext operContext = DatastoreContext.newBuilder().dataStoreType("operational").build();
        DatastoreContextIntrospector operIntrospector = new DatastoreContextIntrospector(operContext);
        boolean updated = operIntrospector.update(properties);
        assertEquals("updated", true, updated);
        operContext = operIntrospector.getContext();

        assertEquals(33, operContext.getShardTransactionIdleTimeout().toMinutes());
        assertEquals(true, operContext.isPersistent());
        assertEquals(333, operContext.getDataStoreProperties().getMaxDataChangeExecutorPoolSize());

        DatastoreContext configContext = DatastoreContext.newBuilder().dataStoreType("config").build();
        DatastoreContextIntrospector configIntrospector = new DatastoreContextIntrospector(configContext);
        updated = configIntrospector.update(properties);
        assertEquals("updated", true, updated);
        configContext = configIntrospector.getContext();

        assertEquals(44, configContext.getShardTransactionIdleTimeout().toMinutes());
        assertEquals(false, configContext.isPersistent());
        assertEquals(444, configContext.getDataStoreProperties().getMaxDataChangeExecutorPoolSize());
    }
}
