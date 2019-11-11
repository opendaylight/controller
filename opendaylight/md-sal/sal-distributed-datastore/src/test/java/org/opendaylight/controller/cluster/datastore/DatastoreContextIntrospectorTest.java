/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_HEARTBEAT_INTERVAL_IN_MILLIS;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_OPERATION_TIMEOUT_IN_MS;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_SHARD_INITIALIZATION_TIMEOUT;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_SHARD_SNAPSHOT_DATA_THRESHOLD_PERCENTAGE;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_SHARD_TRANSACTION_IDLE_TIMEOUT;
import static org.opendaylight.controller.cluster.datastore.DatastoreContext.DEFAULT_SHARD_TX_COMMIT_TIMEOUT_IN_SECONDS;
import static org.opendaylight.mdsal.common.api.LogicalDatastoreType.CONFIGURATION;
import static org.opendaylight.mdsal.common.api.LogicalDatastoreType.OPERATIONAL;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.opendaylight.mdsal.binding.generator.impl.GeneratedClassLoadingStrategy;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.mdsal.binding.spec.reflect.BindingReflections;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataStoreConfigProperties;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.distributed.datastore.provider.rev140612.DataStorePropertiesContainer;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

/**
 * Unit tests for DatastoreContextIntrospector.
 *
 * @author Thomas Pantelis
 */
@SuppressWarnings("checkstyle:IllegalCatch")
public class DatastoreContextIntrospectorTest {

    static SchemaContext SCHEMA_CONTEXT;
    static DatastoreContextIntrospectorFactory INTROSPECTOR_FACTORY;

