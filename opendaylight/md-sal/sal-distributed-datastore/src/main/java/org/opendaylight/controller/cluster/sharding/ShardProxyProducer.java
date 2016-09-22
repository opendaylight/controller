/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.sharding;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListeningExecutorService;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientLocalHistory;
import org.opendaylight.controller.cluster.databroker.actors.dds.DistributedDataStoreClient;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.store.inmemory.DOMDataTreeShardProducer;
import org.opendaylight.mdsal.dom.store.inmemory.DOMDataTreeShardWriteTransaction;
import org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataTreeShard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Proxy producer implementation that creates transactions that forward all calls to {@link DistributedDataStoreClient}.
 */
class ShardProxyProducer implements DOMDataTreeShardProducer {


    private static final Logger LOG = LoggerFactory.getLogger(InMemoryDOMDataTreeShard.class);
    private static final AtomicLong COUNTER = new AtomicLong();
//
//    private final Idle idleState = new Idle(this);
//
//    private static final AtomicReferenceFieldUpdater<ShardProxyProducer, State> STATE_UPDATER =
//            AtomicReferenceFieldUpdater.newUpdater(ShardProxyProducer.class, State.class, "state");
//    private volatile State state;

    private final DOMDataTreeIdentifier shardRoot;
    private final Collection<DOMDataTreeIdentifier> prefixes;
    private final DistributedDataStoreClient client;
    private final ClientLocalHistory history;
    private final ListeningExecutorService executorService;
    private DistributedShardModificationFactory modificationFactory;

    ShardProxyProducer(final DOMDataTreeIdentifier shardRoot, final Collection<DOMDataTreeIdentifier> prefixes,
                       final DistributedDataStoreClient client, final ListeningExecutorService executorService,
                       final DistributedShardModificationFactory modificationFactory) {
        this.shardRoot = Preconditions.checkNotNull(shardRoot);
        this.prefixes = Preconditions.checkNotNull(prefixes);
        this.client = Preconditions.checkNotNull(client);
        this.executorService = Preconditions.checkNotNull(executorService);
        this.modificationFactory = Preconditions.checkNotNull(modificationFactory);
        history = client.createLocalHistory();
    }

    @Nonnull
    @Override
    public Collection<DOMDataTreeIdentifier> getPrefixes() {
        return prefixes;
    }

    @Override
    public DOMDataTreeShardWriteTransaction createTransaction() {
        return new ShardProxyTransaction(shardRoot, prefixes, modificationFactory.createModification(history.createTransaction()), executorService);
    }

    DistributedShardModificationFactory getModificationFactory() {
        return modificationFactory;
    }

    void setModificationFactory(final DistributedShardModificationFactory modificationFactory) {
        this.modificationFactory = Preconditions.checkNotNull(modificationFactory);
    }
}

