/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.sharding;

import com.google.common.base.Preconditions;
import java.util.Collection;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.databroker.actors.dds.DataStoreClient;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShard;
import org.opendaylight.mdsal.dom.spi.shard.DOMDataTreeShardProducer;
import org.opendaylight.mdsal.dom.spi.shard.ReadableWriteableDOMDataTreeShard;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

/**
 * Proxy implementation of a shard that creates forwarding producers to the backend shard.
 */
class ShardFrontend implements ReadableWriteableDOMDataTreeShard {

    private final DataStoreClient client;
    private final DOMDataTreeIdentifier shardRoot;

    ShardFrontend(final DataStoreClient client,
                  final DOMDataTreeIdentifier shardRoot) {
        this.client = Preconditions.checkNotNull(client);
        this.shardRoot = Preconditions.checkNotNull(shardRoot);
    }

    @Override
    public DOMDataTreeShardProducer createProducer(final Collection<DOMDataTreeIdentifier> paths) {
        return new ShardProxyProducer(shardRoot, paths, client);
    }

    @Override
    public void onChildAttached(final DOMDataTreeIdentifier prefix, final DOMDataTreeShard child) {
        // TODO message directly into the shard
    }

    @Override
    public void onChildDetached(final DOMDataTreeIdentifier prefix, final DOMDataTreeShard child) {
        // TODO message directly into the shard
    }

    @Nonnull
    @Override
    public <L extends DOMDataTreeChangeListener> ListenerRegistration<L> registerTreeChangeListener(
            final YangInstanceIdentifier treeId, final L listener) {
        throw new UnsupportedOperationException("Registering data tree change listener is not supported");
    }
}
