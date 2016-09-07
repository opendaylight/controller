/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.sharding;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientLocalHistory;
import org.opendaylight.controller.cluster.databroker.actors.dds.DataStoreClient;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.spi.shard.DOMDataTreeShardProducer;
import org.opendaylight.mdsal.dom.spi.shard.DOMDataTreeShardWriteTransaction;
import org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataTreeShard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Proxy producer implementation that creates transactions that forward all calls to {@link DataStoreClient}.
 */
class ShardProxyProducer implements DOMDataTreeShardProducer {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryDOMDataTreeShard.class);
    private static final AtomicLong COUNTER = new AtomicLong();

    private final DOMDataTreeIdentifier shardRoot;
    private final Collection<DOMDataTreeIdentifier> prefixes;
    private final ClientLocalHistory history;
    private DistributedShardModificationFactory modificationFactory;

    ShardProxyProducer(final DOMDataTreeIdentifier shardRoot,
                       final Collection<DOMDataTreeIdentifier> prefixes,
                       final DataStoreClient client,
                       final DistributedShardModificationFactory modificationFactory) {
        this.shardRoot = Preconditions.checkNotNull(shardRoot);
        this.prefixes = ImmutableList.copyOf(Preconditions.checkNotNull(prefixes));
        this.modificationFactory = Preconditions.checkNotNull(modificationFactory);
        history = Preconditions.checkNotNull(client).createLocalHistory();
    }

    @Nonnull
    @Override
    public Collection<DOMDataTreeIdentifier> getPrefixes() {
        return prefixes;
    }

    @Override
    public DOMDataTreeShardWriteTransaction createTransaction() {
        return new ShardProxyTransaction(shardRoot, prefixes,
                modificationFactory.createModification(history.createTransaction()));
    }

    DistributedShardModificationFactory getModificationFactory() {
        return modificationFactory;
    }

    void setModificationFactory(final DistributedShardModificationFactory modificationFactory) {
        this.modificationFactory = Preconditions.checkNotNull(modificationFactory);
    }
}

