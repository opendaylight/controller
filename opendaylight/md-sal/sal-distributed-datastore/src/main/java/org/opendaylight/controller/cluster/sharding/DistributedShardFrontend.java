/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.sharding;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.cluster.databroker.actors.dds.DataStoreClient;
import org.opendaylight.controller.cluster.datastore.AbstractDataStore;
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
class DistributedShardFrontend implements ReadableWriteableDOMDataTreeShard {
    private static final Logger LOG = LoggerFactory.getLogger(DistributedShardFrontend.class);

    private final DOMDataTreeIdentifier shardRoot;
    private final AbstractDataStore frontend;
    private final DataStoreClient client;

    @GuardedBy("this")
    private final List<ShardProxyProducer> producers = new ArrayList<>();

    @GuardedBy("this")
    private Map<DOMDataTreeIdentifier, ChildShardContext> childShards = ImmutableMap.of();
    @GuardedBy("this")
    private AbstractShardChangePublisher publisher;

    DistributedShardFrontend(final AbstractDataStore frontend, final DataStoreClient client,
            final DOMDataTreeIdentifier shardRoot) {
        this.frontend = Preconditions.checkNotNull(frontend);
        this.client = Preconditions.checkNotNull(client);
        this.shardRoot = Preconditions.checkNotNull(shardRoot);

        this.publisher = new LeafShardChangePublisher(frontend, shardRoot);
    }

    @Override
    public synchronized DOMDataTreeShardProducer createProducer(final Collection<DOMDataTreeIdentifier> paths) {
        for (final DOMDataTreeIdentifier prodPrefix : paths) {
            Preconditions.checkArgument(shardRoot.contains(prodPrefix), "Prefix %s is not contained under shard root",
                    prodPrefix, paths);
        }

        final ShardProxyProducer ret =
                new ShardProxyProducer(shardRoot, paths, client, createModificationFactory(paths));
        producers.add(ret);
        return ret;
    }

    @Override
    public synchronized void onChildAttached(final DOMDataTreeIdentifier prefix, final DOMDataTreeShard child) {
        LOG.debug("{} : Child shard attached at {}", shardRoot, prefix);
        Preconditions.checkArgument(child != this, "Attempted to attach child %s onto self", this);
        Preconditions.checkArgument(child instanceof WriteableDOMDataTreeShard);

        final ChildShardContext ctx = new ChildShardContext(prefix, (WriteableDOMDataTreeShard) child);

        final Builder<DOMDataTreeIdentifier, ChildShardContext> builder = ImmutableMap.builder();
        builder.putAll(childShards);
        builder.put(prefix, ctx);
        childShards = builder.build();

        publisher = publisher.addChild(ctx, childShards);
        updateProducers();
    }

    @Override
    public synchronized void onChildDetached(final DOMDataTreeIdentifier prefix, final DOMDataTreeShard child) {
        LOG.debug("{} : Child shard detached at {}", shardRoot, prefix);

        final ChildShardContext ctx = childShards.get(prefix);
        if (ctx == null) {
            // No-op
            return;
        }

        final Builder<DOMDataTreeIdentifier, ChildShardContext> builder = ImmutableMap.builder();
        for (Entry<DOMDataTreeIdentifier, ChildShardContext> entry : childShards.entrySet()) {
            if (!prefix.equals(entry.getKey())) {
                builder.put(entry);
            }
        }
        childShards = builder.build();

        publisher = publisher.removeChild(ctx, childShards);
        updateProducers();
        // TODO we should grab the dataTreeSnapshot that's in the shard and apply it to this shard
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

                SubshardProducerSpecification spec = affectedSubshards.get(maybeAffected.getPrefix());
                if (spec == null) {
                    spec = new SubshardProducerSpecification(maybeAffected);
                    affectedSubshards.put(maybeAffected.getPrefix(), spec);
                }
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

    @Nonnull
    @Override
    public synchronized <L extends DOMDataTreeChangeListener> ListenerRegistration<L> registerTreeChangeListener(
            final YangInstanceIdentifier treeId, final L listener) {
        return publisher.registerTreeChangeListener(treeId, listener);
    }
}
