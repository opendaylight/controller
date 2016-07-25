/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.dispatch.ExecutionContexts;
import akka.dispatch.OnComplete;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.utils.PruningDataTreeModification;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateTip;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

final class SimpleShardDataTreeCohort extends ShardDataTreeCohort implements Identifiable<TransactionIdentifier> {
    enum State {
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
    private final CompositeDataTreeCohort userCohorts;

    private State state = State.READY;
    private DataTreeCandidateTip candidate;
    private FutureCallback<?> callback;
    private Exception nextFailure;

    SimpleShardDataTreeCohort(final ShardDataTree dataTree, final DataTreeModification transaction,
            final TransactionIdentifier transactionId, final CompositeDataTreeCohort userCohorts) {
        this.dataTree = Preconditions.checkNotNull(dataTree);
        this.transaction = Preconditions.checkNotNull(transaction);
        this.transactionId = Preconditions.checkNotNull(transactionId);
        this.userCohorts = Preconditions.checkNotNull(userCohorts);
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
    DataTreeModification getDataTreeModification() {
        DataTreeModification dataTreeModification = transaction;
        if (transaction instanceof PruningDataTreeModification){
            dataTreeModification = ((PruningDataTreeModification) transaction).getResultingModification();
        }
        return dataTreeModification;
    }

    @Override
    public void canCommit(final FutureCallback<Void> callback) {
        Preconditions.checkState(state == State.READY);
        this.callback = Preconditions.checkNotNull(callback);
        state = State.CAN_COMMIT_PENDING;
        dataTree.startCanCommit(this);
    }

    @Override
    public void preCommit(final FutureCallback<DataTreeCandidate> callback) {
        Preconditions.checkState(state == State.CAN_COMMIT_COMPLETE);
        this.callback = Preconditions.checkNotNull(callback);
        state = State.PRE_COMMIT_PENDING;

        if (nextFailure == null) {
            dataTree.startPreCommit(this);
        } else {
            failedPreCommit(nextFailure);
        }
    }

    @Override
    public ListenableFuture<Void> abort() {
        if (state == State.READY) {
            LOG.debug("Ready transaction {} aborted before commit started", transactionId);
            return VOID_FUTURE;
        }

        dataTree.startAbort(this);
        state = State.ABORTED;

        final Optional<Future<Iterable<Object>>> maybeAborts = userCohorts.abort();
        if (!maybeAborts.isPresent()) {
            return VOID_FUTURE;
        }

        final Future<Iterable<Object>> aborts = maybeAborts.get();
        if (aborts.isCompleted()) {
            return VOID_FUTURE;
        }

        final SettableFuture<Void> ret = SettableFuture.create();
        aborts.onComplete(new OnComplete<Iterable<Object>>() {
            @Override
            public void onComplete(final Throwable arg0, final Iterable<Object> arg1) {
                if (arg0 != null) {
                    ret.setException(arg0);
                } else {
                    ret.set(null);
                }
            }
        }, ExecutionContexts.global());

        return ret;
    }

    @Override
    public void commit(final FutureCallback<UnsignedLong> callback) {
        Preconditions.checkState(state == State.PRE_COMMIT_COMPLETE);
        this.callback = Preconditions.checkNotNull(callback);
        state = State.COMMIT_PENDING;
        dataTree.startCommit(this, candidate);
    }

    private <T> FutureCallback<T> switchState(final State newState) {
        @SuppressWarnings("unchecked")
        final FutureCallback<T> ret = (FutureCallback<T>) this.callback;
        this.callback = null;
        LOG.debug("Transaction {} changing state from {} to {}", transactionId, state, newState);
        this.state = newState;
        return ret;
    }

    void successfulCanCommit() {
        switchState(State.CAN_COMMIT_COMPLETE).onSuccess(null);
    }

    void failedCanCommit(final Exception cause) {
        switchState(State.FAILED).onFailure(cause);
    }

    /**
     * Run user-defined canCommit and preCommit hooks. We want to run these before we initiate persistence so that
     * any failure to validate is propagated before we record the transaction.
     *
     * @param candidate {@link DataTreeCandidate} under consideration
     * @throws ExecutionException
     * @throws TimeoutException
     */
    // FIXME: this should be asynchronous
    void userPreCommit(final DataTreeCandidate candidate) throws ExecutionException, TimeoutException {
        userCohorts.canCommit(candidate);
        userCohorts.preCommit();
    }

    void successfulPreCommit(final DataTreeCandidateTip candidate) {
        LOG.trace("Transaction {} prepared candidate {}", transaction, candidate);
        this.candidate = Verify.verifyNotNull(candidate);
        switchState(State.PRE_COMMIT_COMPLETE).onSuccess(candidate);
    }

    void failedPreCommit(final Exception cause) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Transaction {} failed to prepare", transaction, cause);
        } else {
            LOG.error("Transaction failed to prepare", cause);
        }

        userCohorts.abort();
        switchState(State.FAILED).onFailure(cause);
    }

    void successfulCommit(final UnsignedLong journalIndex) {
        try {
            userCohorts.commit();
        } catch (TimeoutException | ExecutionException e) {
            // We are probably dead, depending on what the cohorts end up doing
            LOG.error("User cohorts failed to commit", e);
        }

        switchState(State.COMMITTED).onSuccess(journalIndex);
    }

    void failedCommit(final Exception cause) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Transaction {} failed to commit", transaction, cause);
        } else {
            LOG.error("Transaction failed to commit", cause);
        }

        userCohorts.abort();
        switchState(State.FAILED).onFailure(cause);
    }

    State getState() {
        return state;
    }

    void reportFailure(final Exception cause) {
        this.nextFailure = Preconditions.checkNotNull(cause);
    }
}
