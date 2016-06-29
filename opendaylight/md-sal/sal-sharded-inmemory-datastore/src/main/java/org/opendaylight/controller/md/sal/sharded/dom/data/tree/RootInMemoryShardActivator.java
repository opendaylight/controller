/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.sharded.dom.data.tree;

import java.util.Collections;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RootInMemoryShardActivator {

    private static final Logger LOG = LoggerFactory.getLogger(RootInMemoryShardActivator.class);

    private ListenerRegistration<InMemoryDOMDataTreeShard> shardRegistration = null;

    public RootInMemoryShardActivator(final DOMDataTreeShardingService shardingService,
                                      final DOMDataTreeService domDataTreeService,
                                      final SchemaService schemaService,
                                      final String datastoreType,
                                      final int dclExecutorMaxPoolSize,
                                      final int dclExecutorMaxQueueSize,
                                      final int maxNotificationQueueSize,
                                      final int maxCommitQueueSize) {

        LOG.info("Starting root shard for {} datastore", datastoreType);
        final DOMDataTreeIdentifier id = new DOMDataTreeIdentifier(LogicalDatastoreType.valueOf(datastoreType), YangInstanceIdentifier.EMPTY);

        final ExecutorService dataChangeListenerExecutor = SpecialExecutors.newBlockingBoundedFastThreadPool(
                dclExecutorMaxPoolSize, dclExecutorMaxQueueSize, datastoreType + "RootShard-DCL");

        final InMemoryDOMDataTreeShard shard =
                InMemoryDOMDataTreeShard.create(
                        id, dataChangeListenerExecutor, maxNotificationQueueSize, maxCommitQueueSize);
        schemaService.registerSchemaContextListener(shard);

        final DOMDataTreeProducer producer = domDataTreeService.createProducer(Collections.singletonList(id));
        try {
            shardRegistration = shardingService.registerDataTreeShard(id, shard, producer);
        } catch (final DOMDataTreeShardingConflictException e) {
            LOG.error("Unable to register root shard at {}", id, e);
        } finally {

            try {
                producer.close();
            } catch (final DOMDataTreeProducerException e) {
                LOG.error("Unable to close shard registration producer", e);
            }
        }
    }

    public void close() {
        if (shardRegistration != null) {
            shardRegistration.close();
            shardRegistration = null;
        }
    }
}
