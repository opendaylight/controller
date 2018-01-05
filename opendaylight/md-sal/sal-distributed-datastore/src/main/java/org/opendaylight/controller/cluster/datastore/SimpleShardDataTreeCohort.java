/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.FutureCallback;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateTip;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SimpleShardDataTreeCohort extends ShardDataTreeCohort {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleShardDataTreeCohort.class);

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

    SimpleShardDataTreeCohort(final ShardDataTree dataTree, final DataTreeModification transaction,
        final TransactionIdentifier transactionId, final Exception nextFailure) {
        this.dataTree = Preconditions.checkNotNull(dataTree);
        this.transaction = Preconditions.checkNotNull(transaction);
        this.transactionId = Preconditions.checkNotNull(transactionId);
        this.userCohorts = null;
        this.nextFailure = Preconditions.checkNotNull(nextFailure);
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
        return transaction;
    }

    private void checkState(final State expected) {
        Preconditions.checkState(state == expected, "State %s does not match expected state %s", state, expected);
    }

    @Override
    public void canCommit(final FutureCallback<Void> newCallback) {
        if (state == State.CAN_COMMIT_PENDING) {
            return;
        }

        checkState(State.READY);
        this.callback = Preconditions.checkNotNull(newCallback);
        state = State.CAN_COMMIT_PENDING;

        if (nextFailure == null) {
            dataTree.startCanCommit(this);
        } else {
            failedCanCommit(nextFailure);
        }
    }

    @Override
    public void preCommit(final FutureCallback<DataTreeCandidate> newCallback) {
        checkState(State.CAN_COMMIT_COMPLETE);
        this.callback = Preconditions.checkNotNull(newCallback);
        state = State.PRE_COMMIT_PENDING;

        if (nextFailure == null) {
            dataTree.startPreCommit(this);
        } else {
            failedPreCommit(nextFailure);
        }
    }

    @Override
    public void abort(final FutureCallback<Void> abortCallback) {
        if (!dataTree.startAbort(this)) {
            abortCallback.onSuccess(null);
            return;
        }

        candidate = null;
        state = State.ABORTED;

        final Optional<CompletionStage<?>> maybeAborts = userCohorts.abort();
        if (!maybeAborts.isPresent()) {
            abortCallback.onSuccess(null);
            return;
        }

        maybeAborts.get().whenComplete((noop, failure) -> {
            if (failure != null) {
                abortCallback.onFailure(failure);
            } else {
                abortCallback.onSuccess(null);
            }
        });
    }

    @Override
    public void commit(final FutureCallback<UnsignedLong> newCallback) {
        checkState(State.PRE_COMMIT_COMPLETE);
        this.callback = Preconditions.checkNotNull(newCallback);
        state = State.COMMIT_PENDING;

        if (nextFailure == null) {
            dataTree.startCommit(this, candidate);
        } else {
            failedCommit(nextFailure);
        }
    }

    private <T> FutureCallback<T> switchState(final State newState) {
        @SuppressWarnings("unchecked")
        final FutureCallback<T> ret = (FutureCallback<T>) this.callback;
        this.callback = null;
        LOG.debug("Transaction {} changing state from {} to {}", transactionId, state, newState);
        this.state = newState;
        return ret;
    }

    void setNewCandidate(final DataTreeCandidateTip dataTreeCandidate) {
        checkState(State.PRE_COMMIT_COMPLETE);
        this.candidate = Verify.verifyNotNull(dataTreeCandidate);
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
     * @param dataTreeCandidate {@link DataTreeCandidate} under consideration
     * @param futureCallback the callback to invoke on completion, which may be immediate or async.
     */
    void userPreCommit(final DataTreeCandidate dataTreeCandidate, final FutureCallback<Void> futureCallback) {
        userCohorts.reset();

        final Optional<CompletionStage<Void>> maybeCanCommitFuture = userCohorts.canCommit(dataTreeCandidate);
        if (!maybeCanCommitFuture.isPresent()) {
            doUserPreCommit(futureCallback);
            return;
        }

        maybeCanCommitFuture.get().whenComplete((noop, failure) -> {
            if (failure != null) {
                futureCallback.onFailure(failure);
            } else {
                doUserPreCommit(futureCallback);
            }
        });
    }

    private void doUserPreCommit(final FutureCallback<Void> futureCallback) {
        final Optional<CompletionStage<Void>> maybePreCommitFuture = userCohorts.preCommit();
        if (!maybePreCommitFuture.isPresent()) {
            futureCallback.onSuccess(null);
            return;
        }

        maybePreCommitFuture.get().whenComplete((noop, failure) -> {
            if (failure != null) {
                futureCallback.onFailure(failure);
            } else {
                futureCallback.onSuccess(null);
            }
        });
    }

    void successfulPreCommit(final DataTreeCandidateTip dataTreeCandidate) {
        LOG.trace("Transaction {} prepared candidate {}", transaction, dataTreeCandidate);
        this.candidate = Verify.verifyNotNull(dataTreeCandidate);
        switchState(State.PRE_COMMIT_COMPLETE).onSuccess(dataTreeCandidate);
    }

    void failedPreCommit(final Throwable cause) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Transaction {} failed to prepare", transaction, cause);
        } else {
            LOG.error("Transaction {} failed to prepare", transactionId, cause);
        }

        userCohorts.abort();
        switchState(State.FAILED).onFailure(cause);
    }

    void successfulCommit(final UnsignedLong journalIndex, final Runnable onComplete) {
        final Optional<CompletionStage<Void>> maybeCommitFuture = userCohorts.commit();
        if (!maybeCommitFuture.isPresent()) {
            finishSuccessfulCommit(journalIndex, onComplete);
            return;
        }

        maybeCommitFuture.get().whenComplete((noop, failure) -> {
            if (failure != null) {
                LOG.error("User cohorts failed to commit", failure);
            }

            finishSuccessfulCommit(journalIndex, onComplete);
        });
    }

    private void finishSuccessfulCommit(final UnsignedLong journalIndex, final Runnable onComplete) {
        switchState(State.COMMITTED).onSuccess(journalIndex);
        onComplete.run();
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

    @Override
    public State getState() {
        return state;
    }

    void reportFailure(final Exception cause) {
        if (nextFailure == null) {
            this.nextFailure = Preconditions.checkNotNull(cause);
        } else {
            LOG.debug("Transaction {} already has a set failure, not updating it", transactionId, cause);
        }
    }

    @Override
    public boolean isFailed() {
        return state == State.FAILED || nextFailure != null;
    }

    @Override
    ToStringHelper addToStringAttributes(final ToStringHelper toStringHelper) {
        return super.addToStringAttributes(toStringHelper).add("nextFailure", nextFailure);
    }
}
