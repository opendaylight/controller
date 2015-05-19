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
import scala.util.Try;

/**
 * Factory for creating local and remote TransactionContext instances. Maintains a cache of known local
 * transaction factories.
 */
abstract class AbstractTransactionContextFactory<F extends LocalTransactionFactory>
        implements ShardInfoListener, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractTransactionContextFactory.class);

    protected static final AtomicLong TX_COUNTER = new AtomicLong();

    private final ConcurrentMap<String, F> knownLocal = new ConcurrentHashMap<>();
    private final ActorContext actorContext;

    protected AbstractTransactionContextFactory(final ActorContext actorContext) {
        this.actorContext = Preconditions.checkNotNull(actorContext);
    }

    final ActorContext getActorContext() {
        return actorContext;
    }

    private TransactionContext maybeCreateLocalTransactionContext(final TransactionProxy parent, final String shardName) {
        final LocalTransactionFactory local = knownLocal.get(shardName);
        if (local != null) {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Tx {} - Creating local component for shard {} using factory {}",
                        parent.getIdentifier(), shardName, local);
            }
            return createLocalTransactionContext(local, parent);
        }

        return null;
    }

    private void onFindPrimaryShardSuccess(PrimaryShardInfo primaryShardInfo, TransactionProxy parent,
            String shardName, TransactionContextWrapper transactionContextAdapter) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("Tx {}: Found primary {} for shard {}", parent.getIdentifier(),
                    primaryShardInfo.getPrimaryShardActor(), shardName);
        }

        updateShardInfo(shardName, primaryShardInfo);

        TransactionContext localContext = maybeCreateLocalTransactionContext(parent, shardName);
        if(localContext != null) {
            transactionContextAdapter.executePriorTransactionOperations(localContext);
        } else {
            RemoteTransactionContextSupport remote = new RemoteTransactionContextSupport(transactionContextAdapter,
                    parent, shardName);
            remote.setPrimaryShard(primaryShardInfo.getPrimaryShardActor());
        }
    }

    private void onFindPrimaryShardFailure(Throwable failure, TransactionProxy parent,
            String shardName, TransactionContextWrapper transactionContextAdapter) {
        LOG.debug("Tx {}: Find primary for shard {} failed", parent.getIdentifier(), shardName, failure);

        transactionContextAdapter.executePriorTransactionOperations(new NoOpTransactionContext(failure,
                parent.getIdentifier(), parent.getLimiter()));
    }

    final TransactionContextWrapper newTransactionAdapter(final TransactionProxy parent, final String shardName) {
        final TransactionContextWrapper transactionContextAdapter = new TransactionContextWrapper(parent.getIdentifier());

        Future<PrimaryShardInfo> findPrimaryFuture = findPrimaryShard(shardName);
        if(findPrimaryFuture.isCompleted()) {
            Try<PrimaryShardInfo> maybe = findPrimaryFuture.value().get();
            if(maybe.isSuccess()) {
                onFindPrimaryShardSuccess(maybe.get(), parent, shardName, transactionContextAdapter);
            } else {
                onFindPrimaryShardFailure(maybe.failed().get(), parent, shardName, transactionContextAdapter);
            }
        } else {
            findPrimaryFuture.onComplete(new OnComplete<PrimaryShardInfo>() {
                @Override
                public void onComplete(final Throwable failure, final PrimaryShardInfo primaryShardInfo) {
                    if (failure == null) {
                        onFindPrimaryShardSuccess(primaryShardInfo, parent, shardName, transactionContextAdapter);
                    } else {
                        onFindPrimaryShardFailure(failure, parent, shardName, transactionContextAdapter);
                    }
                }
            }, actorContext.getClientDispatcher());
        }

        return transactionContextAdapter;
    }

    private void updateShardInfo(final String shardName, final PrimaryShardInfo primaryShardInfo) {
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

    private static TransactionContext createLocalTransactionContext(final LocalTransactionFactory factory, final TransactionProxy parent) {
        return new LocalTransactionContext(parent.getIdentifier(), factory.newReadWriteTransaction(parent.getIdentifier()), parent.getLimiter());
    }
}
