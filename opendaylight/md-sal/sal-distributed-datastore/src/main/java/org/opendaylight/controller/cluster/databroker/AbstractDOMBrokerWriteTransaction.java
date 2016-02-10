/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.databroker;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
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
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreTransactionFactory;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDOMBrokerWriteTransaction<T extends DOMStoreWriteTransaction>
        extends AbstractDOMBrokerTransaction<T> implements DOMDataWriteTransaction {

    @SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<AbstractDOMBrokerWriteTransaction, AbstractDOMTransactionFactory> IMPL_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(AbstractDOMBrokerWriteTransaction.class, AbstractDOMTransactionFactory.class, "commitImpl");
    @SuppressWarnings("rawtypes")
    private static final AtomicReferenceFieldUpdater<AbstractDOMBrokerWriteTransaction, Future> FUTURE_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(AbstractDOMBrokerWriteTransaction.class, Future.class, "commitFuture");
    private static final Logger LOG = LoggerFactory.getLogger(AbstractDOMBrokerWriteTransaction.class);
    private static final Future<?> CANCELLED_FUTURE = Futures.immediateCancelledFuture();

    /**
     * Implementation of real commit. It also acts as an indication that
     * the transaction is running -- which we flip atomically using
     * {@link #IMPL_UPDATER}.
     */
    private volatile AbstractDOMTransactionFactory<?> commitImpl;

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

    protected AbstractDOMBrokerWriteTransaction(final Object identifier,
                                            final Map<LogicalDatastoreType, ? extends DOMStoreTransactionFactory> storeTxFactories, final AbstractDOMTransactionFactory<?> commitImpl) {
        super(identifier, storeTxFactories);
        this.commitImpl = Preconditions.checkNotNull(commitImpl, "commitImpl must not be null.");
    }

    @Override
    public void put(final LogicalDatastoreType store, final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        checkRunning(commitImpl);
        checkInstanceIdentifierReferencesData(path,data);
        getSubtransaction(store).write(path, data);
    }

    private static void checkInstanceIdentifierReferencesData(final YangInstanceIdentifier path,
            final NormalizedNode<?, ?> data) {
        final PathArgument lastArg = path.getLastPathArgument();
        Preconditions.checkArgument(
                (lastArg == data.getIdentifier()) || (lastArg != null && lastArg.equals(data.getIdentifier())),
                "Instance identifier references %s but data identifier is %s", lastArg, data);
    }

    @Override
    public void delete(final LogicalDatastoreType store, final YangInstanceIdentifier path) {
        checkRunning(commitImpl);
        getSubtransaction(store).delete(path);
    }

    @Override
    public void merge(final LogicalDatastoreType store, final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        checkRunning(commitImpl);
        checkInstanceIdentifierReferencesData(path, data);
        getSubtransaction(store).merge(path, data);
    }

    @Override
    public boolean cancel() {
        final AbstractDOMTransactionFactory<?> impl = IMPL_UPDATER.getAndSet(this, null);
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

    @Deprecated
    @Override
    public ListenableFuture<RpcResult<TransactionStatus>> commit() {
        return AbstractDataTransaction.convertToLegacyCommitFuture(submit());
    }

    @Override
    public CheckedFuture<Void, TransactionCommitFailedException> submit() {
        final AbstractDOMTransactionFactory<?> impl = IMPL_UPDATER.getAndSet(this, null);
        checkRunning(impl);

        final Collection<T> txns = getSubtransactions();
        final Collection<DOMStoreThreePhaseCommitCohort> cohorts = new ArrayList<>(txns.size());

        // FIXME: deal with errors thrown by backed (ready and submit can fail in theory)
        for (final T txn : txns) {
            cohorts.add(txn.ready());
        }

        final CheckedFuture<Void, TransactionCommitFailedException> ret = impl.submit(this, cohorts);
        FUTURE_UPDATER.lazySet(this, ret);
        return ret;
    }

    private void checkRunning(final AbstractDOMTransactionFactory<?> impl) {
        Preconditions.checkState(impl != null, "Transaction %s is no longer running", getIdentifier());
    }

}
