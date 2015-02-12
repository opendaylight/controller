/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeProducer;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeShard;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeShardingConflictException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeShardingService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;

public final class ShardedDOMDataTree implements DOMDataTreeService, DOMDataTreeShardingService {
    private final Map<LogicalDatastoreType, ShardingTable> shardingTables = new EnumMap<>(LogicalDatastoreType.class);

    private final ShardRegistration<?> lookupShard(final DOMDataTreeIdentifier prefix) {
        final ShardingTable t = shardingTables.get(prefix.getDatastoreType());
        if (t == null) {
            return null;
        }

        return t.lookup(prefix.getRootIdentifier());
    }

    synchronized void removeShard(final ShardRegistration<?> reg) {
        // FIXME: implement this
    }

    @Override
    public synchronized <T extends DOMDataTreeShard> ListenerRegistration<T> registerDataTreeShard(final DOMDataTreeIdentifier prefix, final T shard) throws DOMDataTreeShardingConflictException {
        /*
         * Lookup the parent shard (e.g. the one which currently matches the prefix),
         * and if it exists, check if its registration prefix does not collide with
         * this registration.
         */
        final ShardRegistration<?> parentReg = lookupShard(prefix);
        if (parentReg != null && prefix.equals(parentReg.getPrefix())) {
            throw new DOMDataTreeShardingConflictException(String.format("Prefix %s is already occupied by shard {}", prefix, parentReg.getInstance()));
        }

        // FIXME: wrap the shard in a proper adaptor based on implemented interface

        return new ShardRegistration<T>(this, prefix, shard);
    }

    @Override
    public DOMDataTreeProducer createProducer(final Collection<DOMDataTreeIdentifier> subtrees, final boolean allowTxMerges) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T extends DOMDataTreeListener> ListenerRegistration<T> registerListener(final T listener, final Collection<DOMDataTreeIdentifier> subtrees, final boolean allowRxMerges, final Collection<DOMDataTreeProducer> producers) {
        // TODO Auto-generated method stub
        return null;
    }

}
