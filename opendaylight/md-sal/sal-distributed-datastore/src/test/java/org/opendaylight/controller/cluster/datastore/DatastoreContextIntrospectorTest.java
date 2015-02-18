/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
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
        properties.put("shardSnapshotDataThresholdPercentage", "89");
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
        assertEquals(89, context.getShardRaftConfig().getSnapshotDataThresholdPercentage());
        assertEquals(21, context.getShardRaftConfig().getElectionTimeoutFactor());
        assertEquals(200, context.getTransactionCreationInitialRateLimit());
        assertEquals(false, context.isPersistent());
    }
}
