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
import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransactionChain;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryShardInfo;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainClosedException;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionFactory;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;
import scala.concurrent.Promise;

/**
 * A chain of {@link TransactionProxy}s. Beside tracking transactions it also tracks
 * the
 */
final class ShardedTransactionChain extends TransactionComponentFactory implements DOMStoreTransactionChain {
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

        Pending(final TransactionIdentifier transaction, Future<?> previousFuture) {
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
        Allocated(final TransactionIdentifier transaction, Future<?> previousFuture) {
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

    private static final Logger LOG = LoggerFactory.getLogger(ShardedTransactionChain.class);
    private static final AtomicInteger CHAIN_COUNTER = new AtomicInteger();
    private static final AtomicReferenceFieldUpdater<ShardedTransactionChain, State> STATE_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(ShardedTransactionChain.class, State.class, "currentState");

    // FIXME: this needs to be invalidated somehow
    private final Map<String, LocalTransactionChain> localChains = new HashMap<>();
    private final String transactionChainId;
    private final TransactionComponentFactory parent;
    private final long counter = 0;
    private volatile State currentState = IDLE_STATE;

    ShardedTransactionChain(final TransactionComponentFactory parent) {
        super(parent.getActorContext());
        transactionChainId = parent.getActorContext().getCurrentMemberName() + "-txn-chain-" + CHAIN_COUNTER.incrementAndGet();
        this.parent = Preconditions.checkNotNull(parent);
    }

    public String getTransactionChainId() {
        return transactionChainId;
    }

    @Override
    public DOMStoreReadTransaction newReadOnlyTransaction() {
        currentState.checkReady();
        return new TransactionProxy(this, TransactionType.READ_ONLY);
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
        getActorContext().broadcast(new CloseTransactionChain(transactionChainId).toSerializable());
    }

    private TransactionProxy allocateWriteTransaction(final TransactionType type) {
        State localState = currentState;
        localState.checkReady();

        final TransactionProxy ret = new TransactionProxy(this, type);
        currentState = new Allocated(ret.getIdentifier(), localState.previousFuture());
        return ret;
    }

    long currentCounter() {
        return counter;
    }

    @Override
    protected DOMStoreTransactionFactory factoryForShard(final String shardName, final ActorSelection shardLeader, final DataTree dataTree) {
        LocalTransactionChain ret = localChains.get(shardName);
        if (ret == null) {
            ret = new LocalTransactionChain(this, shardName, shardLeader, dataTree);
            localChains.put(shardName, ret);
            LOG.debug("Allocated transaction chain {} for shard {} leader {}", ret, shardName, shardLeader);
        }

        return ret;
    }

    /**
     * This method is overridden to ensure the previous Tx's ready operations complete
     * before we initiate the next Tx in the chain to avoid creation failures if the
     * previous Tx's ready operations haven't completed yet.
     */
    @Override
    protected Future<PrimaryShardInfo> findPrimaryShard(final String shardName) {
        // Read current state atomically
        final State localState = currentState;

        // There are no outstanding futures, shortcut
        final Future<?> previous = localState.previousFuture();
        if (previous == null) {
            return parent.findPrimaryShard(shardName);
        }

        LOG.debug("Waiting for ready futures for on chain {}", getTransactionChainId());

        // Add a callback for completion of the combined Futures.
        final Promise<PrimaryShardInfo> returnPromise = akka.dispatch.Futures.promise();

        final OnComplete onComplete = new OnComplete() {
            @Override
            public void onComplete(Throwable failure, Object notUsed) {
                if (failure != null) {
                    // A Ready Future failed so fail the returned Promise.
                    returnPromise.failure(failure);
                } else {
                    LOG.debug("Previous Tx readied - sending FindPrimaryShard on chain {}",
                            getTransactionChainId());

                    // Send the FindPrimaryShard message and use the resulting Future to complete the
                    // returned Promise.
                    returnPromise.completeWith(parent.findPrimaryShard(shardName));
                }
            }
        };

        previous.onComplete(onComplete, getActorContext().getClientDispatcher());
        return returnPromise.future();
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
        final Future<Iterable<T>> combined = akka.dispatch.Futures.sequence(
                cohortFutures, getActorContext().getClientDispatcher());

        // Record the we have outstanding futures
        final State newState = new Submitted(transaction, combined);
        currentState = newState;

        // Attach a completion reset, but only if we do not allocate a transaction
        // in-between
        combined.onComplete(new OnComplete<Iterable<T>>() {
            @Override
            public void onComplete(Throwable arg0, Iterable<T> arg1) {
                STATE_UPDATER.compareAndSet(ShardedTransactionChain.this, newState, IDLE_STATE);
            }
        }, getActorContext().getClientDispatcher());
    }

    @Override
    protected TransactionIdentifier nextIdentifier() {
        return parent.nextIdentifier();
    }
}
