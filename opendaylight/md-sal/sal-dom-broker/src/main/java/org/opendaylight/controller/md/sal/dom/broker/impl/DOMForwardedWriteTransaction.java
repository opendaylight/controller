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
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.common.impl.service.AbstractDataTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;

/**
 *
 *
 * Read-Write Transaction, which is composed of several
 * {@link DOMStoreWriteTransaction} transactions. Subtransaction is selected by
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
 * @param <T>
 *            Subtype of {@link DOMStoreWriteTransaction} which is used as
 *            subtransaction.
 */
class DOMForwardedWriteTransaction<T extends DOMStoreWriteTransaction> extends
        AbstractDOMForwardedCompositeTransaction<LogicalDatastoreType, T> implements DOMDataWriteTransaction {

    /**
     *  Implementation of real commit.
     *
     *  Transaction can not be commited if commitImpl is null,
     *  so this seting this property to null is also used to
     *  prevent write to
     *  already commited / canceled transaction {@link #checkNotCanceled()
     *
     *
     */
    @GuardedBy("this")
    private volatile DOMDataCommitImplementation commitImpl;

    /**
     *
     * Future task of transaction commit.
     *
     * This value is initially null, and is once updated if transaction
     * is commited {@link #commit()}.
     * If this future exists, transaction MUST not be commited again
     * and all modifications should fail. See {@link #checkNotCommited()}.
     *
     */
    @GuardedBy("this")
    private volatile CheckedFuture<Void, TransactionCommitFailedException> commitFuture;

    protected DOMForwardedWriteTransaction(final Object identifier,
            final ImmutableMap<LogicalDatastoreType, T> backingTxs, final DOMDataCommitImplementation commitImpl) {
        super(identifier, backingTxs);
        this.commitImpl = Preconditions.checkNotNull(commitImpl, "commitImpl must not be null.");
    }

    @Override
    public void put(final LogicalDatastoreType store, final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        checkNotReady();
        getSubtransaction(store).write(path, data);
    }

    @Override
    public void delete(final LogicalDatastoreType store, final YangInstanceIdentifier path) {
        checkNotReady();
        getSubtransaction(store).delete(path);
    }

    @Override
    public void merge(final LogicalDatastoreType store, final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        checkNotReady();
        getSubtransaction(store).merge(path, data);
    }

    @Override
    public synchronized boolean cancel() {
        // Transaction is already canceled, we are safe to return true
        final boolean cancelationResult;
        if (commitImpl == null && commitFuture != null) {
            // Transaction is submitted, we try to cancel future.
            cancelationResult = commitFuture.cancel(false);
        } else if(commitImpl == null) {
            return true;
        } else {
            cancelationResult = true;
            commitImpl = null;
        }
        return cancelationResult;

    }

    @Override
    public synchronized ListenableFuture<RpcResult<TransactionStatus>> commit() {
        return AbstractDataTransaction.convertToLegacyCommitFuture(submit());
    }

    @Override
    public CheckedFuture<Void,TransactionCommitFailedException> submit() {
        checkNotReady();

        ImmutableList.Builder<DOMStoreThreePhaseCommitCohort> cohortsBuilder = ImmutableList.builder();
        for (DOMStoreWriteTransaction subTx : getSubtransactions()) {
            cohortsBuilder.add(subTx.ready());
        }
        ImmutableList<DOMStoreThreePhaseCommitCohort> cohorts = cohortsBuilder.build();
        commitFuture = commitImpl.submit(this, cohorts);

        /*
         *We remove reference to Commit Implementation in order
         *to prevent memory leak
         */
        commitImpl = null;
        return commitFuture;
    }

    private void checkNotReady() {
        checkNotCommited();
        checkNotCanceled();
    }

    private void checkNotCanceled() {
        Preconditions.checkState(commitImpl != null, "Transaction was canceled.");
    }

    private void checkNotCommited() {
        checkState(commitFuture == null, "Transaction was already submited.");
    }
}