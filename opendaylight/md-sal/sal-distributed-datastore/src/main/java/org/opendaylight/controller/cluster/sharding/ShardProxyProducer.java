/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.sharding;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientLocalHistory;
import org.opendaylight.controller.cluster.databroker.actors.dds.DataStoreClient;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.spi.shard.DOMDataTreeShardProducer;
import org.opendaylight.mdsal.dom.spi.shard.DOMDataTreeShardWriteTransaction;

/**
 * Proxy producer implementation that creates transactions that forward all calls to {@link DataStoreClient}.
 */
@Deprecated(forRemoval = true)
class ShardProxyProducer implements DOMDataTreeShardProducer {
    private final DOMDataTreeIdentifier shardRoot;
    private final Collection<DOMDataTreeIdentifier> prefixes;
    private final ClientLocalHistory history;
    private DistributedShardModificationFactory modificationFactory;

    ShardProxyProducer(final DOMDataTreeIdentifier shardRoot,
                       final Collection<DOMDataTreeIdentifier> prefixes,
                       final DataStoreClient client,
                       final DistributedShardModificationFactory modificationFactory) {
        this.shardRoot = requireNonNull(shardRoot);
        this.prefixes = ImmutableList.copyOf(prefixes);
        this.modificationFactory = requireNonNull(modificationFactory);
        history = requireNonNull(client).createLocalHistory();
    }

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
        this.modificationFactory = requireNonNull(modificationFactory);
    }

    @Override
    public void close() {
        // FIXME: implement this
    }
}
