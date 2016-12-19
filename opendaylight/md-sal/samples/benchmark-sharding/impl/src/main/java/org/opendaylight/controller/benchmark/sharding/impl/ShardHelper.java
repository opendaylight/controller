/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.benchmark.sharding.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeProducer;
import org.opendaylight.mdsal.dom.api.DOMDataTreeProducerException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShardingConflictException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShardingService;
import org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataTreeShard;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simplifies creation of shards and management of shard-related data which
 * an application must keep track of.
 *
 * @author jmedved
 */
public class ShardHelper implements AutoCloseable, SchemaContextListener {
    private static final Logger LOG = LoggerFactory.getLogger(ShardHelper.class);

    private static final int DCL_EXECUTOR_MAX_POOL_SIZE = 20;
    private static final int DCL_EXECUTOR_MAX_QUEUE_SIZE = 1000;
    private static final int DATA_CHANGE_LISTENER_MAX_QUEUE_SIZE = 1000;

    private final DOMDataTreeShardingService dataTreeShardingService;
    private final DOMDataTreeService dataTreeService;
    private final SchemaService schemaService;
    private final ListenerRegistration<SchemaContextListener> schemaServiceRegistration;

    private final List<ListenerRegistration<InMemoryDOMDataTreeShard>> dataTreeShardRegistrations = new ArrayList<>();
    private final Map<DOMDataTreeIdentifier, ShardData> shardDb = new HashMap<>();

    /**
     * Constructor.
     *
     * @param dataTreeShardingService reference to MD-SAL Data Tree Sharding Service
     * @param dataTreeService reference to MD-SAL Data Tree  Service
     * @param schemaService reference to MD-SAL Schema Service
     */
    public ShardHelper(final DOMDataTreeShardingService dataTreeShardingService,
            final DOMDataTreeService dataTreeService,
            final SchemaService schemaService) {
        this.dataTreeShardingService = dataTreeShardingService;
        this.dataTreeService = dataTreeService;
        this.schemaService = schemaService;
        schemaServiceRegistration = schemaService.registerSchemaContextListener(this);

        LOG.info("ShardHelper Created & Initialized");
    }

    /**
     * Helper function that first creates a shard,creates a producer for the
     * shard, and triggers MD-SAL to "wire together" the shard and the producer.
     * default parameters defined in this class.
     *
     * @param dataStoreType CONFIG or OPERATIONAL
     * @param yiId "Root" of the shard's subtree
     * @return opaque handle to shard data that is later used to create transactions on the shard.
     * @throws DOMDataTreeShardingConflictException when wiring of the shard and its producer failed
     */
    public ShardData createAndInitShard(final LogicalDatastoreType dataStoreType, final YangInstanceIdentifier yiId)
            throws DOMDataTreeShardingConflictException {

        final DOMDataTreeIdentifier ddtId = new DOMDataTreeIdentifier(dataStoreType, yiId.toOptimized());

        final ExecutorService configRootShardExecutor =
                SpecialExecutors.newBlockingBoundedFastThreadPool(ShardHelper.DCL_EXECUTOR_MAX_POOL_SIZE,
                                                                  ShardHelper.DCL_EXECUTOR_MAX_QUEUE_SIZE,
                                                                  ddtId.getDatastoreType() + "RootShard-DCL");
        final InMemoryDOMDataTreeShard shard =
                InMemoryDOMDataTreeShard.create(ddtId,
                                                configRootShardExecutor,
                                                ShardHelper.DATA_CHANGE_LISTENER_MAX_QUEUE_SIZE);

        final DOMDataTreeProducer producer = dataTreeService.createProducer(Collections.singletonList(ddtId));
        final ListenerRegistration<InMemoryDOMDataTreeShard> dataTreeShardReg =
                    dataTreeShardingService.registerDataTreeShard(ddtId, shard, producer);
        dataTreeShardRegistrations.add(dataTreeShardReg);

        shard.onGlobalContextUpdated(schemaService.getGlobalContext());

        ShardData shardData = new ShardData(ddtId, shard, producer);
        shardDb.put(ddtId, shardData);
        LOG.debug("Created shard for {}, shard: {}, provider: {}", ddtId, shard, producer);

        return shardData;
    }

    @Override
    public void onGlobalContextUpdated(final SchemaContext schemaContext) {
        shardDb.forEach((key, dbEntry) -> dbEntry.getShard().onGlobalContextUpdated(schemaContext));
    }

    /* (non-Javadoc) Close down the Shard Manager.
     * @see java.lang.AutoCloseable#close()
     */
    @Override
    public void close()  {
        clear();
        schemaServiceRegistration.close();
        LOG.info("ShardHelper Closed");
    }

    /** Clear Shard Manager's internal databases. Should be used before
     *  each test run.
     *
     */
    private void clear()  {
        LOG.info("clearing databases");
        dataTreeShardRegistrations.forEach(dataTreeShardReg -> dataTreeShardReg.close());
        dataTreeShardRegistrations.clear();
        shardDb.forEach((key, value) -> value.close());
        shardDb.clear();
    }

    /** Class that holds all data for a shard that is needed by an application
     *  to operate on the shard.
     * @author jmedved
     *
     */
    public static class ShardData {
        private final DOMDataTreeIdentifier ddtId;
        private final InMemoryDOMDataTreeShard shard;
        private final DOMDataTreeProducer producer;

        private ShardData(final DOMDataTreeIdentifier ddtId, final InMemoryDOMDataTreeShard shard,
                final DOMDataTreeProducer producer) {
            this.shard = shard;
            this.ddtId = ddtId;
            this.producer = producer;
        }

        private InMemoryDOMDataTreeShard getShard() {
            return shard;
        }

        public DOMDataTreeIdentifier getDOMDataTreeIdentifier() {
            return ddtId;
        }

        public DOMDataTreeProducer getProducer() {
            return producer;
        }

        void close() {
            try {
                producer.close();
            } catch (DOMDataTreeProducerException e) {
                LOG.error("Exception closing producer, {}", e);
            }
        }
    }
}
