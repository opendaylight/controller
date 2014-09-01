/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.common.impl.service.AbstractDataTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.broker.impl.jmx.TransactionStatsTracker;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Read-Write Transaction, which is composed of several
 * {@link DOMStoreWriteTransaction} transactions. A sub-transaction is selected by
 * {@link LogicalDatastoreType} type parameter in:
 *
 * <ul>
 * <li>{@link #put(LogicalDatastoreType, YangInstanceIdentifier, NormalizedNode)}
 * <li>{@link #delete(LogicalDatastoreType, YangInstanceIdentifier)}
 * <li>{@link #merge(LogicalDatastoreType, YangInstanceIdentifier, NormalizedNode)}
 * </ul>
 * <p>
 * {@link #commit()} will result in invocation of
 * {@link DOMDataCommitImplementation#submit(org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction, Iterable)}
 * invocation with all {@link org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort} for underlying
 * transactions.
 *
 * @param <T> Subtype of {@link DOMStoreWriteTransaction} which is used as
 *            subtransaction.
 */
class DOMForwardedWriteTransaction<T extends DOMStoreWriteTransaction> extends
        AbstractDOMForwardedCompositeTransaction<LogicalDatastoreType, T> implements DOMDataWriteTransaction {
    @SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<DOMForwardedWriteTransaction, DOMDataCommitImplementation> IMPL_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(DOMForwardedWriteTransaction.class, DOMDataCommitImplementation.class, "commitImpl");
    @SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<DOMForwardedWriteTransaction, Future> FUTURE_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(DOMForwardedWriteTransaction.class, Future.class, "commitFuture");
    private static final Logger LOG = LoggerFactory.getLogger(DOMForwardedWriteTransaction.class);
    private static final Future<?> CANCELLED_FUTURE = Futures.immediateCancelledFuture();

    /**
     * Implementation of real commit. It also acts as an indication that
     * the transaction is running -- which we flip atomically using
     * {@link #IMPL_UPDATER}.
     */
    private volatile DOMDataCommitImplementation commitImpl;

    /**
     * Future task of transaction commit. It starts off as null, but is
     * set appropriately on {@link #submit()} and {@link #cancel()} via
     * {@link AtomicReferenceFieldUpdater#lazySet(Object, Object)}.
     *
     * Lazy set is safe for use because it is only referenced to in the
     * {@link #cancel()} slow path, where we will busy-wait for it. The
     * fast path gets the benefit of a store-store barrier instead of the
     * usual store-load barrier.
     */
    private volatile Future<?> commitFuture;

    /**
     * The following are local, unsynchronized stat counters. We update the shared txStatsTracker
     * on submit, 1) so we only incur a volatile write once for each stat, 2) so we only update the
     * stats if the commit was successful or wasn't cancelled.
     */
    private int successfulWritesCount;
    private int successfulDeletesCount;

    protected DOMForwardedWriteTransaction(final Object identifier,
            final Map<LogicalDatastoreType, T> backingTxs, final DOMDataCommitImplementation commitImpl,
            final TransactionStatsTracker txStatsTracker) {
        super(identifier, backingTxs, txStatsTracker);
        this.commitImpl = Preconditions.checkNotNull(commitImpl, "commitImpl must not be null.");
    }

    @Override
    public void put(final LogicalDatastoreType store, final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        checkRunning(commitImpl);

        try {
            getSubtransaction(store).write(path, data);
            successfulWritesCount++;
        } catch(RuntimeException e) {
            getTxStatsTracker().incrementFailedWrites();
            throw e;
        }
    }

    @Override
    public void delete(final LogicalDatastoreType store, final YangInstanceIdentifier path) {
        checkRunning(commitImpl);

        try {
            getSubtransaction(store).delete(path);
            successfulDeletesCount++;
        } catch(RuntimeException e) {
            getTxStatsTracker().incrementFailedDeletes();
            throw e;
        }
    }

    @Override
    public void merge(final LogicalDatastoreType store, final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        checkRunning(commitImpl);

        try {
            getSubtransaction(store).merge(path, data);
            successfulWritesCount++;
        } catch(RuntimeException e) {
            getTxStatsTracker().incrementFailedWrites();
            throw e;
        }
    }

    @Override
    public boolean cancel() {
        final DOMDataCommitImplementation impl = IMPL_UPDATER.getAndSet(this, null);
        if (impl != null) {
            LOG.trace("Transaction {} cancelled before submit", getIdentifier());
            FUTURE_UPDATER.lazySet(this, CANCELLED_FUTURE);
            return true;
        }

        // The transaction is in process of being submitted or cancelled. Busy-wait
        // for the corresponding future.
        Future<?> future;
        do {
            future = commitFuture;
        } while (future == null);

        return future.cancel(false);
    }

    @Override
    public ListenableFuture<RpcResult<TransactionStatus>> commit() {
        return AbstractDataTransaction.convertToLegacyCommitFuture(submit());
    }

    @Override
    public CheckedFuture<Void, TransactionCommitFailedException> submit() {
        final DOMDataCommitImplementation impl = IMPL_UPDATER.getAndSet(this, null);
        checkRunning(impl);

        final Collection<T> txns = getSubtransactions();
        final Collection<DOMStoreThreePhaseCommitCohort> cohorts = new ArrayList<>(txns.size());

        // FIXME: deal with errors thrown by backed (ready and submit can fail in theory)
        for (DOMStoreWriteTransaction txn : txns) {
            cohorts.add(txn.ready());
        }

        final CheckedFuture<Void, TransactionCommitFailedException> ret = impl.submit(this, cohorts);

        if(successfulWritesCount > 0 || successfulDeletesCount > 0) {
            // Add a callback to update the txStatsTracker counters.
            Futures.addCallback(ret, new SubmitFutureCallback(successfulWritesCount,
                    successfulDeletesCount, getTxStatsTracker()));
        }

        FUTURE_UPDATER.lazySet(this, ret);
        return ret;
    }

    private void checkRunning(final DOMDataCommitImplementation impl) {
        Preconditions.checkState(impl != null, "Transaction %s is no longer running", getIdentifier());
    }

    private static final class SubmitFutureCallback implements FutureCallback<Void> {
        private final int finalSuccessfulDeletesCount;
        private final TransactionStatsTracker finalTxStatsTracker;
        private final int finalSuccessfulWritesCount;

        private SubmitFutureCallback(int finalSuccessfulWritesCount, int finalSuccessfulDeletesCount,
                TransactionStatsTracker finalTxStatsTracker) {
            this.finalSuccessfulDeletesCount = finalSuccessfulDeletesCount;
            this.finalTxStatsTracker = finalTxStatsTracker;
            this.finalSuccessfulWritesCount = finalSuccessfulWritesCount;
        }

        @Override
        public void onSuccess(Void result) {
            finalTxStatsTracker.addSuccessfulWrites(finalSuccessfulWritesCount);
            finalTxStatsTracker.addSuccessfulDeletes(finalSuccessfulDeletesCount);
        }

        @Override
        public void onFailure(Throwable t) {
        }
    }
}
