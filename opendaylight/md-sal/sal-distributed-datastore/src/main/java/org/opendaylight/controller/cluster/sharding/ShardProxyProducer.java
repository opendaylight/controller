/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.sharding;

import com.google.common.util.concurrent.ListeningExecutorService;
import java.util.Collection;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.databroker.actors.dds.DataStoreClient;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.spi.shard.DOMDataTreeShardProducer;
import org.opendaylight.mdsal.dom.spi.shard.DOMDataTreeShardWriteTransaction;

/**
 * Proxy producer implementation that creates transactions that forward all calls to {@link DistributedDataStoreClient}.
 */
class ShardProxyProducer implements DOMDataTreeShardProducer {

    private final DOMDataTreeIdentifier shardRoot;
    private final Collection<DOMDataTreeIdentifier> prefixes;
    private final DataStoreClient client;
    private final ListeningExecutorService executorService;

    ShardProxyProducer(final DOMDataTreeIdentifier shardRoot, final Collection<DOMDataTreeIdentifier> prefixes,
                       final DataStoreClient client, final ListeningExecutorService executorService) {
        this.shardRoot = shardRoot;
        this.prefixes = prefixes;
        this.client = client;
        this.executorService = executorService;
    }

    @Nonnull
    @Override
    public Collection<DOMDataTreeIdentifier> getPrefixes() {
        return prefixes;
    }

    @Override
    public DOMDataTreeShardWriteTransaction createTransaction() {
        return new ShardProxyTransaction(shardRoot, prefixes, client, executorService);
    }
}

