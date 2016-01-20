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
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;
import scala.util.Try;

/**
 * Factory for creating local and remote TransactionContext instances. Maintains a cache of known local
 * transaction factories.
 */
abstract class AbstractTransactionContextFactory<F extends LocalTransactionFactory> implements AutoCloseable {
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

            try {
                return createLocalTransactionContext(local, parent);
            } catch(Exception e) {
                return new NoOpTransactionContext(e, parent.getIdentifier());
            }
        }

        return null;
    }

    private void onFindPrimaryShardSuccess(PrimaryShardInfo primaryShardInfo, TransactionProxy parent,
            String shardName, TransactionContextWrapper transactionContextWrapper) {
        if(LOG.isDebugEnabled()) {
            LOG.debug("Tx {}: Found primary {} for shard {}", parent.getIdentifier(),
                    primaryShardInfo.getPrimaryShardActor(), shardName);
        }

        updateShardInfo(shardName, primaryShardInfo);

        try {
            TransactionContext localContext = maybeCreateLocalTransactionContext(parent, shardName);
            if(localContext != null) {
                transactionContextWrapper.executePriorTransactionOperations(localContext);
            } else {
                RemoteTransactionContextSupport remote = new RemoteTransactionContextSupport(transactionContextWrapper,
                        parent, shardName);
                remote.setPrimaryShard(primaryShardInfo);
            }
        } finally {
            onTransactionContextCreated(parent.getIdentifier());
        }
    }

    private void onFindPrimaryShardFailure(Throwable failure, TransactionProxy parent,
            String shardName, TransactionContextWrapper transactionContextWrapper) {
        LOG.debug("Tx {}: Find primary for shard {} failed", parent.getIdentifier(), shardName, failure);

        try {
            transactionContextWrapper.executePriorTransactionOperations(new NoOpTransactionContext(failure,
                    parent.getIdentifier()));
        } finally {
            onTransactionContextCreated(parent.getIdentifier());
        }
    }

    final TransactionContextWrapper newTransactionContextWrapper(final TransactionProxy parent, final String shardName) {
        final TransactionContextWrapper transactionContextWrapper =
                new TransactionContextWrapper(parent.getIdentifier(), actorContext);

        Future<PrimaryShardInfo> findPrimaryFuture = findPrimaryShard(shardName, parent.getIdentifier());
        if(findPrimaryFuture.isCompleted()) {
            Try<PrimaryShardInfo> maybe = findPrimaryFuture.value().get();
            if(maybe.isSuccess()) {
                onFindPrimaryShardSuccess(maybe.get(), parent, shardName, transactionContextWrapper);
            } else {
                onFindPrimaryShardFailure(maybe.failed().get(), parent, shardName, transactionContextWrapper);
            }
        } else {
            findPrimaryFuture.onComplete(new OnComplete<PrimaryShardInfo>() {
                @Override
                public void onComplete(final Throwable failure, final PrimaryShardInfo primaryShardInfo) {
                    if (failure == null) {
                        onFindPrimaryShardSuccess(primaryShardInfo, parent, shardName, transactionContextWrapper);
                    } else {
                        onFindPrimaryShardFailure(failure, parent, shardName, transactionContextWrapper);
                    }
                }
            }, actorContext.getClientDispatcher());
        }

        return transactionContextWrapper;
    }

    private void updateShardInfo(final String shardName, final PrimaryShardInfo primaryShardInfo) {
        final Optional<DataTree> maybeDataTree = primaryShardInfo.getLocalShardDataTree();
        if (maybeDataTree.isPresent()) {
            if(!knownLocal.containsKey(shardName)) {
                LOG.debug("Shard {} resolved to local data tree - adding local factory", shardName);

                F factory = factoryForShard(shardName, primaryShardInfo.getPrimaryShardActor(), maybeDataTree.get());
                knownLocal.putIfAbsent(shardName, factory);
            }
        } else if(knownLocal.containsKey(shardName)) {
            LOG.debug("Shard {} invalidating local data tree", shardName);

            knownLocal.remove(shardName);
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
    protected abstract Future<PrimaryShardInfo> findPrimaryShard(@Nonnull String shardName,
            @Nonnull TransactionIdentifier txId);

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
     * Callback invoked from child transactions to push any futures, which need to
     * be waited for before the next transaction is allocated.
     * @param cohortFutures Collection of futures
     */
    protected abstract <T> void onTransactionReady(@Nonnull TransactionIdentifier transaction, @Nonnull Collection<Future<T>> cohortFutures);

    /**
     * Callback invoked when the internal TransactionContext has been created for a transaction.
     *
     * @param transactionId the ID of the transaction.
     */
    protected abstract void onTransactionContextCreated(@Nonnull TransactionIdentifier transactionId);

    private static TransactionContext createLocalTransactionContext(final LocalTransactionFactory factory,
                                                                    final TransactionProxy parent) {

        switch(parent.getType()) {
            case READ_ONLY:
                final DOMStoreReadTransaction readOnly = factory.newReadOnlyTransaction(parent.getIdentifier());
                return new LocalTransactionContext(readOnly, parent.getIdentifier(), factory) {
                    @Override
                    protected DOMStoreWriteTransaction getWriteDelegate() {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    protected DOMStoreReadTransaction getReadDelegate() {
                        return readOnly;
                    }
                };
            case READ_WRITE:
                final DOMStoreReadWriteTransaction readWrite = factory.newReadWriteTransaction(parent.getIdentifier());
                return new LocalTransactionContext(readWrite, parent.getIdentifier(), factory) {
                    @Override
                    protected DOMStoreWriteTransaction getWriteDelegate() {
                        return readWrite;
                    }

                    @Override
                    protected DOMStoreReadTransaction getReadDelegate() {
                        return readWrite;
                    }
                };
            case WRITE_ONLY:
                final DOMStoreWriteTransaction writeOnly = factory.newWriteOnlyTransaction(parent.getIdentifier());
                return new LocalTransactionContext(writeOnly, parent.getIdentifier(), factory) {
                    @Override
                    protected DOMStoreWriteTransaction getWriteDelegate() {
                        return writeOnly;
                    }

                    @Override
                    protected DOMStoreReadTransaction getReadDelegate() {
                        throw new UnsupportedOperationException();
                    }
                };
             default:
                 throw new IllegalArgumentException("Invalid transaction type: " + parent.getType());
        }
    }
}
