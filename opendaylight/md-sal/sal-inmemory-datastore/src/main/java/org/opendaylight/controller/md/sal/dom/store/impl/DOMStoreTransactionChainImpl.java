/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.store.impl;

import com.google.common.base.Preconditions;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import org.opendaylight.controller.md.sal.dom.store.impl.SnapshotBackedWriteTransaction.TransactionReadyPrototype;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionChain;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DOMStoreTransactionChainImpl extends TransactionReadyPrototype implements DOMStoreTransactionChain {
    private static abstract class State {
        /**
         * Allocate a new snapshot.
         *
         * @return A new snapshot
         */
        protected abstract DataTreeSnapshot getSnapshot();
    }

    private static final class Idle extends State {
        private final InMemoryDOMDataStore store;

        Idle(final InMemoryDOMDataStore store) {
            this.store = Preconditions.checkNotNull(store);
        }

        @Override
        protected DataTreeSnapshot getSnapshot() {
            return store.takeSnapshot();
        }
    }

    /**
     * We have a transaction out there.
     */
    private static final class Allocated extends State {
        private static final AtomicReferenceFieldUpdater<Allocated, DataTreeSnapshot> SNAPSHOT_UPDATER =
                AtomicReferenceFieldUpdater.newUpdater(Allocated.class, DataTreeSnapshot.class, "snapshot");
        private final DOMStoreWriteTransaction transaction;
        private volatile DataTreeSnapshot snapshot;

        Allocated(final DOMStoreWriteTransaction transaction) {
            this.transaction = Preconditions.checkNotNull(transaction);
        }

        public DOMStoreWriteTransaction getTransaction() {
            return transaction;
        }

        @Override
        protected DataTreeSnapshot getSnapshot() {
            final DataTreeSnapshot ret = snapshot;
            Preconditions.checkState(ret != null, "Previous transaction %s is not ready yet", transaction.getIdentifier());
            return ret;
        }

        void setSnapshot(final DataTreeSnapshot snapshot) {
            final boolean success = SNAPSHOT_UPDATER.compareAndSet(this, null, snapshot);
            Preconditions.checkState(success, "Transaction %s has already been marked as ready", transaction.getIdentifier());
        }
    }

    /**
     * Chain is logically shut down, no further allocation allowed.
     */
    private static final class Shutdown extends State {
        private final String message;

        Shutdown(final String message) {
            this.message = Preconditions.checkNotNull(message);
        }

        @Override
        protected DataTreeSnapshot getSnapshot() {
            throw new IllegalStateException(message);
        }
    }

    private static final AtomicReferenceFieldUpdater<DOMStoreTransactionChainImpl, State> STATE_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(DOMStoreTransactionChainImpl.class, State.class, "state");
    private static final Logger LOG = LoggerFactory.getLogger(DOMStoreTransactionChainImpl.class);
    private static final Shutdown CLOSED = new Shutdown("Transaction chain is closed");
    private static final Shutdown FAILED = new Shutdown("Transaction chain has failed");
    private final InMemoryDOMDataStore store;
    private final Idle idleState;
    private volatile State state;

    DOMStoreTransactionChainImpl(final InMemoryDOMDataStore store) {
        this.store = Preconditions.checkNotNull(store);
        idleState = new Idle(store);
        state = idleState;
    }

    private Entry<State, DataTreeSnapshot> getSnapshot() {
        final State localState = state;
        return new SimpleEntry<>(localState, localState.getSnapshot());
    }

    private boolean recordTransaction(final State expected, final DOMStoreWriteTransaction transaction) {
        final State state = new Allocated(transaction);
        return STATE_UPDATER.compareAndSet(this, expected, state);
    }

    @Override
    public DOMStoreReadTransaction newReadOnlyTransaction() {
        final Entry<State, DataTreeSnapshot> entry = getSnapshot();
        return new SnapshotBackedReadTransaction(store.nextIdentifier(), store.getDebugTransactions(), entry.getValue());
    }

    @Override
    public DOMStoreReadWriteTransaction newReadWriteTransaction() {
        Entry<State, DataTreeSnapshot> entry;
        DOMStoreReadWriteTransaction ret;

        do {
            entry = getSnapshot();
            ret = new SnapshotBackedReadWriteTransaction(store.nextIdentifier(),
                store.getDebugTransactions(), entry.getValue(), this);
        } while (!recordTransaction(entry.getKey(), ret));

        return ret;
    }

    @Override
    public DOMStoreWriteTransaction newWriteOnlyTransaction() {
        Entry<State, DataTreeSnapshot> entry;
        DOMStoreWriteTransaction ret;

        do {
            entry = getSnapshot();
            ret = new SnapshotBackedWriteTransaction(store.nextIdentifier(),
                store.getDebugTransactions(), entry.getValue(), this);
        } while (!recordTransaction(entry.getKey(), ret));

        return ret;
    }

    @Override
    protected void transactionAborted(final SnapshotBackedWriteTransaction tx) {
        final State localState = state;
        if (localState instanceof Allocated) {
            final Allocated allocated = (Allocated)localState;
            if (allocated.getTransaction().equals(tx)) {
                final boolean success = STATE_UPDATER.compareAndSet(this, localState, idleState);
                if (!success) {
                    LOG.info("State already transitioned from {} to {}", localState, state);
                }
            }
        }
    }

    @Override
    protected DOMStoreThreePhaseCommitCohort transactionReady(final SnapshotBackedWriteTransaction tx, final DataTreeModification tree) {
        final State localState = state;

        if (localState instanceof Allocated) {
            final Allocated allocated = (Allocated)localState;
            final DOMStoreWriteTransaction transaction = allocated.getTransaction();
            Preconditions.checkState(tx.equals(transaction), "Mis-ordered ready transaction %s last allocated was %s", tx, transaction);
            allocated.setSnapshot(tree);
        } else {
            LOG.debug("Ignoring transaction {} readiness due to state {}", tx, localState);
        }

        return new ChainedTransactionCommitImpl(tx, store.transactionReady(tx, tree), this);
    }

    @Override
    public void close() {
        final State localState = state;

        do {
            Preconditions.checkState(!CLOSED.equals(localState), "Transaction chain {} has been closed", this);

            if (FAILED.equals(localState)) {
                LOG.debug("Ignoring user close in failed state");
                return;
            }
        } while (!STATE_UPDATER.compareAndSet(this, localState, CLOSED));
    }

    void onTransactionFailed(final SnapshotBackedWriteTransaction transaction, final Throwable t) {
        LOG.debug("Transaction chain {} failed on transaction {}", this, transaction, t);
        state = FAILED;
    }

    void onTransactionCommited(final SnapshotBackedWriteTransaction transaction) {
        // If the committed transaction was the one we allocated last,
        // we clear it and the ready snapshot, so the next transaction
        // allocated refers to the data tree directly.
        final State localState = state;

        if (!(localState instanceof Allocated)) {
            LOG.debug("Ignoring successful transaction {} in state {}", transaction, localState);
            return;
        }

        final Allocated allocated = (Allocated)localState;
        final DOMStoreWriteTransaction tx = allocated.getTransaction();
        if (!tx.equals(transaction)) {
            LOG.debug("Ignoring non-latest successful transaction {} in state {}", transaction, allocated);
            return;
        }

        if (!STATE_UPDATER.compareAndSet(this, localState, idleState)) {
            LOG.debug("Transaction chain {} has already transitioned from {} to {}, not making it idle", this, localState, state);
        }
    }
}