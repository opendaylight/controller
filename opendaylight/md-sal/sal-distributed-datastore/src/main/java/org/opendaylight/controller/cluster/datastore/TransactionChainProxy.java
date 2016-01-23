/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSelection;
import akka.dispatch.Futures;
import akka.dispatch.OnComplete;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Function;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionChainIdentifier;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransactionChain;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryShardInfo;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainClosedException;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;
import scala.concurrent.Promise;

/**
 * A chain of {@link TransactionProxy}s. It allows a single open transaction to be open
 * at a time. For remote transactions, it also tracks the outstanding readiness requests
 * towards the shard and unblocks operations only after all have completed.
 */
final class TransactionChainProxy extends AbstractTransactionContextFactory<LocalTransactionChain> implements DOMStoreTransactionChain {
    private static abstract class State {
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

    private static abstract class Pending extends State {
        private final TransactionIdentifier transaction;
        private final Future<?> previousFuture;

        Pending(final TransactionIdentifier transaction, final Future<?> previousFuture) {
            this.previousFuture = previousFuture;
            this.transaction = Preconditions.checkNotNull(transaction);
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

    private static abstract class DefaultState extends State {
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
            throw new TransactionChainClosedException("Transaction chain has been closed");
        }
    };

    private static final Logger LOG = LoggerFactory.getLogger(TransactionChainProxy.class);
    private static final AtomicInteger CHAIN_COUNTER = new AtomicInteger();
    private static final AtomicReferenceFieldUpdater<TransactionChainProxy, State> STATE_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(TransactionChainProxy.class, State.class, "currentState");

    private final TransactionChainIdentifier transactionChainId;
    private final TransactionContextFactory parent;
    private volatile State currentState = IDLE_STATE;

    /**
     * This map holds Promise instances for each read-only tx. It is used to maintain ordering of tx creates
     * wrt to read-only tx's between this class and a LocalTransactionChain since they're bridged by
     * asynchronous futures. Otherwise, in the following scenario, eg:
     *
     *   1) Create write tx1 on chain
     *   2) do write and submit
     *   3) Create read-only tx2 on chain and issue read
     *   4) Create write tx3 on chain, do write but do not submit
     *
     * if the sequence/timing is right, tx3 may create its local tx on the LocalTransactionChain before tx2,
     * which results in tx2 failing b/c tx3 isn't ready yet. So maintaining ordering prevents this issue
     * (see Bug 4774).
     * <p>
     * A Promise is added via newReadOnlyTransaction. When the parent class completes the primary shard
     * lookup and creates the TransactionContext (either success or failure), onTransactionContextCreated is
     * called which completes the Promise. A write tx that is created prior to completion will wait on the
     * Promise's Future via findPrimaryShard.
     */
    private final ConcurrentMap<TransactionIdentifier, Promise<Object>> priorReadOnlyTxPromises = new ConcurrentHashMap<>();

    TransactionChainProxy(final TransactionContextFactory parent) {
        super(parent.getActorContext());

        transactionChainId = new TransactionChainIdentifier(parent.getActorContext().getCurrentMemberName(), CHAIN_COUNTER.incrementAndGet());
        this.parent = parent;
    }

    public String getTransactionChainId() {
        return transactionChainId.toString();
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
        getActorContext().acquireTxCreationPermit();
        return allocateWriteTransaction(TransactionType.READ_WRITE);
    }

    @Override
    public DOMStoreWriteTransaction newWriteOnlyTransaction() {
        getActorContext().acquireTxCreationPermit();
        return allocateWriteTransaction(TransactionType.WRITE_ONLY);
    }

    @Override
    public void close() {
        currentState = CLOSED_STATE;

        // Send a close transaction chain request to each and every shard

        getActorContext().broadcast(new Function<Short, Object>() {
            @Override
            public Object apply(Short version) {
                return new CloseTransactionChain(transactionChainId.toString(), version).toSerializable();
            }
        });
    }

