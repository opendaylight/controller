/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import akka.actor.ActorSelection;
import akka.dispatch.Futures;
import akka.dispatch.OnComplete;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransactionChain;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryShardInfo;
import org.opendaylight.mdsal.dom.api.DOMTransactionChainClosedException;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadWriteTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreTransactionChain;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.tree.api.ReadOnlyDataTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;
import scala.concurrent.Promise;

/**
 * A chain of {@link TransactionProxy}s. It allows a single open transaction to be open
 * at a time. For remote transactions, it also tracks the outstanding readiness requests
 * towards the shard and unblocks operations only after all have completed.
 */
final class TransactionChainProxy extends AbstractTransactionContextFactory<LocalTransactionChain>
        implements DOMStoreTransactionChain {
    private abstract static class State {
        /**
         * Check if it is okay to allocate a new transaction.
         * @throws IllegalStateException if a transaction may not be allocated.
         */
        abstract void checkReady();

        /**
         * Return the future which needs to be waited for before shard information
         * is returned (which unblocks remote transactions).
         * @return Future to wait for, or null of no wait is necessary
         */
        abstract Future<?> previousFuture();
    }

    private abstract static class Pending extends State {
        private final TransactionIdentifier transaction;
        private final Future<?> previousFuture;

        Pending(final TransactionIdentifier transaction, final Future<?> previousFuture) {
            this.previousFuture = previousFuture;
            this.transaction = requireNonNull(transaction);
        }

        @Override
        final Future<?> previousFuture() {
            return previousFuture;
        }

        final TransactionIdentifier getIdentifier() {
            return transaction;
        }
    }

    private static final class Allocated extends Pending {
        Allocated(final TransactionIdentifier transaction, final Future<?> previousFuture) {
            super(transaction, previousFuture);
        }

        @Override
        void checkReady() {
            throw new IllegalStateException(String.format("Previous transaction %s is not ready yet", getIdentifier()));
        }
    }

    private static final class Submitted extends Pending {
        Submitted(final TransactionIdentifier transaction, final Future<?> previousFuture) {
            super(transaction, previousFuture);
        }

        @Override
        void checkReady() {
            // Okay to allocate
        }
    }

    private abstract static class DefaultState extends State {
        @Override
        final Future<?> previousFuture() {
            return null;
        }
    }

    private static final State IDLE_STATE = new DefaultState() {
        @Override
        void checkReady() {
            // Okay to allocate
        }
    };

    private static final State CLOSED_STATE = new DefaultState() {
        @Override
        void checkReady() {
            throw new DOMTransactionChainClosedException("Transaction chain has been closed");
        }
    };

    private static final Logger LOG = LoggerFactory.getLogger(TransactionChainProxy.class);
    private static final AtomicReferenceFieldUpdater<TransactionChainProxy, State> STATE_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(TransactionChainProxy.class, State.class, "currentState");

    private final TransactionContextFactory parent;
    private volatile State currentState = IDLE_STATE;

    /**
     * This map holds Promise instances for each read-only tx. It is used to maintain ordering of tx creates
     * wrt to read-only tx's between this class and a LocalTransactionChain since they're bridged by
     * asynchronous futures. Otherwise, in the following scenario, eg:
     * <p/>
     *   1) Create write tx1 on chain
     *   2) do write and submit
     *   3) Create read-only tx2 on chain and issue read
     *   4) Create write tx3 on chain, do write but do not submit
     * <p/>
     * if the sequence/timing is right, tx3 may create its local tx on the LocalTransactionChain before tx2,
     * which results in tx2 failing b/c tx3 isn't ready yet. So maintaining ordering prevents this issue
     * (see Bug 4774).
     * <p/>
     * A Promise is added via newReadOnlyTransaction. When the parent class completes the primary shard
     * lookup and creates the TransactionContext (either success or failure), onTransactionContextCreated is
     * called which completes the Promise. A write tx that is created prior to completion will wait on the
     * Promise's Future via findPrimaryShard.
     */
    private final ConcurrentMap<TransactionIdentifier, Promise<Object>> priorReadOnlyTxPromises =
            new ConcurrentHashMap<>();

    TransactionChainProxy(final TransactionContextFactory parent, final LocalHistoryIdentifier historyId) {
        super(parent.getActorUtils(), historyId);
        this.parent = parent;
    }

    @Override
    public DOMStoreReadTransaction newReadOnlyTransaction() {
        currentState.checkReady();
        TransactionProxy transactionProxy = new TransactionProxy(this, TransactionType.READ_ONLY);
        priorReadOnlyTxPromises.put(transactionProxy.getIdentifier(), Futures.<Object>promise());
        return transactionProxy;
    }

    @Override
    public DOMStoreReadWriteTransaction newReadWriteTransaction() {
        getActorUtils().acquireTxCreationPermit();
        return allocateWriteTransaction(TransactionType.READ_WRITE);
    }

    @Override
    public DOMStoreWriteTransaction newWriteOnlyTransaction() {
        getActorUtils().acquireTxCreationPermit();
        return allocateWriteTransaction(TransactionType.WRITE_ONLY);
    }

    @Override
    public void close() {
        currentState = CLOSED_STATE;

        // Send a close transaction chain request to each and every shard

        getActorUtils().broadcast(version -> new CloseTransactionChain(getHistoryId(), version).toSerializable(),
                CloseTransactionChain.class);
    }

    private TransactionProxy allocateWriteTransaction(final TransactionType type) {
        State localState = currentState;
        localState.checkReady();

        final TransactionProxy ret = new TransactionProxy(this, type);
        currentState = new Allocated(ret.getIdentifier(), localState.previousFuture());
        return ret;
    }

    @Override
    protected LocalTransactionChain factoryForShard(final String shardName, final ActorSelection shardLeader,
            final ReadOnlyDataTree dataTree) {
        final LocalTransactionChain ret = new LocalTransactionChain(this, shardLeader, dataTree);
        LOG.debug("Allocated transaction chain {} for shard {} leader {}", ret, shardName, shardLeader);
        return ret;
    }

    /**
     * This method is overridden to ensure the previous Tx's ready operations complete
     * before we initiate the next Tx in the chain to avoid creation failures if the
     * previous Tx's ready operations haven't completed yet.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    protected Future<PrimaryShardInfo> findPrimaryShard(final String shardName, final TransactionIdentifier txId) {
        // Read current state atomically
        final State localState = currentState;

        // There are no outstanding futures, shortcut
        Future<?> previous = localState.previousFuture();
        if (previous == null) {
            return combineFutureWithPossiblePriorReadOnlyTxFutures(parent.findPrimaryShard(shardName, txId), txId);
        }

        final String previousTransactionId;

        if (localState instanceof Pending) {
            previousTransactionId = ((Pending) localState).getIdentifier().toString();
            LOG.debug("Tx: {} - waiting for ready futures with pending Tx {}", txId, previousTransactionId);
        } else {
            previousTransactionId = "";
            LOG.debug("Waiting for ready futures on chain {}", getHistoryId());
        }

        previous = combineFutureWithPossiblePriorReadOnlyTxFutures(previous, txId);

        // Add a callback for completion of the combined Futures.
        final Promise<PrimaryShardInfo> returnPromise = Futures.promise();

        final OnComplete onComplete = new OnComplete() {
            @Override
            public void onComplete(final Throwable failure, final Object notUsed) {
                if (failure != null) {
                    // A Ready Future failed so fail the returned Promise.
                    LOG.error("Tx: {} - ready future failed for previous Tx {}", txId, previousTransactionId);
                    returnPromise.failure(failure);
                } else {
                    LOG.debug("Tx: {} - previous Tx {} readied - proceeding to FindPrimaryShard",
                            txId, previousTransactionId);

                    // Send the FindPrimaryShard message and use the resulting Future to complete the
                    // returned Promise.
                    returnPromise.completeWith(parent.findPrimaryShard(shardName, txId));
                }
            }
        };

        previous.onComplete(onComplete, getActorUtils().getClientDispatcher());
        return returnPromise.future();
    }

    private <T> Future<T> combineFutureWithPossiblePriorReadOnlyTxFutures(final Future<T> future,
            final TransactionIdentifier txId) {
        return priorReadOnlyTxPromises.isEmpty() || priorReadOnlyTxPromises.containsKey(txId) ? future
                // Tough luck, we need do some work
                : combineWithPriorReadOnlyTxFutures(future, txId);
    }

    // Split out of the common path
    private <T> Future<T> combineWithPriorReadOnlyTxFutures(final Future<T> future, final TransactionIdentifier txId) {
        // Take a stable snapshot, and check if we raced
        final List<Entry<TransactionIdentifier, Promise<Object>>> priorReadOnlyTxPromiseEntries =
                new ArrayList<>(priorReadOnlyTxPromises.entrySet());
        if (priorReadOnlyTxPromiseEntries.isEmpty()) {
            return future;
        }

        final List<Future<Object>> priorReadOnlyTxFutures = new ArrayList<>(priorReadOnlyTxPromiseEntries.size());
        for (Entry<TransactionIdentifier, Promise<Object>> entry: priorReadOnlyTxPromiseEntries) {
            LOG.debug("Tx: {} - waiting on future for prior read-only Tx {}", txId, entry.getKey());
            priorReadOnlyTxFutures.add(entry.getValue().future());
        }

        final Future<Iterable<Object>> combinedFutures = Futures.sequence(priorReadOnlyTxFutures,
            getActorUtils().getClientDispatcher());

        final Promise<T> returnPromise = Futures.promise();
        final OnComplete<Iterable<Object>> onComplete = new OnComplete<>() {
            @Override
            public void onComplete(final Throwable failure, final Iterable<Object> notUsed) {
                LOG.debug("Tx: {} - prior read-only Tx futures complete", txId);

                // Complete the returned Promise with the original Future.
                returnPromise.completeWith(future);
            }
        };

        combinedFutures.onComplete(onComplete, getActorUtils().getClientDispatcher());
        return returnPromise.future();
    }

    @Override
    protected <T> void onTransactionReady(final TransactionIdentifier transaction,
            final Collection<Future<T>> cohortFutures) {
        final State localState = currentState;
        checkState(localState instanceof Allocated, "Readying transaction %s while state is %s", transaction,
            localState);
        final TransactionIdentifier currentTx = ((Allocated)localState).getIdentifier();
        checkState(transaction.equals(currentTx), "Readying transaction %s while %s is allocated", transaction,
            currentTx);

        // Transaction ready and we are not waiting for futures -- go to idle
        if (cohortFutures.isEmpty()) {
            currentState = IDLE_STATE;
            return;
        }

        // Combine the ready Futures into 1
        final Future<Iterable<T>> combined = Futures.sequence(cohortFutures, getActorUtils().getClientDispatcher());

        // Record the we have outstanding futures
        final State newState = new Submitted(transaction, combined);
        currentState = newState;

        // Attach a completion reset, but only if we do not allocate a transaction
        // in-between
        combined.onComplete(new OnComplete<Iterable<T>>() {
            @Override
            public void onComplete(final Throwable arg0, final Iterable<T> arg1) {
                STATE_UPDATER.compareAndSet(TransactionChainProxy.this, newState, IDLE_STATE);
            }
        }, getActorUtils().getClientDispatcher());
    }

    @Override
    protected void onTransactionContextCreated(final TransactionIdentifier transactionId) {
        Promise<Object> promise = priorReadOnlyTxPromises.remove(transactionId);
        if (promise != null) {
            promise.success(null);
        }
    }
}
