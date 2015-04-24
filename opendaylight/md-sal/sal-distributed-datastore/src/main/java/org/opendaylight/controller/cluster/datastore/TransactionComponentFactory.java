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
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryShardInfo;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionFactory;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import scala.concurrent.Future;

/**
 * Factory for creating components to a transaction. Maintains a cache of known
 * local shards.
 */
abstract class TransactionComponentFactory {
    // FIXME: this needs to be invalidated
    private final ConcurrentMap<String, DOMStoreTransactionFactory> knownLocal = new ConcurrentHashMap<>();

    private final ActorContext actorContext;

    protected TransactionComponentFactory(final ActorContext actorContext) {
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

        // FIXME: for remote chained transactions we need to track the last transaction's outstanding futures
        //        to deal with leader/transaction actors synchronization. The problem is that we may be sending
        //        the 'commit transaction' message to the transaction actor, while the 'create transaction' is
        //        being sent to the leader -- which may still see the transaction as open.
        //        Keep track of that right hear

        Future<PrimaryShardInfo> findPrimaryFuture = findPrimaryShard(shardName);
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


        return new RemoteTransactionComponent(callback, parent.getRemoteContext(), parent.getType());
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

    /**
     * Callback invoked from child transactions to push any futures, which need to
     * be waited for before the next transaction is allocated.
     * @param cohortFutures Collection of futures
     */
    protected abstract <T> void onTransactionReady(@Nonnull Collection<Future<T>> cohortFutures);

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
