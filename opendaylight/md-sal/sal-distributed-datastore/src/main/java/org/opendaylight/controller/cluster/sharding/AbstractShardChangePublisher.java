/*
 * Copyright (c) 2017 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.sharding;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.Map;
import org.opendaylight.controller.cluster.datastore.AbstractDataStore;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.spi.shard.ChildShardContext;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreTreeChangePublisher;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;

abstract class AbstractShardChangePublisher implements DOMStoreTreeChangePublisher {
    private final AbstractDataStore frontend;
    private final DOMDataTreeIdentifier prefix;

    AbstractShardChangePublisher(final AbstractDataStore frontend, final DOMDataTreeIdentifier prefix) {
        this.frontend = checkNotNull(frontend);
        this.prefix = checkNotNull(prefix);
    }

    final YangInstanceIdentifier stripPrefix(final YangInstanceIdentifier path) {
        return stripShardPath(prefix.getRootIdentifier(), path);
    }

    final ListenerRegistration<DOMDataTreeChangeListener> registerListener(final YangInstanceIdentifier path,
            final org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener listener) {
        // we need to register the listener registration path based on the shards root
        // we have to strip the shard path from the listener path and then register
        return frontend.registerProxyListener(path, stripPrefix(path), listener);
    }

    static final YangInstanceIdentifier stripShardPath(final YangInstanceIdentifier shardPath,
            final YangInstanceIdentifier listenerPath) {
        final Optional<YangInstanceIdentifier> relativeOpt = listenerPath.relativeTo(shardPath);
        Preconditions.checkArgument(relativeOpt.isPresent(), "Shard path %s is not parent of listener path %s",
            shardPath, listenerPath);
        return relativeOpt.get();
    }

    abstract AbstractShardChangePublisher addChild(ChildShardContext child,
            Map<DOMDataTreeIdentifier, ChildShardContext> childShards);

    abstract AbstractShardChangePublisher removeChild(ChildShardContext child,
            Map<DOMDataTreeIdentifier, ChildShardContext> childShards);
}
