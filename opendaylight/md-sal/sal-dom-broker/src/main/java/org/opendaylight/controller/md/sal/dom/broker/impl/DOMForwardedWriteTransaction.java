/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import com.google.common.base.Supplier;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Read-Write Transaction, which is composed of several
 * {@link DOMStoreWriteTransaction} transactions. A sub-transaction is selected by
 * {@link LogicalDatastoreType} type parameter in:
 *
 * <p>
 * <ul>
 * <li>{@link #put(LogicalDatastoreType, YangInstanceIdentifier, NormalizedNode)}
 * <li>{@link #delete(LogicalDatastoreType, YangInstanceIdentifier)}
 * <li>{@link #merge(LogicalDatastoreType, YangInstanceIdentifier, NormalizedNode)}
 * </ul>
 *
 * <p>
 * {@link #commit()} will result in invocation of
 * {@link DOMDataCommitImplementation#submit(org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction,
 * Iterable)} invocation with all {@link org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort}
 * for underlying transactions.
 *
 * @param <T> Subtype of {@link DOMStoreWriteTransaction} which is used as
 *            subtransaction.
 */
@Deprecated
class DOMForwardedWriteTransaction<T extends DOMStoreWriteTransaction> extends
        AbstractDOMForwardedCompositeTransaction<LogicalDatastoreType, T> implements DOMDataWriteTransaction {
    @SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<DOMForwardedWriteTransaction,
            AbstractDOMForwardedTransactionFactory>
            IMPL_UPDATER = AtomicReferenceFieldUpdater
            .newUpdater(DOMForwardedWriteTransaction.class, AbstractDOMForwardedTransactionFactory.class, "commitImpl");
    @SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<DOMForwardedWriteTransaction, Future> FUTURE_UPDATER
            = AtomicReferenceFieldUpdater.newUpdater(DOMForwardedWriteTransaction.class, Future.class, "commitFuture");
    private static final Logger LOG = LoggerFactory.getLogger(DOMForwardedWriteTransaction.class);
    private static final Future<?> CANCELLED_FUTURE = Futures.immediateCancelledFuture();

    /**
     * Implementation of real commit. It also acts as an indication that
     * the transaction is running -- which we flip atomically using
     * {@link #IMPL_UPDATER}.
     */
    private volatile AbstractDOMForwardedTransactionFactory<?> commitImpl;

    /**
     * Future task of transaction commit. It starts off as null, but is
     * set appropriately on {@link #commit()} and {@link #cancel()} via
     * {@link AtomicReferenceFieldUpdater#lazySet(Object, Object)}.
     *
     * <p>
     * Lazy set is safe for use because it is only referenced to in the
     * {@link #cancel()} slow path, where we will busy-wait for it. The
     * fast path gets the benefit of a store-store barrier instead of the
     * usual store-load barrier.
     */
    private volatile Future<?> commitFuture;

    protected DOMForwardedWriteTransaction(final Object identifier, final Map<LogicalDatastoreType, T> backingTxs,
                                           final AbstractDOMForwardedTransactionFactory<?> commitImpl) {
        super(identifier, backingTxs);
        this.commitImpl = requireNonNull(commitImpl, "commitImpl must not be null.");
    }

    @Override
    public void put(final LogicalDatastoreType store, final YangInstanceIdentifier path,
                    final NormalizedNode<?, ?> data) {
        checkRunning(commitImpl);
        getSubtransaction(store).write(path, data);
    }

    @Override
    public void delete(final LogicalDatastoreType store, final YangInstanceIdentifier path) {
        checkRunning(commitImpl);
        getSubtransaction(store).delete(path);
    }

    @Override
    public void merge(final LogicalDatastoreType store, final YangInstanceIdentifier path,
                      final NormalizedNode<?, ?> data) {
        checkRunning(commitImpl);
        getSubtransaction(store).merge(path, data);
    }

    @Override
    public boolean cancel() {
        final AbstractDOMForwardedTransactionFactory<?> impl = IMPL_UPDATER.getAndSet(this, null);
        if (impl != null) {
            LOG.trace("Transaction {} cancelled before submit", getIdentifier());
            FUTURE_UPDATER.lazySet(this, CANCELLED_FUTURE);
            closeSubtransactions();
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
    public FluentFuture<? extends CommitInfo> commit() {
        return FluentFuture.from(doCommit(CommitInfo::empty));
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private <V> ListenableFuture<V> doCommit(final Supplier<V> futureValueSupplier) {
        final AbstractDOMForwardedTransactionFactory<?> impl = IMPL_UPDATER.getAndSet(this, null);
        checkRunning(impl);

        final Collection<T> txns = getSubtransactions();
        final Collection<DOMStoreThreePhaseCommitCohort> cohorts = new ArrayList<>(txns.size());

        ListenableFuture<V> ret;
        try {
            for (DOMStoreWriteTransaction txn : txns) {
                cohorts.add(txn.ready());
            }

            ret = impl.commit(this, cohorts, futureValueSupplier);
        } catch (RuntimeException e) {
            ret = FluentFuture.from(Futures.immediateFailedFuture(
                    TransactionCommitFailedExceptionMapper.COMMIT_ERROR_MAPPER.apply(e)));
        }
        FUTURE_UPDATER.lazySet(this, ret);
        return ret;
    }

    private void checkRunning(final AbstractDOMForwardedTransactionFactory<?> impl) {
        checkState(impl != null, "Transaction %s is no longer running", getIdentifier());
    }
}
