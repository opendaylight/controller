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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.VerifyException;
import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.FutureCallback;
import java.util.Optional;
import java.util.SortedSet;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidateTip;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeModification;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeTip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Non-sealed for mocking
@VisibleForTesting
public abstract class CommitCohort {

    sealed interface State {
        // Nothing else
    }

    @NonNullByDefault
    record Ready(DataTreeModification modification, CompositeDataTreeCohort userCohorts) implements State {
        Ready {
            requireNonNull(modification);
            requireNonNull(userCohorts);
        }

        CanCommitPending toCanCommitPending(final FutureCallback<Empty> callback) {
            return new CanCommitPending(modification, userCohorts, callback);
        }

        @Override
        public String toString() {
            return Ready.class.getSimpleName();
        }
    }

    @NonNullByDefault
    record CanCommitPending(DataTreeModification modification,
            CompositeDataTreeCohort userCohorts,
            FutureCallback<Empty> callback) implements State {
        CanCommitPending {
            requireNonNull(modification);
            requireNonNull(userCohorts);
            requireNonNull(callback);
        }

        CanCommitComplete toCanCommitComplete(final DataTreeTip tip) {
            return new CanCommitComplete(modification, userCohorts, tip);
        }

        @Override
        public String toString() {
            return CanCommitPending.class.getSimpleName();
        }
    }

    @NonNullByDefault
    record CanCommitComplete(
            DataTreeModification modification,
            CompositeDataTreeCohort userCohorts,
            DataTreeTip tip) implements State {
        CanCommitComplete {
            requireNonNull(modification);
            requireNonNull(userCohorts);
            requireNonNull(tip);
        }

        CanCommitComplete rebase(final DataTreeTip newTip) {
            return new CanCommitComplete(modification, userCohorts, newTip);
        }

        PreCommitPending toPreCommitPending(final FutureCallback<DataTreeCandidate> callback) {
            return new PreCommitPending(modification, userCohorts, tip, callback);
        }

        @Override
        public String toString() {
            return CanCommitComplete.class.getSimpleName();
        }
    }

    @NonNullByDefault
    record PreCommitPending(
            DataTreeModification modification,
            CompositeDataTreeCohort userCohorts,
            DataTreeTip tip,
            FutureCallback<DataTreeCandidate> callback) implements State {
        PreCommitPending {
            requireNonNull(modification);
            requireNonNull(userCohorts);
            requireNonNull(tip);
            requireNonNull(callback);
        }

        @Override
        public String toString() {
            return PreCommitPending.class.getSimpleName();
        }
    }

    @NonNullByDefault
    record PreCommitComplete(
            DataTreeModification modification) implements State {
        PreCommitComplete {
            requireNonNull(modification);
        }

        @Override
        public String toString() {
            return PreCommitComplete.class.getSimpleName();
        }
    }

    record CommitPending(DataTreeCandidate candidate) implements State {
        CommitPending {
            requireNonNull(candidate);
        }

        @Override
        public String toString() {
            return CommitPending.class.getSimpleName();
        }
    }

    @NonNullByDefault
    record Failing(Exception cause) implements State {
        Failing {
            requireNonNull(cause);
        }
    }

    @NonNullByDefault
    record Failed() implements State {
        // FIXME: cause?
    }

//    public enum State {
//        ABORTED,
//        COMMITTED,
//    }

    private static final Logger LOG = LoggerFactory.getLogger(CommitCohort.class);

    private final ShardDataTree dataTree;
    private final @NonNull TransactionIdentifier transactionId;
    private final @Nullable SortedSet<String> participatingShardNames;

    private State state;
    private FutureCallback<?> callback;
    private long lastAccess;

    CommitCohort(final ShardDataTree dataTree, final ReadWriteShardDataTreeTransaction transaction,
            final CompositeDataTreeCohort userCohorts, final Optional<SortedSet<String>> participatingShardNames) {
        this(dataTree, transaction.getSnapshot(), transaction.getIdentifier(), userCohorts, participatingShardNames);
    }

    CommitCohort(final ShardDataTree dataTree, final DataTreeModification modification,
            final TransactionIdentifier transactionId, final CompositeDataTreeCohort userCohorts,
            final Optional<SortedSet<String>> participatingShardNames) {
        this.dataTree = requireNonNull(dataTree);
        this.transactionId = requireNonNull(transactionId);
        this.participatingShardNames = requireNonNull(participatingShardNames).orElse(null);
        state = new Ready(modification, userCohorts);
    }

    CommitCohort(final ShardDataTree dataTree, final TransactionIdentifier transactionId, final Exception nextFailure) {
        this.dataTree = requireNonNull(dataTree);
        this.transactionId = requireNonNull(transactionId);
        participatingShardNames = null;
        state = new Failing(nextFailure);
    }

    final @NonNull TransactionIdentifier transactionId() {
        return transactionId;
    }

    final long lastAccess() {
        return lastAccess;
    }

    final void setLastAccess(final long newLastAccess) {
        lastAccess = newLastAccess;
    }

    public final State getState() {
        return state;
    }

    public final boolean isFailed() {
        return switch (state) {
            case Failing failing -> true;
            case Failed failed -> true;
            default -> false;
        };
    }

    final Optional<SortedSet<String>> getParticipatingShardNames() {
        return Optional.ofNullable(participatingShardNames);
    }

    // FIXME: Should return rebased DataTreeCandidateTip
    @VisibleForTesting
    public final void canCommit(final FutureCallback<Empty> callback) {
        switch (state) {
            case CanCommitPending pending -> dataTree.startCanCommit(this);
            case Ready ready -> {
                state = ready.toCanCommitPending(callback);
                dataTree.startCanCommit(this);
            }
            case Failing(var cause) -> {
                state = new Failed();
                callback.onFailure(cause);
            }
            default -> {
                throw new IllegalStateException("Transaction %s in state %s cannot start can-commit".formatted(
                    transactionId, state));
            }
        }
    }

