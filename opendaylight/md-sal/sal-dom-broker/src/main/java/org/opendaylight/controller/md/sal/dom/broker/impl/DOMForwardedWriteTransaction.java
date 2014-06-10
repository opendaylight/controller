/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import static com.google.common.base.Preconditions.checkState;

import javax.annotation.concurrent.GuardedBy;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.InstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListenableFuture;

/**
 *
 *
 * Read-Write Transaction, which is composed of several
 * {@link DOMStoreWriteTransaction} transactions. Subtransaction is selected by
 * {@link LogicalDatastoreType} type parameter in:
 *
 * <ul>
 * <li>{@link #put(LogicalDatastoreType, InstanceIdentifier, NormalizedNode)}
 * <li>{@link #delete(LogicalDatastoreType, InstanceIdentifier)}
 * <li>{@link #merge(LogicalDatastoreType, InstanceIdentifier, NormalizedNode)}
 * </ul>
 * <p>
 * {@link #commit()} will result in invocation of
 * {@link DOMDataCommitImplementation#commit(org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction, Iterable)}
 * invocation with all {@link org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort} for underlying
 * transactions.
 *
 * @param <T>
 *            Subtype of {@link DOMStoreWriteTransaction} which is used as
 *            subtransaction.
 */
class DOMForwardedWriteTransaction<T extends DOMStoreWriteTransaction> extends
        AbstractDOMForwardedCompositeTransaction<LogicalDatastoreType, T> implements DOMDataWriteTransaction {

    @GuardedBy("this")
    private DOMDataCommitImplementation commitImpl;

    @GuardedBy("this")
    private boolean canceled;
    @GuardedBy("this")
    private ListenableFuture<RpcResult<TransactionStatus>> commitFuture;

    protected DOMForwardedWriteTransaction(final Object identifier,
            final ImmutableMap<LogicalDatastoreType, T> backingTxs, final DOMDataCommitImplementation commitImpl) {
        super(identifier, backingTxs);
        this.commitImpl = Preconditions.checkNotNull(commitImpl, "commitImpl must not be null.");
    }

    @Override
    public void put(final LogicalDatastoreType store, final InstanceIdentifier path, final NormalizedNode<?, ?> data) {
        checkNotReady();
        getSubtransaction(store).write(path, data);
    }

    @Override
    public void delete(final LogicalDatastoreType store, final InstanceIdentifier path) {
        checkNotReady();
        getSubtransaction(store).delete(path);
    }

    @Override
    public void merge(final LogicalDatastoreType store, final InstanceIdentifier path, final NormalizedNode<?, ?> data) {
        checkNotReady();
        getSubtransaction(store).merge(path, data);
    }

    @Override
    public synchronized void cancel() {
        checkState(!canceled, "Transaction was canceled.");
        if (commitFuture != null) {
            // FIXME: Implement cancelation of commit future
            // when Broker impl will support cancelation.
            throw new UnsupportedOperationException("Not implemented yet.");
        }
        canceled = true;
        commitImpl = null;

    }

    @Override
    public synchronized ListenableFuture<RpcResult<TransactionStatus>> commit() {
        checkNotReady();

        ImmutableList.Builder<DOMStoreThreePhaseCommitCohort> cohortsBuilder = ImmutableList.builder();
        for (DOMStoreWriteTransaction subTx : getSubtransactions()) {
            cohortsBuilder.add(subTx.ready());
        }
        ImmutableList<DOMStoreThreePhaseCommitCohort> cohorts = cohortsBuilder.build();
        commitFuture = commitImpl.commit(this, cohorts);
        return commitFuture;
    }

    private void checkNotReady() {
        checkNotCanceled();
        checkNotCommited();
    }

    private void checkNotCanceled() {
        Preconditions.checkState(!canceled, "Transaction was canceled.");
    }

    private void checkNotCommited() {
        checkState(commitFuture == null, "Transaction was already commited.");
    }

}