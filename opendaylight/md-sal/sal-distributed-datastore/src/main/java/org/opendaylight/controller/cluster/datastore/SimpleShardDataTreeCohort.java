/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.dispatch.ExecutionContexts;
import akka.dispatch.Futures;
import akka.dispatch.OnComplete;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.FutureCallback;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateTip;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

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

        final Optional<List<Future<Object>>> maybeAborts = userCohorts.abort();
        if (!maybeAborts.isPresent()) {
            abortCallback.onSuccess(null);
            return;
        }

        final Future<Iterable<Object>> aborts = Futures.sequence(maybeAborts.get(), ExecutionContexts.global());
        if (aborts.isCompleted()) {
            abortCallback.onSuccess(null);
            return;
        }

        aborts.onComplete(new OnComplete<Iterable<Object>>() {
            @Override
            public void onComplete(final Throwable failure, final Iterable<Object> objs) {
                if (failure != null) {
                    abortCallback.onFailure(failure);
                } else {
                    abortCallback.onSuccess(null);
                }
            }
        }, ExecutionContexts.global());
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
     * @throws ExecutionException if the operation fails
     * @throws TimeoutException if the operation times out
     */
    // FIXME: this should be asynchronous
    void userPreCommit(final DataTreeCandidate dataTreeCandidate) throws ExecutionException, TimeoutException {
        userCohorts.reset();
        userCohorts.canCommit(dataTreeCandidate);
        userCohorts.preCommit();
    }

    void successfulPreCommit(final DataTreeCandidateTip dataTreeCandidate) {
        LOG.trace("Transaction {} prepared candidate {}", transaction, dataTreeCandidate);
        this.candidate = Verify.verifyNotNull(dataTreeCandidate);
        switchState(State.PRE_COMMIT_COMPLETE).onSuccess(dataTreeCandidate);
    }

    void failedPreCommit(final Exception cause) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Transaction {} failed to prepare", transaction, cause);
        } else {
            LOG.error("Transaction {} failed to prepare", transactionId, cause);
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