    static {
        final ModuleInfoBackedContext moduleContext = ModuleInfoBackedContext.create();
        try {
            moduleContext.addModuleInfos(Arrays.asList(
                    BindingReflections.getModuleInfo(DataStorePropertiesContainer.class)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        SCHEMA_CONTEXT = moduleContext.tryToCreateSchemaContext().get();

        DOMSchemaService mockSchemaService = mock(DOMSchemaService.class);
        doReturn(SCHEMA_CONTEXT).when(mockSchemaService).getGlobalContext();
        INTROSPECTOR_FACTORY = new DatastoreContextIntrospectorFactory(mockSchemaService,
                GeneratedClassLoadingStrategy.getTCCLClassLoadingStrategy());
    }

    @Test
    public void testYangDefaults() {
        final DatastoreContextIntrospector introspector = INTROSPECTOR_FACTORY.newInstance(
                DatastoreContext.newBuilder().shardBatchedModificationCount(2)
                .transactionDebugContextEnabled(true).build());
        DatastoreContext context = introspector.getContext();

        assertEquals(1000, context.getShardBatchedModificationCount());
        assertFalse(context.isTransactionDebugContextEnabled());
    }

    @Test
    public void testUpdate() {
        final DatastoreContextIntrospector introspector = INTROSPECTOR_FACTORY.newInstance(OPERATIONAL);

        final Map<String, Object> properties = new HashMap<>();
        properties.put("shard-transaction-idle-timeout-in-minutes", "31");
        properties.put("operation-timeout-in-seconds", "26");
        properties.put("shard-transaction-commit-timeout-in-seconds", "100");
        properties.put("shard-journal-recovery-log-batch-size", "199");
        properties.put("shard-snapshot-batch-count", "212");
        properties.put("shard-heartbeat-interval-in-millis", "101");
        properties.put("shard-transaction-commit-queue-capacity", "567");
        properties.put("shard-initialization-timeout-in-seconds", "82");
        properties.put("shard-leader-election-timeout-in-seconds", "66");
        properties.put("shard-leader-election-timeout-multiplier", "5");
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
        properties.put("initial-payload-serialized-buffer-capacity", "600");

        boolean updated = introspector.update(properties);
        assertTrue("updated", updated);
        DatastoreContext context = introspector.getContext();

        assertEquals(31, context.getShardTransactionIdleTimeout().toMinutes());
        assertEquals(26000, context.getOperationTimeoutInMillis());
        assertEquals(100, context.getShardTransactionCommitTimeoutInSeconds());
        assertEquals(199, context.getShardRaftConfig().getJournalRecoveryLogBatchSize());
        assertEquals(212, context.getShardRaftConfig().getSnapshotBatchCount());
        assertEquals(101, context.getShardRaftConfig().getHeartBeatInterval().length());
        assertEquals(567, context.getShardTransactionCommitQueueCapacity());
        assertEquals(82, context.getShardInitializationTimeout().duration().toSeconds());
        assertEquals(66, context.getShardLeaderElectionTimeout().duration().toSeconds());
        assertEquals(5, context.getShardLeaderElectionTimeoutMultiplier());
        assertEquals(123, context.getShardRaftConfig().getIsolatedCheckIntervalInMillis());
        assertEquals(100, context.getShardRaftConfig().getSnapshotDataThresholdPercentage());
        assertEquals(21, context.getShardRaftConfig().getElectionTimeoutFactor());
        assertEquals(901, context.getShardBatchedModificationCount());
        assertEquals(200, context.getTransactionCreationInitialRateLimit());
        assertEquals(41, context.getDataStoreProperties().getMaxDataChangeExecutorPoolSize());
        assertEquals(1111, context.getDataStoreProperties().getMaxDataChangeExecutorQueueSize());
        assertEquals(2222, context.getDataStoreProperties().getMaxDataChangeListenerQueueSize());
        assertEquals(3333, context.getDataStoreProperties().getMaxDataStoreExecutorQueueSize());
        assertEquals(600, context.getInitialPayloadSerializedBufferCapacity());
        assertFalse(context.isPersistent());

        properties.put("shard-transaction-idle-timeout-in-minutes", "32");
        properties.put("operation-timeout-in-seconds", "27");
        properties.put("shard-heartbeat-interval-in-millis", "102");
        properties.put("shard-election-timeout-factor", "22");
        properties.put("shard-leader-election-timeout-multiplier", "6");
        properties.put("max-shard-data-change-executor-pool-size", "42");
        properties.put("max-shard-data-store-executor-queue-size", "4444");
        properties.put("persistent", "true");

        updated = introspector.update(properties);
        assertTrue("updated", updated);
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
        assertEquals(6, context.getShardLeaderElectionTimeoutMultiplier());
        assertEquals(123, context.getShardRaftConfig().getIsolatedCheckIntervalInMillis());
        assertEquals(100, context.getShardRaftConfig().getSnapshotDataThresholdPercentage());
        assertEquals(22, context.getShardRaftConfig().getElectionTimeoutFactor());
        assertEquals(200, context.getTransactionCreationInitialRateLimit());
        assertEquals(42, context.getDataStoreProperties().getMaxDataChangeExecutorPoolSize());
        assertEquals(1111, context.getDataStoreProperties().getMaxDataChangeExecutorQueueSize());
        assertEquals(2222, context.getDataStoreProperties().getMaxDataChangeListenerQueueSize());
        assertEquals(4444, context.getDataStoreProperties().getMaxDataStoreExecutorQueueSize());
        assertTrue(context.isPersistent());

        updated = introspector.update(null);
        assertFalse("updated", updated);

        updated = introspector.update(new HashMap<>());
        assertFalse("updated", updated);
    }


    @Test
    public void testUpdateWithInvalidValues() {
        final DatastoreContextIntrospector introspector = INTROSPECTOR_FACTORY.newInstance(OPERATIONAL);

        final Map<String, Object> properties = new HashMap<>();
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

        final boolean updated = introspector.update(properties);
        assertTrue("updated", updated);
        DatastoreContext context = introspector.getContext();

        assertEquals(DEFAULT_SHARD_TRANSACTION_IDLE_TIMEOUT, context.getShardTransactionIdleTimeout());
        assertEquals(199, context.getShardRaftConfig().getJournalRecoveryLogBatchSize());
        assertEquals(DEFAULT_SHARD_TX_COMMIT_TIMEOUT_IN_SECONDS, context.getShardTransactionCommitTimeoutInSeconds());
        assertEquals(212, context.getShardRaftConfig().getSnapshotBatchCount());
        assertEquals(DEFAULT_OPERATION_TIMEOUT_IN_MS, context.getOperationTimeoutInMillis());
        assertEquals(DEFAULT_HEARTBEAT_INTERVAL_IN_MILLIS,
                context.getShardRaftConfig().getHeartBeatInterval().length());
        assertEquals(567, context.getShardTransactionCommitQueueCapacity());
        assertEquals(DEFAULT_SHARD_SNAPSHOT_DATA_THRESHOLD_PERCENTAGE,
                context.getShardRaftConfig().getSnapshotDataThresholdPercentage());
        assertEquals(DEFAULT_SHARD_INITIALIZATION_TIMEOUT, context.getShardInitializationTimeout());
        assertEquals(InMemoryDOMDataStoreConfigProperties.DEFAULT_MAX_DATA_CHANGE_EXECUTOR_POOL_SIZE,
                context.getDataStoreProperties().getMaxDataChangeExecutorPoolSize());
    }

    @Test
    public void testUpdateWithDatastoreTypeSpecificProperties() {
        final Map<String, Object> properties = new HashMap<>();
        properties.put("shard-transaction-idle-timeout-in-minutes", "22"); // global setting
        properties.put("operational.shard-transaction-idle-timeout-in-minutes", "33"); // operational override
        properties.put("config.shard-transaction-idle-timeout-in-minutes", "44"); // config override

        properties.put("max-shard-data-change-executor-pool-size", "222"); // global setting
        properties.put("operational.max-shard-data-change-executor-pool-size", "333"); // operational override
        properties.put("config.max-shard-data-change-executor-pool-size", "444"); // config override

        properties.put("persistent", "false"); // global setting
        properties.put("operational.Persistent", "true"); // operational override

        final DatastoreContextIntrospector operIntrospector = INTROSPECTOR_FACTORY.newInstance(OPERATIONAL);
        boolean updated = operIntrospector.update(properties);
        assertTrue("updated", updated);
        DatastoreContext operContext = operIntrospector.getContext();

        assertEquals(33, operContext.getShardTransactionIdleTimeout().toMinutes());
        assertTrue(operContext.isPersistent());
        assertEquals(333, operContext.getDataStoreProperties().getMaxDataChangeExecutorPoolSize());

        final DatastoreContextIntrospector configIntrospector = INTROSPECTOR_FACTORY.newInstance(CONFIGURATION);
        updated = configIntrospector.update(properties);
        assertTrue("updated", updated);
        DatastoreContext configContext = configIntrospector.getContext();

        assertEquals(44, configContext.getShardTransactionIdleTimeout().toMinutes());
        assertFalse(configContext.isPersistent());
        assertEquals(444, configContext.getDataStoreProperties().getMaxDataChangeExecutorPoolSize());
    }

    @Test
    public void testGetDatastoreContextForShard() {
        final Map<String, Object> properties = new HashMap<>();
        properties.put("shard-transaction-idle-timeout-in-minutes", "22"); // global setting
        properties.put("operational.shard-transaction-idle-timeout-in-minutes", "33"); // operational override
        properties.put("config.shard-transaction-idle-timeout-in-minutes", "44"); // config override
        properties.put("topology.shard-transaction-idle-timeout-in-minutes", "55"); // global shard override

        final DatastoreContextIntrospector operIntrospector = INTROSPECTOR_FACTORY.newInstance(OPERATIONAL);

        DatastoreContext shardContext = operIntrospector.newContextFactory().getShardDatastoreContext("topology");
        assertEquals(10, shardContext.getShardTransactionIdleTimeout().toMinutes());

        operIntrospector.update(properties);
        DatastoreContext operContext = operIntrospector.getContext();
        assertEquals(33, operContext.getShardTransactionIdleTimeout().toMinutes());

        shardContext = operIntrospector.newContextFactory().getShardDatastoreContext("topology");
        assertEquals(55, shardContext.getShardTransactionIdleTimeout().toMinutes());

        final DatastoreContextIntrospector configIntrospector = INTROSPECTOR_FACTORY.newInstance(CONFIGURATION);
        configIntrospector.update(properties);
        DatastoreContext configContext = configIntrospector.getContext();
        assertEquals(44, configContext.getShardTransactionIdleTimeout().toMinutes());

        shardContext = configIntrospector.newContextFactory().getShardDatastoreContext("topology");
        assertEquals(55, shardContext.getShardTransactionIdleTimeout().toMinutes());

        // operational shard override
        properties.put("operational.topology.shard-transaction-idle-timeout-in-minutes", "66");
        // config shard override
        properties.put("config.topology.shard-transaction-idle-timeout-in-minutes", "77");

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
