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
import java.util.HashMap;
import java.util.List;
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
        abstract boolean isReady();
        abstract Future<?> previousFuture();
    }

    private static final class Allocated extends State {
        private final TransactionIdentifier transaction;

        Allocated(final TransactionIdentifier transaction) {
            this.transaction = transaction;
        }

        @Override
        boolean isReady() {
            return false;
        }

        @Override
        Future<?> previousFuture() {
            throw new IllegalStateException("Transaction " + transaction + " not ready");
        }
    }

    private static final class Submitted extends State {
        private final Future<?> previousFuture;

        Submitted(final Future<?> previousFuture) {
            this.previousFuture = Preconditions.checkNotNull(previousFuture);
        }

        @Override
        boolean isReady() {
            return true;
        }

        @Override
        Future<?> previousFuture() {
            return previousFuture;
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
        boolean isReady() {
            return true;
        }
    };

    private static final State CLOSED_STATE = new DefaultState() {
        @Override
        boolean isReady() {
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
        State localState = currentState;
        checkReadyState(localState);

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
        checkReadyState(localState);

        final TransactionProxy ret = new TransactionProxy(this, type);
        currentState = new Allocated(ret.getIdentifier());
        return ret;
    }

    private static void checkReadyState(final State state) {
        Preconditions.checkState(state.isReady(), "Previous transaction is not ready yet");
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
    protected <T> void onTransactionReady(final List<Future<T>> cohortFutures) {
        if (cohortFutures.isEmpty()) {
            currentState = IDLE_STATE;
            return;
        }

        // Combine the ready Futures into 1
        final Future<Iterable<T>> combined = akka.dispatch.Futures.sequence(
                cohortFutures, getActorContext().getClientDispatcher());

        // Record the we have outstanding futures
        final State newState = new Submitted(combined);
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
}
