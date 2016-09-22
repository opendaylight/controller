/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.spi.data;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import org.opendaylight.controller.sal.core.spi.data.SnapshotBackedWriteTransaction.TransactionReadyPrototype;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract implementation of the {@link DOMStoreTransactionChain} interface relying on {@link DataTreeSnapshot} supplier
 * and backend commit coordinator.
 *
 * @param <T> transaction identifier type
 */
@Beta
public abstract class AbstractSnapshotBackedTransactionChain<T> extends TransactionReadyPrototype<T> implements DOMStoreTransactionChain {
    private static abstract class State {
        /**
         * Allocate a new snapshot.
         *
         * @return A new snapshot
         */
        protected abstract DataTreeSnapshot getSnapshot(Object transactionId);
    }

    private static final class Idle extends State {
        private final AbstractSnapshotBackedTransactionChain<?> chain;

        Idle(final AbstractSnapshotBackedTransactionChain<?> chain) {
            this.chain = Preconditions.checkNotNull(chain);
        }

        @Override
        protected DataTreeSnapshot getSnapshot(Object transactionId) {
            return chain.takeSnapshot();
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
        protected DataTreeSnapshot getSnapshot(Object transactionId) {
            final DataTreeSnapshot ret = snapshot;
            Preconditions.checkState(ret != null, "Could not get snapshot for transaction %s - previous transaction %s is not ready yet",
                    transactionId, transaction.getIdentifier());
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
        protected DataTreeSnapshot getSnapshot(Object transactionId) {
            throw new IllegalStateException(message);
        }
    }

    @SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<AbstractSnapshotBackedTransactionChain, State> STATE_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(AbstractSnapshotBackedTransactionChain.class, State.class, "state");
    private static final Logger LOG = LoggerFactory.getLogger(AbstractSnapshotBackedTransactionChain.class);
    private static final Shutdown CLOSED = new Shutdown("Transaction chain is closed");
    private static final Shutdown FAILED = new Shutdown("Transaction chain has failed");
    private final Idle idleState;
    private volatile State state;

    protected AbstractSnapshotBackedTransactionChain() {
        idleState = new Idle(this);
        state = idleState;
    }

    private Entry<State, DataTreeSnapshot> getSnapshot(T transactionId) {
        final State localState = state;
        return new SimpleEntry<>(localState, localState.getSnapshot(transactionId));
    }

    private boolean recordTransaction(final State expected, final DOMStoreWriteTransaction transaction) {
        final State state = new Allocated(transaction);
        return STATE_UPDATER.compareAndSet(this, expected, state);
    }

    @Override
    public final DOMStoreReadTransaction newReadOnlyTransaction() {
        return newReadOnlyTransaction(nextTransactionIdentifier());
    }

    protected DOMStoreReadTransaction newReadOnlyTransaction(T transactionId) {
        final Entry<State, DataTreeSnapshot> entry = getSnapshot(transactionId);
        return SnapshotBackedTransactions.newReadTransaction(transactionId, getDebugTransactions(), entry.getValue());
    }

    @Override
    public final DOMStoreReadWriteTransaction newReadWriteTransaction() {
        return newReadWriteTransaction(nextTransactionIdentifier());
    }

    protected DOMStoreReadWriteTransaction newReadWriteTransaction(T transactionId) {
        Entry<State, DataTreeSnapshot> entry;
        DOMStoreReadWriteTransaction ret;

        do {
            entry = getSnapshot(transactionId);
            ret = new SnapshotBackedReadWriteTransaction<>(transactionId, getDebugTransactions(), entry.getValue(),
                    this);
        } while (!recordTransaction(entry.getKey(), ret));

        return ret;
    }

    @Override
    public final DOMStoreWriteTransaction newWriteOnlyTransaction() {
        return newWriteOnlyTransaction(nextTransactionIdentifier());
    }

    protected DOMStoreWriteTransaction newWriteOnlyTransaction(T transactionId) {
        Entry<State, DataTreeSnapshot> entry;
        DOMStoreWriteTransaction ret;

        do {
            entry = getSnapshot(transactionId);
            ret = new SnapshotBackedWriteTransaction<>(transactionId, getDebugTransactions(), entry.getValue(), this);
        } while (!recordTransaction(entry.getKey(), ret));

        return ret;
    }

    @Override
    protected final void transactionAborted(final SnapshotBackedWriteTransaction<T> tx) {
        final State localState = state;
        if (localState instanceof Allocated) {
            final Allocated allocated = (Allocated)localState;
            if (allocated.getTransaction().equals(tx)) {
                final boolean success = STATE_UPDATER.compareAndSet(this, localState, idleState);
                if (!success) {
                    LOG.warn("Transaction {} aborted, but chain {} state already transitioned from {} to {}, very strange",
                        tx, this, localState, state);
                }
            }
        }
    }

    @Override
    protected final DOMStoreThreePhaseCommitCohort transactionReady(final SnapshotBackedWriteTransaction<T> tx, final DataTreeModification tree) {
        final State localState = state;

        if (localState instanceof Allocated) {
            final Allocated allocated = (Allocated)localState;
            final DOMStoreWriteTransaction transaction = allocated.getTransaction();
            Preconditions.checkState(tx.equals(transaction), "Mis-ordered ready transaction %s last allocated was %s", tx, transaction);
            allocated.setSnapshot(tree);
        } else {
            LOG.debug("Ignoring transaction {} readiness due to state {}", tx, localState);
        }

        return createCohort(tx, tree);
    }

    @Override
    public final void close() {
        final State localState = state;

        do {
            Preconditions.checkState(!CLOSED.equals(localState), "Transaction chain {} has been closed", this);

            if (FAILED.equals(localState)) {
                LOG.debug("Ignoring user close in failed state");
                return;
            }
        } while (!STATE_UPDATER.compareAndSet(this, localState, CLOSED));
    }

    /**
     * Notify the base logic that a previously-submitted transaction has been committed successfully.
     *
     * @param transaction Transaction which completed successfully.
     */
    protected final void onTransactionCommited(final SnapshotBackedWriteTransaction<T> transaction) {
        // If the committed transaction was the one we allocated last,
        // we clear it and the ready snapshot, so the next transaction
        // allocated refers to the data tree directly.
        final State localState = state;

        if (!(localState instanceof Allocated)) {
            // This can legally happen if the chain is shut down before the transaction was committed
            // by the backend.
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

    /**
     * Notify the base logic that a previously-submitted transaction has failed.
     *
     * @param transaction Transaction which failed.
     * @param cause Failure cause
     */
    protected final void onTransactionFailed(final SnapshotBackedWriteTransaction<T> transaction, final Throwable cause) {
        LOG.debug("Transaction chain {} failed on transaction {}", this, transaction, cause);
        state = FAILED;
    }

    /**
     * Return the next transaction identifier.
     *
     * @return transaction identifier.
     */
    protected abstract T nextTransactionIdentifier();

    /**
     * Inquire as to whether transactions should record their allocation context.
     *
     * @return True if allocation context should be recorded.
     */
    protected abstract boolean getDebugTransactions();

    /**
     * Take a fresh {@link DataTreeSnapshot} from the backend.
     *
     * @return A new snapshot.
     */
    protected abstract DataTreeSnapshot takeSnapshot();

    /**
     * Create a cohort for driving the transaction through the commit process.
     *
     * @param transaction Transaction handle
     * @param modification {@link DataTreeModification} which needs to be applied to the backend
     * @return A {@link DOMStoreThreePhaseCommitCohort} cohort.
     */
    protected abstract DOMStoreThreePhaseCommitCohort createCohort(final SnapshotBackedWriteTransaction<T> transaction, final DataTreeModification modification);
}
