/*
 * Copyright © 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.benchmark.sharding.impl;

import java.util.Collections;
import java.util.concurrent.ExecutorService;
import org.opendaylight.controller.sal.core.api.model.SchemaService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeProducer;
import org.opendaylight.mdsal.dom.api.DOMDataTreeProducerException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShardingConflictException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShardingService;
import org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataTreeShard;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InMemoryShardFactory implements ShardFactory {
    private static Logger LOG = LoggerFactory.getLogger(InMemoryShardFactory.class);

    private static final int DCL_EXECUTOR_MAX_POOL_SIZE = 20;
    private static final int DCL_EXECUTOR_MAX_QUEUE_SIZE = 1000;
    private static final int DATA_CHANGE_LISTENER_MAX_QUEUE_SIZE = 1000;

    private final DOMDataTreeShardingService dataTreeShardingService;
    private final DOMDataTreeService dataTreeService;
    private final SchemaService schemaService;

    public InMemoryShardFactory(final DOMDataTreeShardingService dataTreeShardingService,
                                final DOMDataTreeService dataTreeService, final SchemaService schemaService) {
        this.dataTreeShardingService = dataTreeShardingService;
        this.dataTreeService = dataTreeService;
        this.schemaService = schemaService;
    }


    @Override
    public ShardRegistration createShard(final DOMDataTreeIdentifier prefix)
    throws DOMDataTreeShardingConflictException {
        LOG.debug("Creating InMemoryShard for prefix {}", prefix);

        // TODO shouldn't this be shared among all shards?
        final ExecutorService configRootShardExecutor =
                SpecialExecutors.newBlockingBoundedFastThreadPool(DCL_EXECUTOR_MAX_POOL_SIZE,
                        DCL_EXECUTOR_MAX_QUEUE_SIZE,
                        prefix.getDatastoreType() + "RootShard-DCL");

        final InMemoryDOMDataTreeShard shard =
                InMemoryDOMDataTreeShard.create(prefix,
                        configRootShardExecutor,
                        DATA_CHANGE_LISTENER_MAX_QUEUE_SIZE);

        final DOMDataTreeProducer producer = dataTreeService.createProducer(Collections.singletonList(prefix));
        final ListenerRegistration<InMemoryDOMDataTreeShard> dataTreeShardReg =
                dataTreeShardingService.registerDataTreeShard(prefix, shard, producer);
        shard.onGlobalContextUpdated(schemaService.getGlobalContext());

        try {
            producer.close();
        } catch (DOMDataTreeProducerException e) {
            // this shouldn't happen
            LOG.error("Closing producer after {} shard registration failed.", prefix);
            // producer can be still open and bound to given prefix, thus
            // blocking creation of another producers
            throw new RuntimeException(e);
        }

        return dataTreeShardReg::close;
    }
}
