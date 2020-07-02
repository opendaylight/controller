/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import akka.actor.ActorSelection;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryShardInfo;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadWriteTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ReadOnlyDataTree;
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
    @SuppressWarnings("rawtypes")
    private static final AtomicLongFieldUpdater<AbstractTransactionContextFactory> TX_COUNTER_UPDATER =
            AtomicLongFieldUpdater.newUpdater(AbstractTransactionContextFactory.class, "nextTx");

    private final ConcurrentMap<String, F> knownLocal = new ConcurrentHashMap<>();
    private final LocalHistoryIdentifier historyId;
    private final ActorUtils actorUtils;

    // Used via TX_COUNTER_UPDATER
    @SuppressWarnings("unused")
    private volatile long nextTx;

    protected AbstractTransactionContextFactory(final ActorUtils actorUtils, final LocalHistoryIdentifier historyId) {
        this.actorUtils = requireNonNull(actorUtils);
        this.historyId = requireNonNull(historyId);
    }

    final ActorUtils getActorUtils() {
        return actorUtils;
    }

    final LocalHistoryIdentifier getHistoryId() {
        return historyId;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private TransactionContext maybeCreateLocalTransactionContext(final TransactionProxy parent,
            final String shardName) {
        final LocalTransactionFactory local = knownLocal.get(shardName);
        if (local != null) {
            LOG.debug("Tx {} - Creating local component for shard {} using factory {}", parent.getIdentifier(),
                shardName, local);

            try {
                return createLocalTransactionContext(local, parent);
            } catch (Exception e) {
                return new NoOpTransactionContext(e, parent.getIdentifier());
            }
        }

        return null;
    }

    private AbstractTransactionContextWrapper maybeCreateDirectTransactionContextWrapper(
            final PrimaryShardInfo primaryShardInfo, final TransactionProxy parent,
            final String shardName, final AbstractTransactionContextWrapper transactionContextWrapper) {
        LOG.debug("Tx {}: Found primary {} for shard {}, trying to use DirectTransactionContextWrapper",
                parent.getIdentifier(), primaryShardInfo.getPrimaryShardActor(), shardName);
        updateShardInfo(shardName, primaryShardInfo);
        final TransactionContext localContext = maybeCreateLocalTransactionContext(parent, shardName);
        try {
            if (localContext != null) {
                LOG.debug("Local transaction context created successfully, using DirectTransactionWrapper");
                return new DirectTransactionContextWrapper(parent.getIdentifier(), actorUtils, shardName,
                        localContext);
            } else {
                LOG.debug("Local transaction context creation failed, using DelayedTransactionWrapper");
                final RemoteTransactionContextSupport remote = new RemoteTransactionContextSupport(
                        (DelayedTransactionContextWrapper) transactionContextWrapper, parent, shardName);
                remote.setPrimaryShard(primaryShardInfo);
                return transactionContextWrapper;
            }
        } finally {
            onTransactionContextCreated(parent.getIdentifier());
        }
    }

    private void onFindPrimaryShardSuccess(final PrimaryShardInfo primaryShardInfo, final TransactionProxy parent,
            final String shardName, final DelayedTransactionContextWrapper transactionContextWrapper) {
        LOG.debug("Tx {}: Found primary {} for shard {}", parent.getIdentifier(),
                primaryShardInfo.getPrimaryShardActor(), shardName);

        updateShardInfo(shardName, primaryShardInfo);

        final TransactionContext localContext = maybeCreateLocalTransactionContext(parent, shardName);
        try {
            if (localContext != null) {
                transactionContextWrapper.executePriorTransactionOperations(localContext);
            } else {
                final RemoteTransactionContextSupport remote = new RemoteTransactionContextSupport(
                        transactionContextWrapper, parent, shardName);
                remote.setPrimaryShard(primaryShardInfo);
            }
        } finally {
            onTransactionContextCreated(parent.getIdentifier());
        }
    }

    private void onFindPrimaryShardFailure(final Throwable failure, final TransactionProxy parent,
            final String shardName, final DelayedTransactionContextWrapper transactionContextWrapper) {
        LOG.debug("Tx {}: Find primary for shard {} failed", parent.getIdentifier(), shardName, failure);

        try {
            transactionContextWrapper.executePriorTransactionOperations(
                    new NoOpTransactionContext(failure, parent.getIdentifier()));
        } finally {
            onTransactionContextCreated(parent.getIdentifier());
        }
    }

    final AbstractTransactionContextWrapper newTransactionContextWrapper(final TransactionProxy parent,
                                                                         final String shardName) {
        DelayedTransactionContextWrapper contextWrapper = new DelayedTransactionContextWrapper(
                parent.getIdentifier(), actorUtils, shardName);
        final Future<PrimaryShardInfo> findPrimaryFuture = findPrimaryShard(shardName, parent.getIdentifier());
        if (findPrimaryFuture.isCompleted()) {
            final Try<PrimaryShardInfo> maybe = findPrimaryFuture.value().get();
            if (maybe.isSuccess()) {
                return maybeCreateDirectTransactionContextWrapper(maybe.get(), parent, shardName,
                        contextWrapper);
            } else {
                onFindPrimaryShardFailure(maybe.failed().get(), parent, shardName,
                        contextWrapper);
            }
        } else {
            final DelayedTransactionContextWrapper finalContextWrapper =
                    contextWrapper;
            findPrimaryFuture.onComplete((result) -> {
                if (result.isSuccess()) {
                    onFindPrimaryShardSuccess(result.get(), parent, shardName, finalContextWrapper);
                } else {
                    onFindPrimaryShardFailure(result.failed().get(), parent, shardName, finalContextWrapper);
                }
                return null;
            }, actorUtils.getClientDispatcher());
        }
        return contextWrapper;

    }

    private void updateShardInfo(final String shardName, final PrimaryShardInfo primaryShardInfo) {
        final Optional<ReadOnlyDataTree> maybeDataTree = primaryShardInfo.getLocalShardDataTree();
        if (maybeDataTree.isPresent()) {
            if (!knownLocal.containsKey(shardName)) {
                LOG.debug("Shard {} resolved to local data tree - adding local factory", shardName);

                F factory = factoryForShard(shardName, primaryShardInfo.getPrimaryShardActor(), maybeDataTree.get());
                knownLocal.putIfAbsent(shardName, factory);
            }
        } else if (knownLocal.containsKey(shardName)) {
            LOG.debug("Shard {} invalidating local data tree", shardName);

            knownLocal.remove(shardName);
        }
    }

    protected final MemberName getMemberName() {
        return historyId.getClientId().getFrontendId().getMemberName();
    }

    /**
     * Create an identifier for the next TransactionProxy attached to this component
     * factory.
     * @return Transaction identifier, may not be null.
     */
    protected final TransactionIdentifier nextIdentifier() {
        return new TransactionIdentifier(historyId, TX_COUNTER_UPDATER.getAndIncrement(this));
    }

    /**
     * Find the primary shard actor.
     *
     * @param shardName Shard name
     * @return Future containing shard information.
     */
    protected abstract Future<PrimaryShardInfo> findPrimaryShard(@NonNull String shardName,
            @NonNull TransactionIdentifier txId);

    /**
     * Create local transaction factory for specified shard, backed by specified shard leader
     * and data tree instance.
     *
     * @param shardName the shard name
     * @param shardLeader the shard leader
     * @param dataTree Backing data tree instance. The data tree may only be accessed in
     *                 read-only manner.
     * @return Transaction factory for local use.
     */
    protected abstract F factoryForShard(String shardName, ActorSelection shardLeader, ReadOnlyDataTree dataTree);

    /**
     * Callback invoked from child transactions to push any futures, which need to
     * be waited for before the next transaction is allocated.
     * @param cohortFutures Collection of futures
     */
    protected abstract <T> void onTransactionReady(@NonNull TransactionIdentifier transaction,
            @NonNull Collection<Future<T>> cohortFutures);

    /**
     * Callback invoked when the internal TransactionContext has been created for a transaction.
     *
     * @param transactionId the ID of the transaction.
     */
    protected abstract void onTransactionContextCreated(@NonNull TransactionIdentifier transactionId);

    private static TransactionContext createLocalTransactionContext(final LocalTransactionFactory factory,
                                                                    final TransactionProxy parent) {

        switch (parent.getType()) {
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
