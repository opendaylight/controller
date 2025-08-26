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
import org.opendaylight.controller.cluster.datastore.CommitPhase.Aborted;
import org.opendaylight.controller.cluster.datastore.CommitPhase.CanCommit;
import org.opendaylight.controller.cluster.datastore.CommitPhase.Committed;
import org.opendaylight.controller.cluster.datastore.CommitPhase.DoCommit;
import org.opendaylight.controller.cluster.datastore.CommitPhase.Failed;
import org.opendaylight.controller.cluster.datastore.CommitPhase.NeedCanCommit;
import org.opendaylight.controller.cluster.datastore.CommitPhase.NeedPreCommit;
import org.opendaylight.controller.cluster.datastore.CommitPhase.Pending;
import org.opendaylight.controller.cluster.datastore.CommitPhase.PreCommit;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidateTip;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeModification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Non-sealed for mocking
abstract class CommitCohort {
    private static final Logger LOG = LoggerFactory.getLogger(CommitCohort.class);

    private final DataTreeModification modification;
    private final ShardDataTree dataTree;
    private final @NonNull TransactionIdentifier transactionId;
    private final CompositeDataTreeCohort userCohorts;

    private @NonNull CommitPhase phase = new Pending();
    private DataTreeCandidateTip candidate;
    private FutureCallback<?> callback;
    private Exception nextFailure;
    private long lastAccess;

    @NonNullByDefault
    CommitCohort(final ShardDataTree dataTree, final ReadWriteShardDataTreeTransaction transaction,
            final CompositeDataTreeCohort userCohorts) {
        this(dataTree, transaction.getSnapshot(), transaction.getIdentifier(), userCohorts);
    }

    @NonNullByDefault
    CommitCohort(final ShardDataTree dataTree, final DataTreeModification modification,
            final TransactionIdentifier transactionId, final CompositeDataTreeCohort userCohorts) {
        this.dataTree = requireNonNull(dataTree);
        this.modification = requireNonNull(modification);
        this.transactionId = requireNonNull(transactionId);
        this.userCohorts = requireNonNull(userCohorts);
    }

    @NonNullByDefault
    CommitCohort(final ShardDataTree dataTree, final DataTreeModification modification,
            final TransactionIdentifier transactionId, final Exception nextFailure) {
        this.dataTree = requireNonNull(dataTree);
        this.modification = requireNonNull(modification);
        this.transactionId = requireNonNull(transactionId);
        userCohorts = null;
        this.nextFailure = requireNonNull(nextFailure);
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

    final @NonNull CommitPhase phase() {
        return phase;
    }

    final boolean isFailed() {
        return phase instanceof Failed || nextFailure != null;
    }

    // FIXME: This leaks internal state generated in preCommit,
    // should be result of canCommit
    final DataTreeCandidateTip getCandidate() {
        return candidate;
    }

    final void setNewCandidate(final DataTreeCandidateTip dataTreeCandidate) {
        if (!(phase instanceof PreCommit)) {
            throw new IllegalStateException("State %s does not match expected statePreCommit for %s".formatted(
                phase, transactionId));
        }
        candidate = verifyNotNull(dataTreeCandidate);
    }

    final DataTreeModification getDataTreeModification() {
        return modification;
    }

    // FIXME: Should return rebased DataTreeCandidateTip
    final void canCommit(final FutureCallback<Empty> newCallback) {
        switch (phase) {
            case NeedCanCommit ph -> {
                // No-op
            }
            case Pending ph -> {
                callback = requireNonNull(newCallback);
                phase = new NeedCanCommit();

                if (nextFailure == null) {
                    dataTree.startCanCommit(this);
                } else {
                    failedCanCommit(nextFailure);
                }
            }
            default -> throw new IllegalStateException(
                "State %s does not match expected Pending for %s".formatted(phase, transactionId));
        }
    }

    final void successfulCanCommit() {
        switchState(new CanCommit()).onSuccess(Empty.value());
    }

    final void failedCanCommit(final Exception cause) {
        dataTree.getStats().incrementFailedTransactionsCount();
        switchState(new Failed()).onFailure(cause);
    }

    final void preCommit(final FutureCallback<DataTreeCandidate> newCallback) {
        switch (phase) {
            case CanCommit ph -> {
                callback = requireNonNull(newCallback);
                phase = new NeedPreCommit();

                if (nextFailure == null) {
                    dataTree.startPreCommit(this);
                } else {
                    failedPreCommit(nextFailure);
                }
            }
            default -> throw new IllegalStateException(
                "State %s does not match expected CanCommit for %s".formatted(phase, transactionId));
        }
    }

    final void successfulPreCommit(final DataTreeCandidateTip dataTreeCandidate) {
        LOG.trace("Transaction {} prepared candidate {}", modification, dataTreeCandidate);
        candidate = verifyNotNull(dataTreeCandidate);
        switchState(new PreCommit()).onSuccess(dataTreeCandidate);
    }

    final void failedPreCommit(final Throwable cause) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Transaction {} failed to prepare", modification, cause);
        } else {
            LOG.error("Transaction {} failed to prepare", transactionId, cause);
        }

        userCohorts.abort();
        dataTree.getStats().incrementFailedTransactionsCount();
        switchState(new Failed()).onFailure(cause);
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

    final void abort(final FutureCallback<Empty> abortCallback) {
        if (!dataTree.startAbort(this)) {
            abortCallback.onSuccess(Empty.value());
            return;
        }

        candidate = null;
        dataTree.getStats().incrementAbortTransactionsCount();
        phase = new Aborted();

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

        switchState(new Committed()).onSuccess(journalIndex);
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
        switchState(new Failed()).onFailure(cause);
    }

    void commit(final FutureCallback<UnsignedLong> newCallback) {
        switch (phase) {
            case PreCommit ph -> {
                callback = requireNonNull(newCallback);
                phase = new DoCommit();

                if (nextFailure == null) {
                    dataTree.startCommit(this, candidate);
                } else {
                    failedCommit(nextFailure);
                }
            }
            default -> throw new IllegalStateException(
                "State %s does not match expected PreCommit for %s".formatted(phase, transactionId));
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
            .add("phase", phase)
            .add("nextFailure", nextFailure)
            .toString();
    }

    private <T> FutureCallback<T> switchState(final @NonNull CommitPhase newPhase) {
        @SuppressWarnings("unchecked")
        final var ret = (FutureCallback<T>) callback;
        callback = null;
        LOG.debug("Transaction {} changing state from {} to {}", transactionId, phase, newPhase);
        phase = requireNonNull(newPhase);
        return ret;
    }
}