    final void setState(final State expected, final State next) {
        if (state == expected) {
            state = requireNonNull(next);
        } else {
            throw new VerifyException(
                "Unexpected state " + state + " when transitioning from " + expected + " to " + next);
        }
    }

    final void failedCanCommit(final Exception cause) {
        dataTree.getStats().incrementFailedTransactionsCount();
        switchState(State.FAILED).onFailure(cause);
    }

    @VisibleForTesting
    public final void preCommit(final FutureCallback<DataTreeCandidate> callback) {
        switch (state) {
            case CanCommitComplete canCommit -> {
                state = canCommit.toPreCommitPending(callback);
                dataTree.startPreCommit(this);
            }
            case Failing(var cause) -> {
                state = new Failed();
                userCohorts.abort();
                dataTree.getStats().incrementFailedTransactionsCount();
                callback.onFailure(cause);
            }
            default -> {
                throw new IllegalStateException("Transaction %s in state %s cannot start pre-commit".formatted(
                    transactionId, state));
            }
        }
    }

    final void successfulPreCommit(final DataTreeCandidateTip dataTreeCandidate) {
        LOG.trace("Transaction {} prepared candidate {}", modification, dataTreeCandidate);
        candidate = verifyNotNull(dataTreeCandidate);
        switchState(State.PRE_COMMIT_COMPLETE).onSuccess(dataTreeCandidate);
    }

    final void failedPreCommit(final Throwable cause) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Transaction {} failed to prepare", modification, cause);
        } else {
            LOG.error("Transaction {} failed to prepare", transactionId, cause);
        }

        userCohorts.abort();
        dataTree.getStats().incrementFailedTransactionsCount();
        switchState(State.FAILED).onFailure(cause);
    }

    /**
     * Run user-defined canCommit and preCommit hooks. We want to run these before we initiate persistence so that
     * any failure to validate is propagated before we record the transaction.
     *
     * @param dataTreeCandidate {@link DataTreeCandidate} under consideration
     * @param futureCallback the callback to invoke on completion, which may be immediate or async.
     */
    final void userPreCommit(final DataTreeCandidate dataTreeCandidate, final FutureCallback<Empty> futureCallback) {
        userCohorts.reset();

        final var userCanCommit = userCohorts.canCommit(dataTreeCandidate);
        if (userCanCommit != null) {
            userCanCommit.whenComplete((noop, failure) -> {
                if (failure != null) {
                    futureCallback.onFailure(failure);
                } else {
                    doUserPreCommit(futureCallback);
                }
            });
        } else {
            doUserPreCommit(futureCallback);
        }
    }

    private void doUserPreCommit(final FutureCallback<Empty> futureCallback) {
        final var userPreCommit = userCohorts.preCommit();
        if (userPreCommit != null) {
            userPreCommit.whenComplete((noop, failure) -> {
                if (failure != null) {
                    futureCallback.onFailure(failure);
                } else {
                    futureCallback.onSuccess(Empty.value());
                }
            });
        } else {
            futureCallback.onSuccess(Empty.value());
        }
    }

    @VisibleForTesting
    public final void abort(final FutureCallback<Empty> abortCallback) {
        candidate = null;
        state = State.ABORTED;

        final var userAbort = userCohorts.abort();
        if (userAbort != null) {
            userAbort.whenComplete((noop, failure) -> {
                if (failure != null) {
                    abortCallback.onFailure(failure);
                } else {
                    abortCallback.onSuccess(Empty.value());
                }
            });
        } else {
            abortCallback.onSuccess(Empty.value());
        }
    }

    void successfulCommit(final UnsignedLong journalIndex, final Runnable onComplete) {
        final var userCommit = userCohorts.commit();
        if (userCommit != null) {
            userCommit.whenComplete((noop, failure) -> {
                if (failure != null) {
                    LOG.error("User cohorts failed to commit", failure);
                }
                finishSuccessfulCommit(journalIndex, onComplete);
            });
        } else {
            finishSuccessfulCommit(journalIndex, onComplete);
        }
    }

    private void finishSuccessfulCommit(final UnsignedLong journalIndex, final Runnable onComplete) {
        final var stats = dataTree.getStats();
        stats.incrementCommittedTransactionCount();

        switchState(State.COMMITTED).onSuccess(journalIndex);
        onComplete.run();
    }

    final void failedCommit(final Exception cause) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Transaction {} failed to commit", modification, cause);
        } else {
            LOG.error("Transaction failed to commit", cause);
        }

        userCohorts.abort();
        dataTree.getStats().incrementFailedTransactionsCount();
        switchState(State.FAILED).onFailure(cause);
    }

    @VisibleForTesting
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

    final void reportFailure(final Exception cause) {
        if (nextFailure == null) {
            nextFailure = requireNonNull(cause);
        } else {
            // FIXME: add suppressed exception
            LOG.debug("Transaction {} already has a set failure, not updating it", transactionId, cause);
        }
    }

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(this).omitNullValues()
            .add("id", transactionId)
            .add("state", state)
            .add("nextFailure", nextFailure)
            .toString();
    }

    private void checkState(final State expected) {
        if (state != expected) {
            throw new IllegalStateException("State %s does not match expected state %s for %s".formatted(
                state, expected, transactionId));
        }
    }

    private <T> FutureCallback<T> switchState(final State newState) {
        @SuppressWarnings("unchecked")
        final var ret = (FutureCallback<T>) callback;
        callback = null;
        LOG.debug("Transaction {} changing state from {} to {}", transactionId, state, newState);
        state = newState;
        return ret;
    }
}