    private TransactionProxy allocateWriteTransaction(final TransactionType type) {
        State localState = currentState;
        localState.checkReady();

        final TransactionProxy ret = new TransactionProxy(this, type);
        currentState = new Allocated(ret.getIdentifier(), localState.previousFuture());
        return ret;
    }

    @Override
    protected LocalTransactionChain factoryForShard(final String shardName, final ActorSelection shardLeader, final DataTree dataTree) {
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

        if(localState instanceof Pending){
            previousTransactionId = ((Pending) localState).getIdentifier().toString();
            LOG.debug("Tx: {} - waiting for ready futures with pending Tx {}", txId, previousTransactionId);
        } else {
            previousTransactionId = "";
            LOG.debug("Waiting for ready futures on chain {}", getTransactionChainId());
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

        previous.onComplete(onComplete, getActorContext().getClientDispatcher());
        return returnPromise.future();
    }

    private <T> Future<T> combineFutureWithPossiblePriorReadOnlyTxFutures(final Future<T> future,
            final TransactionIdentifier txId) {
        if(!priorReadOnlyTxPromises.containsKey(txId) && !priorReadOnlyTxPromises.isEmpty()) {
            Collection<Entry<TransactionIdentifier, Promise<Object>>> priorReadOnlyTxPromiseEntries =
                    new ArrayList<>(priorReadOnlyTxPromises.entrySet());
            if(priorReadOnlyTxPromiseEntries.isEmpty()) {
                return future;
            }

            List<Future<Object>> priorReadOnlyTxFutures = new ArrayList<>(priorReadOnlyTxPromiseEntries.size());
            for(Entry<TransactionIdentifier, Promise<Object>> entry: priorReadOnlyTxPromiseEntries) {
                LOG.debug("Tx: {} - waiting on future for prior read-only Tx {}", txId, entry.getKey());
                priorReadOnlyTxFutures.add(entry.getValue().future());
            }

            Future<Iterable<Object>> combinedFutures = Futures.sequence(priorReadOnlyTxFutures,
                    getActorContext().getClientDispatcher());

            final Promise<T> returnPromise = Futures.promise();
            final OnComplete<Iterable<Object>> onComplete = new OnComplete<Iterable<Object>>() {
                @Override
                public void onComplete(final Throwable failure, final Iterable<Object> notUsed) {
                    LOG.debug("Tx: {} - prior read-only Tx futures complete", txId);

                    // Complete the returned Promise with the original Future.
                    returnPromise.completeWith(future);
                }
            };

            combinedFutures.onComplete(onComplete, getActorContext().getClientDispatcher());
            return returnPromise.future();
        } else {
            return future;
        }
    }

    @Override
    protected <T> void onTransactionReady(final TransactionIdentifier transaction, final Collection<Future<T>> cohortFutures) {
        final State localState = currentState;
        Preconditions.checkState(localState instanceof Allocated, "Readying transaction %s while state is %s", transaction, localState);
        final TransactionIdentifier currentTx = ((Allocated)localState).getIdentifier();
        Preconditions.checkState(transaction.equals(currentTx), "Readying transaction %s while %s is allocated", transaction, currentTx);

        // Transaction ready and we are not waiting for futures -- go to idle
        if (cohortFutures.isEmpty()) {
            currentState = IDLE_STATE;
            return;
        }

        // Combine the ready Futures into 1
        final Future<Iterable<T>> combined = Futures.sequence(cohortFutures, getActorContext().getClientDispatcher());

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
        }, getActorContext().getClientDispatcher());
    }

    @Override
    protected void onTransactionContextCreated(@Nonnull TransactionIdentifier transactionId) {
        Promise<Object> promise = priorReadOnlyTxPromises.remove(transactionId);
        if(promise != null) {
            promise.success(null);
        }
    }

    @Override
    protected TransactionIdentifier nextIdentifier() {
        return transactionChainId.newTransactionIdentifier();
    }
}
