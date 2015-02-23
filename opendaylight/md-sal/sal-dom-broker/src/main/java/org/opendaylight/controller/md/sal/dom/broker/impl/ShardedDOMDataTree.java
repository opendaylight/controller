/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeProducer;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeShard;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeShardingConflictException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeShardingService;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ShardedDOMDataTree implements DOMDataTreeService, DOMDataTreeShardingService {
    private static final Logger LOG = LoggerFactory.getLogger(ShardedDOMDataTree.class);
    private final Map<LogicalDatastoreType, ShardingTableEntry> shardingTables = new EnumMap<>(LogicalDatastoreType.class);
    @GuardedBy("this")
    private final Map<DOMDataTreeIdentifier, DOMDataTreeProducer> idToProducer = new TreeMap<>();

    // FIXME: figure out how to do this
    @GuardedBy("this")
    private final DirectedGraph<Object, Object> connectivity = new DirectedSparseMultigraph<>();

    @GuardedBy("this")
    private ShardingTableEntry lookupShard(final DOMDataTreeIdentifier prefix) {
        final ShardingTableEntry t = shardingTables.get(prefix.getDatastoreType());
        if (t == null) {
            return null;
        }

        return t.lookup(prefix.getRootIdentifier());
    }

    @GuardedBy("this")
    private void storeShard(final DOMDataTreeIdentifier prefix, final ShardRegistration<?> reg) {
        ShardingTableEntry t = shardingTables.get(prefix.getDatastoreType());
        if (t == null) {
            t = new ShardingTableEntry();
            shardingTables.put(prefix.getDatastoreType(), t);
        }

        t.store(prefix.getRootIdentifier(), reg);
    }

    void removeShard(final ShardRegistration<?> reg) {
        final DOMDataTreeIdentifier prefix = reg.getPrefix();
        final ShardRegistration<?> parentReg;

        synchronized (this) {
            final ShardingTableEntry t = shardingTables.get(prefix.getDatastoreType());
            if (t == null) {
                LOG.warn("Shard registration {} points to non-existent table", reg);
                return;
            }

            t.remove(prefix.getRootIdentifier());
            parentReg = lookupShard(prefix).getRegistration();

            /*
             * FIXME: adjust all producers. This is tricky, as we need different locking strategy,
             *        simply because we risk AB/BA deadlock with a producer being split off from
             *        a producer.
             *
             */
        }

        if (parentReg != null) {
            parentReg.getInstance().onChildDetached(prefix, reg.getInstance());
        }
    }

    @Override
    public <T extends DOMDataTreeShard> ListenerRegistration<T> registerDataTreeShard(final DOMDataTreeIdentifier prefix, final T shard) throws DOMDataTreeShardingConflictException {
        final ShardRegistration<T> reg;
        final ShardRegistration<?> parentReg;

        synchronized (this) {
            /*
             * Lookup the parent shard (e.g. the one which currently matches the prefix),
             * and if it exists, check if its registration prefix does not collide with
             * this registration.
             */
            final ShardingTableEntry parent = lookupShard(prefix);
            parentReg = parent.getRegistration();
            if (parentReg != null && prefix.equals(parentReg.getPrefix())) {
                throw new DOMDataTreeShardingConflictException(String.format("Prefix %s is already occupied by shard {}", prefix, parentReg.getInstance()));
            }

            // FIXME: wrap the shard in a proper adaptor based on implemented interface

            reg = new ShardRegistration<T>(this, prefix, shard);

            storeShard(prefix, reg);

            // FIXME: update any producers/registrations
        }

        // Notify the parent shard
        if (parentReg != null) {
            parentReg.getInstance().onChildAttached(prefix, shard);
        }

        return reg;
    }

    @GuardedBy("this")
    private DOMDataTreeProducer findProducer(final DOMDataTreeIdentifier subtree) {
        for (Entry<DOMDataTreeIdentifier, DOMDataTreeProducer> e : idToProducer.entrySet()) {
            if (e.getKey().contains(subtree)) {
                return e.getValue();
            }
        }

        return null;
    }

    synchronized void destroyProducer(final ShardedDOMDataTreeProducer producer) {
        for (DOMDataTreeIdentifier s : producer.getSubtrees()) {
            DOMDataTreeProducer r = idToProducer.remove(s);
            if (!producer.equals(r)) {
                LOG.error("Removed producer %s on subtree %s while removing %s", r, s, producer);
            }
        }
    }

    @GuardedBy("this")
    private DOMDataTreeProducer createProducer(final Map<DOMDataTreeIdentifier, DOMDataTreeShard> shardMap) {
        // Record the producer's attachment points
        final DOMDataTreeProducer ret = ShardedDOMDataTreeProducer.create(this, shardMap);
        for (DOMDataTreeIdentifier s : shardMap.keySet()) {
            final DOMDataTreeProducer prev = idToProducer.put(s, ret);
            if (prev != null) {
                LOG.warn("Replaced producer %s on subtree %s", prev, s);
            }
        }

        return ret;
    }

    @Override
    public synchronized DOMDataTreeProducer createProducer(final Collection<DOMDataTreeIdentifier> subtrees) {
        Preconditions.checkArgument(!subtrees.isEmpty(), "Subtrees may not be empty");

        final Map<DOMDataTreeIdentifier, DOMDataTreeShard> shardMap = new HashMap<>();
        for (DOMDataTreeIdentifier s : subtrees) {
            // Attempting to create a disconnected producer -- all subtrees have to be unclaimed
            final DOMDataTreeProducer producer = findProducer(s);
            Preconditions.checkArgument(producer == null, "Subtree %s is attached to producer %s", s, producer);

            shardMap.put(s, lookupShard(s).getRegistration().getInstance());
        }

        return createProducer(shardMap);
    }

    synchronized DOMDataTreeProducer createProducer(final ShardedDOMDataTreeProducer parent, final Collection<DOMDataTreeIdentifier> subtrees) {
        Preconditions.checkNotNull(parent);

        final Map<DOMDataTreeIdentifier, DOMDataTreeShard> shardMap = new HashMap<>();
        for (DOMDataTreeIdentifier s : subtrees) {
            shardMap.put(s, lookupShard(s).getRegistration().getInstance());
        }

        return createProducer(shardMap);
    }

    @Override
    public <T extends DOMDataTreeListener> ListenerRegistration<T> registerListener(final T listener, final Collection<DOMDataTreeIdentifier> subtrees,
        final boolean allowRxMerges, final Collection<DOMDataTreeProducer> producers) {

        // Check arguments first and build the producer set
        Preconditions.checkArgument(!subtrees.isEmpty());

        final Builder<ShardedDOMDataTreeProducer> pb = ImmutableSet.builder();
        for (DOMDataTreeProducer p : producers) {
            Preconditions.checkArgument(p instanceof ShardedDOMDataTreeProducer, "Unsupported producer type %s", p.getClass());

            final ShardedDOMDataTreeProducer sp = (ShardedDOMDataTreeProducer)p;
            final DOMDataTreeListener l = sp.getListener();
            Preconditions.checkArgument(l == null, "Producer %s is bound to listener %s", p, l);
            pb.add(sp);
        }

        final ShardedListenerRegistration<T> ret = new ShardedListenerRegistration<T>(listener);
        final Collection<ListenerRegistration<?>> regs = new ArrayList<>();

        synchronized (this) {
            final Map<DOMDataTreeIdentifier, DOMDataTreeChangeService> shardMap = new HashMap<>();
            for (DOMDataTreeIdentifier s : subtrees) {
                final DOMDataTreeShard shard = lookupShard(s).getRegistration().getInstance();

                Preconditions.checkArgument(shard instanceof DOMDataTreeChangeService, "Subtree %s is non-listenable shard type %s", s, shard.getClass());
                shardMap.put(s, (DOMDataTreeChangeService)shard);
            }

            // FIXME: play the 'what-if' game to prove the system remains loop-free


            for (Entry<DOMDataTreeIdentifier, DOMDataTreeChangeService> e : shardMap.entrySet()) {
                regs.add(e.getValue().registerDataTreeChangeListener(e.getKey(), ret));
            }

            // FIXME: register the listener
        }

        ret.startForwarding(regs);




        // TODO Auto-generated method stub
        return ret;
    }
}
