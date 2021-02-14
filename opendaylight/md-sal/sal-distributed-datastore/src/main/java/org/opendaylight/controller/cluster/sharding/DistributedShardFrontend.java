/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.sharding;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.opendaylight.controller.cluster.databroker.actors.dds.DataStoreClient;
import org.opendaylight.controller.cluster.datastore.DistributedDataStoreInterface;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShard;
import org.opendaylight.mdsal.dom.spi.shard.ChildShardContext;
import org.opendaylight.mdsal.dom.spi.shard.DOMDataTreeShardProducer;
import org.opendaylight.mdsal.dom.spi.shard.ForeignShardModificationContext;
import org.opendaylight.mdsal.dom.spi.shard.ReadableWriteableDOMDataTreeShard;
import org.opendaylight.mdsal.dom.spi.shard.SubshardProducerSpecification;
import org.opendaylight.mdsal.dom.spi.shard.WriteableDOMDataTreeShard;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Proxy implementation of a shard that creates forwarding producers to the backend shard.
 */
@Deprecated(forRemoval = true)
class DistributedShardFrontend implements ReadableWriteableDOMDataTreeShard {

    private static final Logger LOG = LoggerFactory.getLogger(DistributedShardFrontend.class);

    private final DataStoreClient client;
    private final DOMDataTreeIdentifier shardRoot;
    @GuardedBy("this")
    private final Map<DOMDataTreeIdentifier, ChildShardContext> childShards = new HashMap<>();
    @GuardedBy("this")
    private final List<ShardProxyProducer> producers = new ArrayList<>();

    private final DistributedShardChangePublisher publisher;

    DistributedShardFrontend(final DistributedDataStoreInterface distributedDataStore,
                             final DataStoreClient client,
                             final DOMDataTreeIdentifier shardRoot) {
        this.client = requireNonNull(client);
        this.shardRoot = requireNonNull(shardRoot);

        publisher = new DistributedShardChangePublisher(client, requireNonNull(distributedDataStore), shardRoot,
            childShards);
    }

    @Override
    public synchronized DOMDataTreeShardProducer createProducer(final Collection<DOMDataTreeIdentifier> paths) {
        for (final DOMDataTreeIdentifier prodPrefix : paths) {
            checkArgument(shardRoot.contains(prodPrefix), "Prefix %s is not contained under shard root", prodPrefix,
                paths);
        }

        final ShardProxyProducer ret =
                new ShardProxyProducer(shardRoot, paths, client, createModificationFactory(paths));
        producers.add(ret);
        return ret;
    }

    @Override
    public synchronized void onChildAttached(final DOMDataTreeIdentifier prefix, final DOMDataTreeShard child) {
        LOG.debug("{} : Child shard attached at {}", shardRoot, prefix);
        checkArgument(child != this, "Attempted to attach child %s onto self", this);
        addChildShard(prefix, child);
        updateProducers();
    }

    @Override
    public synchronized void onChildDetached(final DOMDataTreeIdentifier prefix, final DOMDataTreeShard child) {
        LOG.debug("{} : Child shard detached at {}", shardRoot, prefix);
        childShards.remove(prefix);
        updateProducers();
        // TODO we should grab the dataTreeSnapshot that's in the shard and apply it to this shard
    }

    private void addChildShard(final DOMDataTreeIdentifier prefix, final DOMDataTreeShard child) {
        checkArgument(child instanceof WriteableDOMDataTreeShard);
        childShards.put(prefix, new ChildShardContext(prefix, (WriteableDOMDataTreeShard) child));
    }

    DistributedShardModificationFactory createModificationFactory(final Collection<DOMDataTreeIdentifier> prefixes) {
        // TODO this could be abstract
        final Map<DOMDataTreeIdentifier, SubshardProducerSpecification> affectedSubshards = new HashMap<>();

        for (final DOMDataTreeIdentifier producerPrefix : prefixes) {
            for (final ChildShardContext maybeAffected : childShards.values()) {
                final DOMDataTreeIdentifier bindPath;
                if (producerPrefix.contains(maybeAffected.getPrefix())) {
                    bindPath = maybeAffected.getPrefix();
                } else if (maybeAffected.getPrefix().contains(producerPrefix)) {
                    // Bound path is inside subshard
                    bindPath = producerPrefix;
                } else {
                    continue;
                }

                SubshardProducerSpecification spec = affectedSubshards.computeIfAbsent(maybeAffected.getPrefix(),
                    k -> new SubshardProducerSpecification(maybeAffected));
                spec.addPrefix(bindPath);
            }
        }

        final DistributedShardModificationFactoryBuilder builder =
                new DistributedShardModificationFactoryBuilder(shardRoot);
        for (final SubshardProducerSpecification spec : affectedSubshards.values()) {
            final ForeignShardModificationContext foreignContext =
                    new ForeignShardModificationContext(spec.getPrefix(), spec.createProducer());
            builder.addSubshard(foreignContext);
            builder.addSubshard(spec.getPrefix(), foreignContext);
        }

        return builder.build();
    }

    private void updateProducers() {
        for (final ShardProxyProducer producer : producers) {
            producer.setModificationFactory(createModificationFactory(producer.getPrefixes()));
        }
    }

    @Override
    public <L extends DOMDataTreeChangeListener> ListenerRegistration<L> registerTreeChangeListener(
            final YangInstanceIdentifier treeId, final L listener) {
        return publisher.registerTreeChangeListener(treeId, listener);
    }
}
