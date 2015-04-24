/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSelection;
import akka.dispatch.OnComplete;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryShardInfo;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionFactory;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import scala.concurrent.Future;

/**
 * Factory for creating components to a transaction. Maintains a cache of known
 * local shards.
 */
abstract class ComponentFactory {
    // FIXME: this needs to be invalidated
    private final ConcurrentMap<String, DOMStoreTransactionFactory> knownLocal = new ConcurrentHashMap<>();

    private final ActorContext actorContext;

    protected ComponentFactory(final ActorContext actorContext) {
        this.actorContext = Preconditions.checkNotNull(actorContext);
    }

    final ActorContext getActorContext() {
        return actorContext;
    }

    final AbstractTransactionComponent newTransactionComponent(final TransactionProxy parent, final String shardName) {
        final DOMStoreTransactionFactory local = knownLocal.get(shardName);
        if (local != null) {
            return createLocalComponent(local, parent.getType());
        }

        Future<PrimaryShardInfo> findPrimaryFuture = actorContext.findPrimaryShardAsync(shardName);
        final TransactionFutureCallback callback = new TransactionFutureCallback(parent.getRemoteContext(), shardName);
        findPrimaryFuture.onComplete(new OnComplete<PrimaryShardInfo>() {
            @Override
            public void onComplete(final Throwable failure, final PrimaryShardInfo primaryShardInfo) {
                if(failure != null) {
                    callback.createTransactionContext(failure, null);
                } else {
                    callback.setPrimaryShard(primaryShardInfo.getPrimaryShardActor());
                    updateShardInfo(shardName, primaryShardInfo);
                }
            }
        }, actorContext.getClientDispatcher());

        return null;
    }

    void updateShardInfo(final String shardName, final PrimaryShardInfo primaryShardInfo) {
        final Optional<DataTree> maybeDataTree = primaryShardInfo.getLocalShardDataTree();
        if (maybeDataTree.isPresent()) {
            knownLocal.putIfAbsent(shardName, factoryForShard(shardName, primaryShardInfo.getPrimaryShardActor(), maybeDataTree.get()));
        }
    }

    /**
     * Find the primary shard actor.
     *
     * @param shardName Shard name
     * @return Future containing shard information.
     */
    protected abstract Future<PrimaryShardInfo> findPrimaryShard(String shardName);

    /**
     * Create local transaction factory for specified shard, backed by specified shard leader
     * and data tree instance.
     *
     * @param shardName
     * @param shardLeader
     * @param dataTree Backing data tree instance. The data tree may only be accessed in
     *                 read-only manner.
     * @return Transaction factory for local use.
     */
    protected abstract DOMStoreTransactionFactory factoryForShard(String shardName, ActorSelection shardLeader, DataTree dataTree);

    private static AbstractTransactionComponent createLocalComponent(final DOMStoreTransactionFactory factory, final TransactionType type) {
        switch (type) {
        case READ_ONLY:
            return new LocalReadTransactionComponent(factory.newReadOnlyTransaction());
        case READ_WRITE:
            return new LocalReadWriteTransactionComponent(factory.newReadWriteTransaction());
        case WRITE_ONLY:
            return new LocalWriteTransactionComponent(factory.newWriteOnlyTransaction());
        default:
          throw new IllegalArgumentException("Unhandled transaction type " + type);
        }
    }
}
