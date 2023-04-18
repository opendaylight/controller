/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.FutureCallback;
import java.util.Optional;
import java.util.SortedSet;
import java.util.concurrent.CompletionStage;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidateTip;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeModification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SimpleShardDataTreeCohort extends ShardDataTreeCohort {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleShardDataTreeCohort.class);

    private final DataTreeModification transaction;
    private final ShardDataTree dataTree;
    private final TransactionIdentifier transactionId;
    private final CompositeDataTreeCohort userCohorts;
    private final @Nullable SortedSet<String> participatingShardNames;

    private State state = State.READY;
    private DataTreeCandidateTip candidate;
    private FutureCallback<?> callback;
    private Exception nextFailure;

    SimpleShardDataTreeCohort(final ShardDataTree dataTree, final DataTreeModification transaction,
            final TransactionIdentifier transactionId, final CompositeDataTreeCohort userCohorts,
            final Optional<SortedSet<String>> participatingShardNames) {
        this.dataTree = requireNonNull(dataTree);
        this.transaction = requireNonNull(transaction);
        this.transactionId = requireNonNull(transactionId);
        this.userCohorts = requireNonNull(userCohorts);
        this.participatingShardNames = requireNonNull(participatingShardNames).orElse(null);
    }

    SimpleShardDataTreeCohort(final ShardDataTree dataTree, final DataTreeModification transaction,
        final TransactionIdentifier transactionId, final Exception nextFailure) {
        this.dataTree = requireNonNull(dataTree);
        this.transaction = requireNonNull(transaction);
        this.transactionId = requireNonNull(transactionId);
        userCohorts = null;
        participatingShardNames = null;
        this.nextFailure = requireNonNull(nextFailure);
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

    @Override
    Optional<SortedSet<String>> getParticipatingShardNames() {
        return Optional.ofNullable(participatingShardNames);
    }

    private void checkState(final State expected) {
        Preconditions.checkState(state == expected, "State %s does not match expected state %s for %s",
                state, expected, getIdentifier());
    }

    @Override
    public void canCommit(final FutureCallback<Empty> newCallback) {
        if (state == State.CAN_COMMIT_PENDING) {
            return;
        }

        checkState(State.READY);
        callback = requireNonNull(newCallback);
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
        callback = requireNonNull(newCallback);
        state = State.PRE_COMMIT_PENDING;

        if (nextFailure == null) {
            dataTree.startPreCommit(this);
        } else {
            failedPreCommit(nextFailure);
        }
    }

    @Override
    public void abort(final FutureCallback<Empty> abortCallback) {
        if (!dataTree.startAbort(this)) {
            abortCallback.onSuccess(Empty.value());
            return;
        }

        candidate = null;
        state = State.ABORTED;

        final Optional<CompletionStage<?>> maybeAborts = userCohorts.abort();
        if (!maybeAborts.isPresent()) {
            abortCallback.onSuccess(Empty.value());
            return;
        }

        maybeAborts.orElseThrow().whenComplete((noop, failure) -> {
            if (failure != null) {
                abortCallback.onFailure(failure);
            } else {
                abortCallback.onSuccess(Empty.value());
            }
        });
    }

    @Override
    public void commit(final FutureCallback<UnsignedLong> newCallback) {
        checkState(State.PRE_COMMIT_COMPLETE);
        callback = requireNonNull(newCallback);
        state = State.COMMIT_PENDING;

        if (nextFailure == null) {
            dataTree.startCommit(this, candidate);
        } else {
            failedCommit(nextFailure);
        }
    }

    private <T> FutureCallback<T> switchState(final State newState) {
        @SuppressWarnings("unchecked")
        final FutureCallback<T> ret = (FutureCallback<T>) callback;
        callback = null;
        LOG.debug("Transaction {} changing state from {} to {}", transactionId, state, newState);
        state = newState;
        return ret;
    }

    void setNewCandidate(final DataTreeCandidateTip dataTreeCandidate) {
        checkState(State.PRE_COMMIT_COMPLETE);
        candidate = verifyNotNull(dataTreeCandidate);
    }

    void successfulCanCommit() {
        switchState(State.CAN_COMMIT_COMPLETE).onSuccess(Empty.value());
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
    void userPreCommit(final DataTreeCandidate dataTreeCandidate, final FutureCallback<Empty> futureCallback) {
        userCohorts.reset();

        final Optional<CompletionStage<Empty>> maybeCanCommitFuture = userCohorts.canCommit(dataTreeCandidate);
        if (!maybeCanCommitFuture.isPresent()) {
            doUserPreCommit(futureCallback);
            return;
        }

        maybeCanCommitFuture.orElseThrow().whenComplete((noop, failure) -> {
            if (failure != null) {
                futureCallback.onFailure(failure);
            } else {
                doUserPreCommit(futureCallback);
            }
        });
    }

    private void doUserPreCommit(final FutureCallback<Empty> futureCallback) {
        final Optional<CompletionStage<Empty>> maybePreCommitFuture = userCohorts.preCommit();
        if (!maybePreCommitFuture.isPresent()) {
            futureCallback.onSuccess(Empty.value());
            return;
        }

        maybePreCommitFuture.orElseThrow().whenComplete((noop, failure) -> {
            if (failure != null) {
                futureCallback.onFailure(failure);
            } else {
                futureCallback.onSuccess(Empty.value());
            }
        });
    }

    void successfulPreCommit(final DataTreeCandidateTip dataTreeCandidate) {
        LOG.trace("Transaction {} prepared candidate {}", transaction, dataTreeCandidate);
        candidate = verifyNotNull(dataTreeCandidate);
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
        final Optional<CompletionStage<Empty>> maybeCommitFuture = userCohorts.commit();
        if (!maybeCommitFuture.isPresent()) {
            finishSuccessfulCommit(journalIndex, onComplete);
            return;
        }

        maybeCommitFuture.orElseThrow().whenComplete((noop, failure) -> {
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
            nextFailure = requireNonNull(cause);
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
