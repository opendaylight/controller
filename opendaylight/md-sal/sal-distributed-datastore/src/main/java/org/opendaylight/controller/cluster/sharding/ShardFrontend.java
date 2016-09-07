/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.sharding;

import akka.actor.ActorRef;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Collection;
import java.util.concurrent.Executors;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.databroker.actors.dds.DistributedDataStoreClient;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.cluster.datastore.utils.ClusterUtils;
import org.opendaylight.controller.cluster.sharding.messages.ChildShardAttached;
import org.opendaylight.controller.cluster.sharding.messages.ChildShardDetached;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeShard;
import org.opendaylight.mdsal.dom.store.inmemory.DOMDataTreeShardProducer;
import org.opendaylight.mdsal.dom.store.inmemory.ReadableWriteableDOMDataTreeShard;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.compat.java8.FutureConverters;
import scala.concurrent.Future;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Proxy implementation of a shard that creates forwarding producers to the backend shard
 */
class ShardFrontend implements ReadableWriteableDOMDataTreeShard {

    private static final Logger LOG = LoggerFactory.getLogger(ShardFrontend.class);

    private final DistributedDataStoreClient client;
    private final DOMDataTreeIdentifier shardRoot;
    private final ActorContext actorContext;
    private final ListeningExecutorService executorService = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());

    ShardFrontend(final DistributedDataStoreClient client,
                  final DOMDataTreeIdentifier shardRoot,
                  final ActorContext actorContext) {
        this.client = client;
        this.shardRoot = shardRoot;
        this.actorContext = actorContext;
    }

    @Override
    public DOMDataTreeShardProducer createProducer(final Collection<DOMDataTreeIdentifier> paths) {
        return new ShardProxyProducer(shardRoot, paths, client, executorService);
    }

    @Override
    public void onChildAttached(final DOMDataTreeIdentifier prefix, final DOMDataTreeShard child) {
        final String shardName = ClusterUtils.getCleanShardName(shardRoot.getRootIdentifier());
        final Future<ActorRef> scalaFuture = actorContext.findLocalShardAsync(shardName);

        // should this ever fail with anything else other than local shard not found?
        FutureConverters.toJava(scalaFuture).whenComplete((ref, throwable) -> {
            if (throwable != null) {
                LOG.error("ShardFrontend{} onChildAttached failed. {}", shardRoot, throwable);
            } else {
                LOG.debug("ShardFrontend{} sending child{} attached to backend shard", shardRoot, prefix);
                ref.tell(new ChildShardAttached(prefix, child), ActorRef.noSender());
            }
        });
    }

    @Override
    public void onChildDetached(final DOMDataTreeIdentifier prefix, final DOMDataTreeShard child) {
        final String shardName = ClusterUtils.getCleanShardName(shardRoot.getRootIdentifier());
        final Future<ActorRef> scalaFuture = actorContext.findLocalShardAsync(shardName);

        // should this ever fail with anything else other than local shard not found?
        FutureConverters.toJava(scalaFuture).whenComplete((ref, throwable) -> {
            if (throwable != null) {
                LOG.error("ShardFrontend{} onChildDetached failed. {}", shardRoot, throwable);
            } else {
                LOG.debug("ShardFrontend{} sending child{} detached on backend shard", shardRoot, prefix);
                ref.tell(new ChildShardDetached(prefix, child), ActorRef.noSender());
            }
        });
    }

    @Nonnull
    @Override
    public <L extends DOMDataTreeChangeListener> ListenerRegistration<L> registerTreeChangeListener(final YangInstanceIdentifier treeId, final L listener) {
        throw new NotImplementedException();
    }
}
