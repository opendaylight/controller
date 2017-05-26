/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.controller.md.sal.dom.spi.ForwardingDOMDataReadWriteTransaction;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of {@link DOMTransactionChain}, which has a very specific
 * behavior, which some users may find surprising. If keeps the general
 * intent of the contract, but it makes sure there are never more than two
 * transactions allocated at any given time: one of them is being committed,
 * and while that is happening, the other one acts as the scratch pad. Once
 * the committing transaction completes successfully, the scratch transaction
 * is enqueued as soon as it is ready.
 *
 * This mode of operation means that there is no inherent isolation between
 * the front-end transactions and transactions cannot be reasonably cancelled.
 *
 * It furthermore means that the transactions returned by {@link #newReadOnlyTransaction()}
 * counts as an outstanding transaction and the user may not allocate multiple
 * read-only transactions at the same time.
 */
public final class PingPongTransactionChain implements DOMTransactionChain {
    private static final Logger LOG = LoggerFactory.getLogger(PingPongTransactionChain.class);
    private final TransactionChainListener listener;
    private final DOMTransactionChain delegate;

    @GuardedBy("this")
    private boolean failed;
    @GuardedBy("this")
    private PingPongTransaction shutdownTx;
    @GuardedBy("this")
    private Entry<PingPongTransaction, Throwable> deadTx;

    /**
     * This updater is used to manipulate the "ready" transaction. We perform only atomic
     * get-and-set on it.
     */
    private static final AtomicReferenceFieldUpdater<PingPongTransactionChain, PingPongTransaction> READY_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(PingPongTransactionChain.class, PingPongTransaction.class, "readyTx");
    private volatile PingPongTransaction readyTx;

    /**
     * This updater is used to manipulate the "locked" transaction. A locked transaction
     * means we know that the user still holds a transaction and should at some point call
     * us. We perform on compare-and-swap to ensure we properly detect when a user is
     * attempting to allocated multiple transactions concurrently.
     */
    private static final AtomicReferenceFieldUpdater<PingPongTransactionChain, PingPongTransaction> LOCKED_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(PingPongTransactionChain.class, PingPongTransaction.class, "lockedTx");
    private volatile PingPongTransaction lockedTx;

    /**
     * This updater is used to manipulate the "inflight" transaction. There can be at most
     * one of these at any given time. We perform only compare-and-swap on these.
     */
    private static final AtomicReferenceFieldUpdater<PingPongTransactionChain, PingPongTransaction> INFLIGHT_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(PingPongTransactionChain.class, PingPongTransaction.class, "inflightTx");
    private volatile PingPongTransaction inflightTx;

    PingPongTransactionChain(final DOMDataBroker broker, final TransactionChainListener listener) {
        this.listener = Preconditions.checkNotNull(listener);
        this.delegate = broker.createTransactionChain(new TransactionChainListener() {
            @Override
            public void onTransactionChainFailed(final TransactionChain<?, ?> chain,
                    final AsyncTransaction<?, ?> transaction, final Throwable cause) {
                LOG.debug("Transaction chain {} reported failure in {}", chain, transaction, cause);
                delegateFailed(chain, cause);
            }

            @Override
            public void onTransactionChainSuccessful(final TransactionChain<?, ?> chain) {
                delegateSuccessful(chain);
            }
        });
    }

    void delegateSuccessful(final TransactionChain<?, ?> chain) {
        final Entry<PingPongTransaction, Throwable> canceled;
        synchronized (this) {
            // This looks weird, but we need not hold the lock while invoking callbacks
            canceled = deadTx;
        }

        if (canceled == null) {
            listener.onTransactionChainSuccessful(this);
            return;
        }

        // Backend shutdown successful, but we have a batch of transactions we have to report as dead due to the
        // user calling cancel().
        final PingPongTransaction tx = canceled.getKey();
        final Throwable cause = canceled.getValue();
        LOG.debug("Transaction chain {} successful, failing cancelled transaction {}", chain, tx, cause);

        listener.onTransactionChainFailed(this, tx.getFrontendTransaction(), cause);
        tx.onFailure(cause);
    }

    void delegateFailed(final TransactionChain<?, ?> chain, final Throwable cause) {

        final DOMDataReadWriteTransaction frontend;
        final PingPongTransaction tx = inflightTx;
        if (tx == null) {
            LOG.warn("Transaction chain {} failed with no pending transactions", chain);
            frontend = null;
        } else {
            frontend = tx.getFrontendTransaction();
        }

        listener.onTransactionChainFailed(this, frontend, cause);

        synchronized (this) {
            failed = true;

            /*
             * If we do not have a locked transaction, we need to ensure that
             * the backend transaction is cancelled. Otherwise we can defer
             * until the user calls us.
             */
            if (lockedTx == null) {
                processIfReady();
            }
        }
    }

    private synchronized PingPongTransaction slowAllocateTransaction() {
        Preconditions.checkState(shutdownTx == null, "Transaction chain %s has been shut down", this);

        if (deadTx != null) {
            throw new IllegalStateException(String.format(
                "Transaction chain %s has failed due to transaction %s being canceled", this, deadTx.getKey()),
                deadTx.getValue());
        }

        final DOMDataReadWriteTransaction delegateTx = delegate.newReadWriteTransaction();
        final PingPongTransaction newTx = new PingPongTransaction(delegateTx);

        if (!LOCKED_UPDATER.compareAndSet(this, null, newTx)) {
            delegateTx.cancel();
            throw new IllegalStateException(String.format("New transaction %s raced with transacion %s", newTx, lockedTx));
        }

        return newTx;
    }

    private PingPongTransaction allocateTransaction() {
        // Step 1: acquire current state
        final PingPongTransaction oldTx = READY_UPDATER.getAndSet(this, null);

        // Slow path: allocate a delegate transaction
        if (oldTx == null) {
            return slowAllocateTransaction();
        }

        // Fast path: reuse current transaction. We will check failures and similar on submit().
        if (!LOCKED_UPDATER.compareAndSet(this, null, oldTx)) {
            // Ouch. Delegate chain has not detected a duplicate transaction allocation. This is the best we can do.
            oldTx.getTransaction().cancel();
            throw new IllegalStateException(String.format("Reusable transaction %s raced with transaction %s", oldTx, lockedTx));
        }

        return oldTx;
    }

    /*
     * This forces allocateTransaction() on a slow path, which has to happen after
     * this method has completed executing. Also inflightTx may be updated outside
     * the lock, hence we need to re-check.
     */
    @GuardedBy("this")
    private void processIfReady() {
        if (inflightTx == null) {
            final PingPongTransaction tx = READY_UPDATER.getAndSet(this, null);
            if (tx != null) {
                processTransaction(tx);
            }
        }
    }

    /**
     * Process a ready transaction. The caller needs to ensure that
     * each transaction is seen only once by this method.
     *
     * @param tx Transaction which needs processing.
     */
    @GuardedBy("this")
    private void processTransaction(@Nonnull final PingPongTransaction tx) {
        if (failed) {
            LOG.debug("Cancelling transaction {}", tx);
            tx.getTransaction().cancel();
            return;
        }

        LOG.debug("Submitting transaction {}", tx);
        if (!INFLIGHT_UPDATER.compareAndSet(this, null, tx)) {
            LOG.warn("Submitting transaction {} while {} is still running", tx, inflightTx);
        }

        Futures.addCallback(tx.getTransaction().submit(), new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                transactionSuccessful(tx, result);
            }

            @Override
            public void onFailure(final Throwable t) {
                transactionFailed(tx, t);
            }
        });
    }

    /*
     * We got invoked from the data store thread. We need to do two things:
     * 1) release the in-flight transaction
     * 2) process the potential next transaction
     *
     * We have to perform 2) under lock. We could perform 1) without locking, but that means the CAS result may
     * not be accurate, as a user thread may submit the ready transaction before we acquire the lock -- and checking
     * for next transaction is not enough, as that may have also be allocated (as a result of a quick
     * submit/allocate/submit between 1) and 2)). Hence we'd end up doing the following:
     * 1) CAS of inflightTx
     * 2) take lock
     * 3) volatile read of inflightTx
     *
     * Rather than doing that, we keep this method synchronized, hence performing only:
     * 1) take lock
     * 2) CAS of inflightTx
     *
     * Since the user thread is barred from submitting the transaction (in processIfReady), we can then proceed with
     * the knowledge that inflightTx is null -- processTransaction() will still do a CAS, but that is only for
     * correctness.
     */
    private synchronized void processNextTransaction(final PingPongTransaction tx) {
        final boolean success = INFLIGHT_UPDATER.compareAndSet(this, tx, null);
        Preconditions.checkState(success, "Completed transaction %s while %s was submitted", tx, inflightTx);

        final PingPongTransaction nextTx = READY_UPDATER.getAndSet(this, null);
        if (nextTx != null) {
            processTransaction(nextTx);
        } else if (shutdownTx != null) {
            processTransaction(shutdownTx);
            delegate.close();
            shutdownTx = null;
        }
    }

    void transactionSuccessful(final PingPongTransaction tx, final Void result) {
        LOG.debug("Transaction {} completed successfully", tx);

        tx.onSuccess(result);
        processNextTransaction(tx);
    }

    void transactionFailed(final PingPongTransaction tx, final Throwable t) {
        LOG.debug("Transaction {} failed", tx, t);

        tx.onFailure(t);
        processNextTransaction(tx);
    }

    void readyTransaction(@Nonnull final PingPongTransaction tx) {
        // First mark the transaction as not locked.
        final boolean lockedMatch = LOCKED_UPDATER.compareAndSet(this, tx, null);
        Preconditions.checkState(lockedMatch, "Attempted to submit transaction %s while we have %s", tx, lockedTx);
        LOG.debug("Transaction {} unlocked", tx);

        /*
         * The transaction is ready. It will then be picked up by either next allocation,
         * or a background transaction completion callback.
         */
        final boolean success = READY_UPDATER.compareAndSet(this, null, tx);
        Preconditions.checkState(success, "Transaction %s collided on ready state", tx, readyTx);
        LOG.debug("Transaction {} readied", tx);

        /*
         * We do not see a transaction being in-flight, so we need to take care of dispatching
         * the transaction to the backend. We are in the ready case, we cannot short-cut
         * the checking of readyTx, as an in-flight transaction may have completed between us
         * setting the field above and us checking.
         */
        if (inflightTx == null) {
            synchronized (this) {
                processIfReady();
            }
        }
    }

    /**
     * Transaction cancellation is a heavyweight operation. We only support cancelation of a locked transaction
     * and return false for everything else. Cancelling such a transaction will result in all transactions in the
     * batch to be cancelled.
     *
     * @param tx Backend shared transaction
     * @param frontendTx
     * @param isOpen indicator whether the transaction was already closed
     * @return True if cancellation succeeded, false otherwise
     */
    synchronized void cancelTransaction(final PingPongTransaction tx, final DOMDataReadWriteTransaction frontendTx) {
        // Attempt to unlock the operation.
        final boolean lockedMatch = LOCKED_UPDATER.compareAndSet(this, tx, null);
        Verify.verify(lockedMatch, "Cancelling transaction {} collided with locked transaction {}", tx, lockedTx);

        // Cancel the backend transaction, so we do not end up leaking it.
        final boolean backendCancelled = tx.getTransaction().cancel();

        if (failed) {
            // The transaction has failed, this is probably the user just clearing up the transaction they had. We have
            // already cancelled the transaction anyway,
            return;
        } else if (!backendCancelled) {
            LOG.warn("Backend transaction cannot be cancelled during cancellation of {}, attempting to continue", tx);
        }

        // We have dealt with canceling the backend transaction and have unlocked the transaction. Since we are still
        // inside the synchronized block, any allocations are blocking on the slow path. Now we have to decide the fate
        // of this transaction chain.
        //
        // If there are no other frontend transactions in this batch we are aligned with backend state and we can
        // continue processing.
        if (frontendTx.equals(tx.getFrontendTransaction())) {
            LOG.debug("Cancelled transaction {} was head of the batch, resuming processing", tx);
            return;
        }

        // There are multiple frontend transactions in this batch. We have to report them as failed, which dooms this
        // transaction chain, too. Since we just came off of a locked transaction, we do not have a ready transaction
        // at the moment, but there may be some transaction in-flight. So we proceed to shutdown the backend chain
        // and mark the fact that we should be turning its completion into a failure.
        deadTx = new SimpleImmutableEntry<>(tx,
                new CancellationException("Transaction " + frontendTx + " canceled").fillInStackTrace());
        delegate.close();
    }

    @Override
    public synchronized void close() {
        final PingPongTransaction notLocked = lockedTx;
        Preconditions.checkState(notLocked == null, "Attempted to close chain with outstanding transaction %s", notLocked);

        // This is not reliable, but if we observe it to be null and the process has already completed,
        // the backend transaction chain will throw the appropriate error.
        Preconditions.checkState(shutdownTx == null, "Attempted to close an already-closed chain");

        // This may be a reaction to our failure callback, in that case the backend is already shutdown
        if (deadTx != null) {
            LOG.debug("Delegate {} is already closed due to failure {}", delegate, deadTx);
            return;
        }

        // Force allocations on slow path, picking up a potentially-outstanding transaction
        final PingPongTransaction tx = READY_UPDATER.getAndSet(this, null);

        if (tx != null) {
            // We have one more transaction, which needs to be processed somewhere. If we do not
            // a transaction in-flight, we need to push it down ourselves.
            // If there is an in-flight transaction we will schedule this last one into a dedicated
            // slot. Allocation slow path will check its presence and fail, the in-flight path will
            // pick it up, submit and immediately close the chain.
            if (inflightTx == null) {
                processTransaction(tx);
                delegate.close();
            } else {
                shutdownTx = tx;
            }
        } else {
            // Nothing outstanding, we can safely shutdown
            delegate.close();
        }
    }

    @Override
    public DOMDataReadOnlyTransaction newReadOnlyTransaction() {
        final PingPongTransaction tx = allocateTransaction();

        return new DOMDataReadOnlyTransaction() {
            @Override
            public CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> read(final LogicalDatastoreType store,
                    final YangInstanceIdentifier path) {
                return tx.getTransaction().read(store, path);
            }

            @Override
            public CheckedFuture<Boolean, ReadFailedException> exists(final LogicalDatastoreType store,
                    final YangInstanceIdentifier path) {
                return tx.getTransaction().exists(store, path);
            }

            @Override
            public Object getIdentifier() {
                return tx.getTransaction().getIdentifier();
            }

            @Override
            public void close() {
                readyTransaction(tx);
            }
        };
    }

    @Override
    public DOMDataReadWriteTransaction newReadWriteTransaction() {
        final PingPongTransaction tx = allocateTransaction();
        final DOMDataReadWriteTransaction ret = new ForwardingDOMDataReadWriteTransaction() {
            private boolean isOpen = true;

            @Override
            protected DOMDataReadWriteTransaction delegate() {
                return tx.getTransaction();
            }

            @Override
            public CheckedFuture<Void, TransactionCommitFailedException> submit() {
                readyTransaction(tx);
                isOpen = false;
                return tx.getSubmitFuture();
            }

            @Deprecated
            @Override
            public ListenableFuture<RpcResult<TransactionStatus>> commit() {
                readyTransaction(tx);
                isOpen = false;
                return tx.getCommitFuture();
            }

            @Override
            public boolean cancel() {
                if (isOpen) {
                    cancelTransaction(tx, this);
                    isOpen = false;
                    return true;
                } else {
                    return false;
                }
            }
        };

        tx.recordFrontendTransaction(ret);
        return ret;
    }

    @Override
    public DOMDataWriteTransaction newWriteOnlyTransaction() {
        return newReadWriteTransaction();
    }
}
