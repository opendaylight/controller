/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.utils.PruningDataTreeModification;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateTip;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SimpleShardDataTreeCohort extends ShardDataTreeCohort implements Identifiable<TransactionIdentifier> {
    private enum State {
        READY,
        CAN_COMMIT_PENDING,
        CAN_COMMIT_COMPLETE,
        PRE_COMMIT_PENDING,
        PRE_COMMIT_COMPLETE,
        COMMIT_PENDING,

        ABORTED,
        COMMITTED,
        FAILED,
    }

    private static final Logger LOG = LoggerFactory.getLogger(SimpleShardDataTreeCohort.class);
    private static final ListenableFuture<Void> VOID_FUTURE = Futures.immediateFuture(null);
    private final DataTreeModification transaction;
    private final ShardDataTree dataTree;
    private final TransactionIdentifier transactionId;

    private State state = State.READY;
    private DataTreeCandidateTip candidate;
    private FutureCallback<?> callback;

    SimpleShardDataTreeCohort(final ShardDataTree dataTree, final DataTreeModification transaction,
            final TransactionIdentifier transactionId) {
        this.dataTree = Preconditions.checkNotNull(dataTree);
        this.transaction = Preconditions.checkNotNull(transaction);
        this.transactionId = Preconditions.checkNotNull(transactionId);
    }

    @Override
    public TransactionIdentifier getIdentifier() {
        return transactionId;
    }

    @Override
    DataTreeCandidateTip getCandidate() {
        return candidate;
    }

    @Override
    public void canCommit(final FutureCallback<Void> callback) {
        Preconditions.checkState(state == State.READY);
        this.callback = Preconditions.checkNotNull(callback);
        state = State.CAN_COMMIT_PENDING;
        dataTree.startCanCommit(this);
    }

    @Override
    public void preCommit(final FutureCallback<DataTreeCandidateTip> callback) {
        Preconditions.checkState(state == State.CAN_COMMIT_COMPLETE);
        this.callback = Preconditions.checkNotNull(callback);
        state = State.PRE_COMMIT_PENDING;
        dataTree.startPreCommit(this);
    }

    @Override
    DataTreeModification getDataTreeModification() {
        DataTreeModification dataTreeModification = transaction;
        if(transaction instanceof PruningDataTreeModification){
            dataTreeModification = ((PruningDataTreeModification) transaction).getResultingModification();
        }
        return dataTreeModification;
    }

    @Override
    public ListenableFuture<Void> abort() {
        // No-op, really
        return VOID_FUTURE;
    }

    @Override
    public ListenableFuture<Void> commit() {
        try {
            dataTree.getDataTree().commit(candidate);
        } catch (Exception e) {
            if(LOG.isTraceEnabled()) {
                LOG.trace("Transaction {} failed to commit", transaction, e);
            } else {
                LOG.error("Transaction failed to commit", e);
            }
            return Futures.immediateFailedFuture(e);
        }

        LOG.trace("Transaction {} committed, proceeding to notify", transaction);
        dataTree.notifyListeners(candidate);
        return VOID_FUTURE;
    }

    private <T> FutureCallback<T> switchState(final State newState) {
        @SuppressWarnings("unchecked")
        final FutureCallback<T> ret = (FutureCallback<T>) this.callback;
        this.callback = null;
        LOG.debug("Transaction {} changing state from {} to {}", state, newState);
        this.state = newState;
        return ret;
    }

    void successfulCanCommit() {
        switchState(State.CAN_COMMIT_COMPLETE).onSuccess(null);
    }

    void failedCanCommit(final Exception cause) {
        switchState(State.FAILED).onFailure(cause);
    }

    void successfulPreCommit(final DataTreeCandidateTip candidate) {
        LOG.trace("Transaction {} prepared candidate {}", transaction, candidate);

        switchState(State.PRE_COMMIT_COMPLETE).onSuccess(candidate);
    }

    void failedPreCommit(final Exception cause) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Transaction {} failed to prepare", transaction, cause);
        } else {
            LOG.error("Transaction failed to prepare", cause);
        }

        switchState(State.FAILED).onFailure(cause);
    }
}
