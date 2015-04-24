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
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryShardInfo;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.opendaylight.controller.cluster.datastore.utils.ShardInfoListener;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

/**
 * Factory for creating components to a transaction. Maintains a cache of known
 * local transaction factories.
 */
abstract class AbstractTransactionComponentFactory<F extends LocalTransactionFactory> implements ShardInfoListener {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractTransactionComponentFactory.class);

    protected static final AtomicLong TX_COUNTER = new AtomicLong();

    private final ConcurrentMap<String, F> knownLocal = new ConcurrentHashMap<>();
    private final ActorContext actorContext;

    protected AbstractTransactionComponentFactory(final ActorContext actorContext) {
        this.actorContext = Preconditions.checkNotNull(actorContext);
    }

    final ActorContext getActorContext() {
        return actorContext;
    }

    final AbstractTransactionComponent newTransactionComponent(final TransactionProxy parent, final String shardName) {
        final LocalTransactionFactory local = knownLocal.get(shardName);
        if (local != null) {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Tx {} - Creating local component for shard {} using factory {}",
                        parent.getIdentifier(), shardName, local);
            }
            return createLocalComponent(local, parent);
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("Tx {}: Creating remote component for shard {}", parent.getIdentifier(), shardName);
        }

        Future<PrimaryShardInfo> findPrimaryFuture = findPrimaryShard(shardName);
        final TransactionFutureCallback callback = new TransactionFutureCallback(parent, shardName);
        findPrimaryFuture.onComplete(new OnComplete<PrimaryShardInfo>() {
            @Override
            public void onComplete(final Throwable failure, final PrimaryShardInfo primaryShardInfo) {
                if (failure == null) {
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("Tx {}: Found primary {} for shard {}", parent.getIdentifier(),
                                primaryShardInfo.getPrimaryShardActor(), shardName);
                    }

                    callback.setPrimaryShard(primaryShardInfo.getPrimaryShardActor());
                    updateShardInfo(shardName, primaryShardInfo);
                } else {
                    LOG.debug("Tx {}: Find primary for shard {} failed", parent.getIdentifier(),
                            shardName, failure);

                    callback.createTransactionContext(failure, null);
                }
            }
        }, actorContext.getClientDispatcher());

        return new RemoteTransactionComponent(callback, parent);
    }

    void updateShardInfo(final String shardName, final PrimaryShardInfo primaryShardInfo) {
        final Optional<DataTree> maybeDataTree = primaryShardInfo.getLocalShardDataTree();
        if (maybeDataTree.isPresent()) {
            knownLocal.put(shardName, factoryForShard(shardName, primaryShardInfo.getPrimaryShardActor(), maybeDataTree.get()));
            LOG.debug("Shard {} resolved to local data tree", shardName);
        }
    }

    @Override
    public void onShardInfoUpdated(final String shardName, final PrimaryShardInfo primaryShardInfo) {
        final F existing = knownLocal.get(shardName);
        if (existing != null) {
            if (primaryShardInfo != null) {
                final Optional<DataTree> maybeDataTree = primaryShardInfo.getLocalShardDataTree();
                if (maybeDataTree.isPresent()) {
                    final DataTree newDataTree = maybeDataTree.get();
                    final DataTree oldDataTree = dataTreeForFactory(existing);
                    if (!oldDataTree.equals(newDataTree)) {
                        final F newChain = factoryForShard(shardName, primaryShardInfo.getPrimaryShardActor(), newDataTree);
                        knownLocal.replace(shardName, existing, newChain);
                        LOG.debug("Replaced shard {} local data tree to {}", shardName, newDataTree);
                    }

                    return;
                }
            }
            if (knownLocal.remove(shardName, existing)) {
                LOG.debug("Shard {} invalidated data tree {}", shardName, existing);
            } else {
                LOG.debug("Shard {} failed to invalidate data tree {} ... strange", shardName, existing);
            }
        }
    }

    protected String getMemberName() {
        String memberName = getActorContext().getCurrentMemberName();
        if (memberName == null) {
            memberName = "UNKNOWN-MEMBER";
        }

        return memberName;
    }

    /**
     * Create an identifier for the next TransactionProxy attached to this component
     * factory.
     * @return Transaction identifier, may not be null.
     */
    protected abstract TransactionIdentifier nextIdentifier();

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
    protected abstract F factoryForShard(String shardName, ActorSelection shardLeader, DataTree dataTree);

    /**
     * Extract the backing data tree from a particular factory.
     *
     * @param factory Transaction factory
     * @return Backing data tree
     */
    protected abstract DataTree dataTreeForFactory(F factory);

    /**
     * Callback invoked from child transactions to push any futures, which need to
     * be waited for before the next transaction is allocated.
     * @param cohortFutures Collection of futures
     */
    protected abstract <T> void onTransactionReady(@Nonnull TransactionIdentifier transaction, @Nonnull Collection<Future<T>> cohortFutures);

    private static AbstractTransactionComponent createLocalComponent(final LocalTransactionFactory factory, final TransactionProxy parent) {
        switch (parent.getType()) {
        case READ_ONLY:
            return new LocalReadTransactionComponent(parent.getIdentifier(), factory.newReadOnlyTransaction(parent.getIdentifier()));
        case READ_WRITE:
            return new LocalReadWriteTransactionComponent(parent.getIdentifier(), factory.newReadWriteTransaction(parent.getIdentifier()));
        case WRITE_ONLY:
            return new LocalWriteTransactionComponent(parent.getIdentifier(), factory.newWriteOnlyTransaction(parent.getIdentifier()));
        default:
          throw new IllegalArgumentException("Unhandled transaction type " + parent.getType());
        }
    }
}
