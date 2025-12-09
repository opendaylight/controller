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

import com.google.common.base.MoreObjects;
import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.FutureCallback;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidateTip;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeModification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class CommitCohort {
    public enum State {
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

    private static final Logger LOG = LoggerFactory.getLogger(CommitCohort.class);

    private final @NonNull ReadWriteShardDataTreeTransaction transaction;
    private final UserCohorts userCohorts;

    private State state = State.READY;
    private DataTreeCandidateTip candidate;
    private FutureCallback<?> callback;
    private Exception nextFailure;
    private long lastAccess;

    @NonNullByDefault
    CommitCohort(final ReadWriteShardDataTreeTransaction transaction, final UserCohorts userCohorts) {
        this.transaction = requireNonNull(transaction);
        this.userCohorts = requireNonNull(userCohorts);
    }

    @NonNullByDefault
    CommitCohort(final ReadWriteShardDataTreeTransaction transaction, final Exception nextFailure) {
        this.transaction = requireNonNull(transaction);
        this.nextFailure = requireNonNull(nextFailure);
        userCohorts = null;
    }

    @NonNull TransactionIdentifier transactionId() {
        return transaction.getIdentifier();
    }

    long lastAccess() {
        return lastAccess;
    }

    void setLastAccess(final long newLastAccess) {
        lastAccess = newLastAccess;
    }

    State getState() {
        return state;
    }

    boolean isFailed() {
        return state == State.FAILED || nextFailure != null;
    }

    // FIXME: This leaks internal state generated in preCommit, should be result of canCommit
    DataTreeCandidateTip getCandidate() {
        return candidate;
    }

    void setNewCandidate(final DataTreeCandidateTip dataTreeCandidate) {
        checkState(State.PRE_COMMIT_COMPLETE);
        candidate = verifyNotNull(dataTreeCandidate);
    }

    @NonNull DataTreeModification getDataTreeModification() {
        return transaction.getSnapshot();
    }

    private @NonNull ShardDataTree dataTree() {
        return transaction.getParent().dataTree;
    }

    // FIXME: Should return rebased DataTreeCandidateTip
    void canCommit(final FutureCallback<Empty> newCallback) {
        checkState(State.READY);
        callback = requireNonNull(newCallback);
        state = State.CAN_COMMIT_PENDING;

        if (nextFailure == null) {
            dataTree().startCanCommit(this);
        } else {
            failedCanCommit(nextFailure);
        }
    }

    void successfulCanCommit() {
        switchState(State.CAN_COMMIT_COMPLETE).onSuccess(Empty.value());
    }

    void failedCanCommit(final Exception cause) {
        dataTree().getStats().incrementFailedTransactionsCount();
        switchState(State.FAILED).onFailure(cause);
    }

    void preCommit(final FutureCallback<DataTreeCandidate> newCallback) {
        checkState(State.CAN_COMMIT_COMPLETE);
        callback = requireNonNull(newCallback);
        state = State.PRE_COMMIT_PENDING;

        if (nextFailure == null) {
            dataTree().startPreCommit(this);
        } else {
            failedPreCommit(nextFailure);
        }
    }

    void successfulPreCommit(final DataTreeCandidateTip dataTreeCandidate) {
        LOG.trace("Transaction {} prepared candidate {}", getDataTreeModification(), dataTreeCandidate);
        candidate = verifyNotNull(dataTreeCandidate);
        switchState(State.PRE_COMMIT_COMPLETE).onSuccess(dataTreeCandidate);
    }

    void failedPreCommit(final Throwable cause) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Transaction {} failed to prepare", getDataTreeModification(), cause);
        } else {
            LOG.error("Transaction {} failed to prepare", transactionId(), cause);
        }

        userCohorts.abort();
        dataTree().getStats().incrementFailedTransactionsCount();
        switchState(State.FAILED).onFailure(cause);
    }

    /**
     * Run user-defined canCommit and preCommit hooks. We want to run these before we initiate persistence so that
     * any failure to validate is propagated before we record the transaction.
     *
     * @param dataTreeCandidate {@link DataTreeCandidate} under consideration
     * @param futureCallback the callback to invoke on completion, which may be immediate or async.
     */
    @NonNullByDefault
    void userPreCommit(final DataTreeCandidate dataTreeCandidate, final FutureCallback<Empty> futureCallback) {
        userCohorts.reset();

        final var userCanCommit = userCohorts.canCommit(dataTreeCandidate);
        if (userCanCommit == null) {
            doUserPreCommit(futureCallback);
            return;
        }

        userCanCommit.whenComplete((noop, failure) -> {
            if (failure != null) {
                futureCallback.onFailure(failure);
            } else {
                doUserPreCommit(futureCallback);
            }
        });
    }

    @NonNullByDefault
    private void doUserPreCommit(final FutureCallback<Empty> futureCallback) {
        final var userPreCommit = userCohorts.preCommit();
        if (userPreCommit == null) {
            futureCallback.onSuccess(Empty.value());
            return;
        }

        userPreCommit.whenComplete((noop, failure) -> {
            if (failure != null) {
                futureCallback.onFailure(failure);
            } else {
                futureCallback.onSuccess(Empty.value());
            }
        });
    }

    @NonNullByDefault
    void abort(final FutureCallback<Empty> abortCallback) {
        final var dataTree = dataTree();
        if (!dataTree.startAbort(this)) {
            abortCallback.onSuccess(Empty.value());
            return;
        }

        candidate = null;
        dataTree.getStats().incrementAbortTransactionsCount();
        state = State.ABORTED;

        final var userAbort = userCohorts.abort();
        if (userAbort == null) {
            abortCallback.onSuccess(Empty.value());
            return;
        }

        userAbort.whenComplete((noop, failure) -> {
            if (failure != null) {
                abortCallback.onFailure(failure);
            } else {
                abortCallback.onSuccess(Empty.value());
            }
        });
    }

    @NonNullByDefault
    void successfulCommit(final UnsignedLong journalIndex, final Runnable onComplete) {
        final var userCommit = userCohorts.commit();
        if (userCommit == null) {
            finishSuccessfulCommit(journalIndex, onComplete);
            return;
        }

        userCommit.whenComplete((noop, failure) -> {
            if (failure != null) {
                LOG.error("User cohorts failed to commit", failure);
            }
            finishSuccessfulCommit(journalIndex, onComplete);
        });
    }

    private void finishSuccessfulCommit(final UnsignedLong journalIndex, final Runnable onComplete) {
        dataTree().getStats().incrementCommittedTransactionCount();

        switchState(State.COMMITTED).onSuccess(journalIndex);
        onComplete.run();
    }

    void failedCommit(final Exception cause) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Transaction {} failed to commit", getDataTreeModification(), cause);
        } else {
            LOG.error("Transaction failed to commit", cause);
        }

        userCohorts.abort();
        dataTree().getStats().incrementFailedTransactionsCount();
        switchState(State.FAILED).onFailure(cause);
    }

    void commit(final FutureCallback<UnsignedLong> newCallback) {
        checkState(State.PRE_COMMIT_COMPLETE);
        callback = transaction.getParent().wrapCommitCallback(transaction, requireNonNull(newCallback));
        state = State.COMMIT_PENDING;

        if (nextFailure == null) {
            dataTree().startCommit(this, candidate);
        } else {
            failedCommit(nextFailure);
        }
    }

    void reportFailure(final Exception cause) {
        if (nextFailure == null) {
            nextFailure = requireNonNull(cause);
        } else {
            // FIXME: add suppressed exception
            LOG.debug("Transaction {} already has a set failure, not updating it", transactionId(), cause);
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).omitNullValues()
            .add("id", transactionId())
            .add("state", state)
            .add("nextFailure", nextFailure)
            .toString();
    }

    private void checkState(final State expected) {
        if (state != expected) {
            throw new IllegalStateException("State %s does not match expected state %s for %s".formatted(
                state, expected, transactionId()));
        }
    }

    private <T> FutureCallback<T> switchState(final State newState) {
        @SuppressWarnings("unchecked")
        final var ret = (FutureCallback<T>) callback;
        callback = null;
        LOG.debug("Transaction {} changing state from {} to {}", transactionId(), state, newState);
        state = newState;
        return ret;
    }
}
