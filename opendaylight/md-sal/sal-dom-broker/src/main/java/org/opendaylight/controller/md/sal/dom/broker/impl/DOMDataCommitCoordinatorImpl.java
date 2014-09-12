/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.util.DurationStatsTracker;
import org.opendaylight.yangtools.util.concurrent.MappingCheckedFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Implementation of blocking three phase commit coordinator, which which
 * supports coordination on multiple {@link DOMStoreThreePhaseCommitCohort}.
 *
 * This implementation does not support cancelation of commit,
 *
 * In order to advance to next phase of three phase commit all subtasks of
 * previous step must be finish.
 *
 * This executor does not have an upper bound on subtask timeout.
 *
 *
 */
public class DOMDataCommitCoordinatorImpl implements DOMDataCommitExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(DOMDataCommitCoordinatorImpl.class);
    private final DurationStatsTracker commitStatsTracker = new DurationStatsTracker();
    private final ListeningExecutorService executor;

    /**
     *
     * Construct DOMDataCommitCoordinator which uses supplied executor to
     * process commit coordinations.
     *
     * @param executor
     */
    public DOMDataCommitCoordinatorImpl(final ListeningExecutorService executor) {
        this.executor = Preconditions.checkNotNull(executor, "executor must not be null.");
    }

    public DurationStatsTracker getCommitStatsTracker() {
        return commitStatsTracker;
    }

    @Override
    public CheckedFuture<Void,TransactionCommitFailedException> submit(final DOMDataWriteTransaction transaction,
            final Iterable<DOMStoreThreePhaseCommitCohort> cohorts, final Optional<DOMDataCommitErrorListener> listener) {
        Preconditions.checkArgument(transaction != null, "Transaction must not be null.");
        Preconditions.checkArgument(cohorts != null, "Cohorts must not be null.");
        Preconditions.checkArgument(listener != null, "Listener must not be null");
        LOG.debug("Tx: {} is submitted for execution.", transaction.getIdentifier());

        ListenableFuture<Void> commitFuture = null;
        try {
            commitFuture = executor.submit(new CommitCoordinationTask(transaction, cohorts,
                    listener, commitStatsTracker));
        } catch(RejectedExecutionException e) {
            LOG.error("The commit executor's queue is full - submit task was rejected. \n" +
                      executor, e);
            return Futures.immediateFailedCheckedFuture(
                    new TransactionCommitFailedException(
                        "Could not submit the commit task - the commit queue capacity has been exceeded.", e));
        }

        if (listener.isPresent()) {
            Futures.addCallback(commitFuture, new DOMDataCommitErrorInvoker(transaction, listener.get()));
        }

        return MappingCheckedFuture.create(commitFuture,
                TransactionCommitFailedExceptionMapper.COMMIT_ERROR_MAPPER);
    }

    /**
     *
     * Phase of 3PC commit
     *
     * Represents phase of 3PC Commit
     *
     *
     */
    private static enum CommitPhase {
        /**
         *
         * Commit Coordination Task is submitted for executing
         *
         */
        SUBMITTED,
        /**
         * Commit Coordination Task is in can commit phase of 3PC
         *
         */
        CAN_COMMIT,
        /**
         * Commit Coordination Task is in pre-commit phase of 3PC
         *
         */
        PRE_COMMIT,
        /**
         * Commit Coordination Task is in commit phase of 3PC
         *
         */
        COMMIT,
        /**
         * Commit Coordination Task is in abort phase of 3PC
         *
         */
        ABORT
    }

    /**
     * Implementation of blocking three-phase commit-coordination tasks without
     * support of cancellation.
     */
    private static final class CommitCoordinationTask implements Callable<Void> {
        private static final AtomicReferenceFieldUpdater<CommitCoordinationTask, CommitPhase> PHASE_UPDATER =
                AtomicReferenceFieldUpdater.newUpdater(CommitCoordinationTask.class, CommitPhase.class, "currentPhase");
        private final DOMDataWriteTransaction tx;
        private final Iterable<DOMStoreThreePhaseCommitCohort> cohorts;
        private final DurationStatsTracker commitStatTracker;
        private final int cohortSize;
        private volatile CommitPhase currentPhase = CommitPhase.SUBMITTED;

        public CommitCoordinationTask(final DOMDataWriteTransaction transaction,
                final Iterable<DOMStoreThreePhaseCommitCohort> cohorts,
                final Optional<DOMDataCommitErrorListener> listener,
                final DurationStatsTracker commitStatTracker) {
            this.tx = Preconditions.checkNotNull(transaction, "transaction must not be null");
            this.cohorts = Preconditions.checkNotNull(cohorts, "cohorts must not be null");
            this.commitStatTracker = commitStatTracker;
            this.cohortSize = Iterables.size(cohorts);
        }

        @Override
        public Void call() throws TransactionCommitFailedException {
            final long startTime = commitStatTracker != null ? System.nanoTime() : 0;

            try {
                canCommitBlocking();
                preCommitBlocking();
                commitBlocking();
                return null;
            } catch (TransactionCommitFailedException e) {
                final CommitPhase phase = currentPhase;
                LOG.warn("Tx: {} Error during phase {}, starting Abort", tx.getIdentifier(), phase, e);
                abortBlocking(e, phase);
                throw e;
            } finally {
                if (commitStatTracker != null) {
                    commitStatTracker.addDuration(System.nanoTime() - startTime);
                }
            }
        }

        /**
         *
         * Invokes canCommit on underlying cohorts and blocks till
         * all results are returned.
         *
         * Valid state transition is from SUBMITTED to CAN_COMMIT,
         * if currentPhase is not SUBMITTED throws IllegalStateException.
         *
         * @throws TransactionCommitFailedException
         *             If one of cohorts failed can Commit
         *
         */
        private void canCommitBlocking() throws TransactionCommitFailedException {
            for (ListenableFuture<?> canCommit : canCommitAll()) {
                try {
                    final Boolean result = (Boolean)canCommit.get();
                    if (result == null || !result) {
                        throw new TransactionCommitFailedException("Can Commit failed, no detailed cause available.");
                    }
                } catch (InterruptedException | ExecutionException e) {
                    throw TransactionCommitFailedExceptionMapper.CAN_COMMIT_ERROR_MAPPER.apply(e);
                }
            }
        }

        /**
         *
         * Invokes canCommit on underlying cohorts and returns composite future
         * which will contains {@link Boolean#TRUE} only and only if
         * all cohorts returned true.
         *
         * Valid state transition is from SUBMITTED to CAN_COMMIT,
         * if currentPhase is not SUBMITTED throws IllegalStateException.
         *
         * @return List of all cohorts futures from can commit phase.
         *
         */
        private ListenableFuture<?>[] canCommitAll() {
            changeStateFrom(CommitPhase.SUBMITTED, CommitPhase.CAN_COMMIT);

            final ListenableFuture<?>[] ops = new ListenableFuture<?>[cohortSize];
            int i = 0;
            for (DOMStoreThreePhaseCommitCohort cohort : cohorts) {
                ops[i++] = cohort.canCommit();
            }
            return ops;
        }

        /**
         *
         * Invokes preCommit on underlying cohorts and blocks till
         * all results are returned.
         *
         * Valid state transition is from CAN_COMMIT to PRE_COMMIT, if current
         * state is not CAN_COMMIT
         * throws IllegalStateException.
         *
         * @throws TransactionCommitFailedException
         *             If one of cohorts failed preCommit
         *
         */
        private void preCommitBlocking() throws TransactionCommitFailedException {
            final ListenableFuture<?>[] preCommitFutures = preCommitAll();
            try {
                for(ListenableFuture<?> future : preCommitFutures) {
                    future.get();
                }
            } catch (InterruptedException | ExecutionException e) {
                throw TransactionCommitFailedExceptionMapper.PRE_COMMIT_MAPPER.apply(e);
            }
        }

        /**
         *
         * Invokes preCommit on underlying cohorts and returns future
         * which will complete once all preCommit on cohorts completed or
         * failed.
         *
         *
         * Valid state transition is from CAN_COMMIT to PRE_COMMIT, if current
         * state is not CAN_COMMIT
         * throws IllegalStateException.
         *
         * @return List of all cohorts futures from can commit phase.
         *
         */
        private ListenableFuture<?>[] preCommitAll() {
            changeStateFrom(CommitPhase.CAN_COMMIT, CommitPhase.PRE_COMMIT);

            final ListenableFuture<?>[] ops = new ListenableFuture<?>[cohortSize];
            int i = 0;
            for (DOMStoreThreePhaseCommitCohort cohort : cohorts) {
                ops[i++] = cohort.preCommit();
            }
            return ops;
        }

        /**
         *
         * Invokes commit on underlying cohorts and blocks till
         * all results are returned.
         *
         * Valid state transition is from PRE_COMMIT to COMMIT, if not throws
         * IllegalStateException.
         *
         * @throws TransactionCommitFailedException
         *             If one of cohorts failed preCommit
         *
         */
        private void commitBlocking() throws TransactionCommitFailedException {
            final ListenableFuture<?>[] commitFutures = commitAll();
            try {
                for(ListenableFuture<?> future : commitFutures) {
                    future.get();
                }
            } catch (InterruptedException | ExecutionException e) {
                throw TransactionCommitFailedExceptionMapper.COMMIT_ERROR_MAPPER.apply(e);
            }
        }

        /**
         *
         * Invokes commit on underlying cohorts and returns future which
         * completes
         * once all commits on cohorts are completed.
         *
         * Valid state transition is from PRE_COMMIT to COMMIT, if not throws
         * IllegalStateException
         *
         * @return List of all cohorts futures from can commit phase.
         *
         */
        private ListenableFuture<?>[] commitAll() {
            changeStateFrom(CommitPhase.PRE_COMMIT, CommitPhase.COMMIT);

            final ListenableFuture<?>[] ops = new ListenableFuture<?>[cohortSize];
            int i = 0;
            for (DOMStoreThreePhaseCommitCohort cohort : cohorts) {
                ops[i++] = cohort.commit();
            }
            return ops;
        }

        /**
         * Aborts transaction.
         *
         * Invokes {@link DOMStoreThreePhaseCommitCohort#abort()} on all
         * cohorts, blocks
         * for all results. If any of the abort failed throws
         * IllegalStateException,
         * which will contains originalCause as suppressed Exception.
         *
         * If aborts we're successful throws supplied exception
         *
         * @param originalCause
         *            Exception which should be used to fail transaction for
         *            consumers of transaction
         *            future and listeners of transaction failure.
         * @param phase phase in which the problem ensued
         * @throws TransactionCommitFailedException
         *             on invocation of this method.
         *             originalCa
         * @throws IllegalStateException
         *             if abort failed.
         */
        private void abortBlocking(final TransactionCommitFailedException originalCause, final CommitPhase phase)
                throws TransactionCommitFailedException {
            LOG.warn("Tx: {} Error during phase {}, starting Abort", tx.getIdentifier(), phase, originalCause);
            Exception cause = originalCause;
            try {
                abortAsyncAll(phase).get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Tx: {} Error during Abort.", tx.getIdentifier(), e);
                cause = new IllegalStateException("Abort failed.", e);
                cause.addSuppressed(e);
            }
            Throwables.propagateIfPossible(cause, TransactionCommitFailedException.class);
        }

        /**
         * Invokes abort on underlying cohorts and returns future which
         * completes once all abort on cohorts are completed.
         *
         * @param phase phase in which the problem ensued
         * @return Future which will complete once all cohorts completed
         *         abort.
         */
        private ListenableFuture<Void> abortAsyncAll(final CommitPhase phase) {
            changeStateFrom(phase, CommitPhase.ABORT);

            final ListenableFuture<?>[] ops = new ListenableFuture<?>[cohortSize];
            int i = 0;
            for (DOMStoreThreePhaseCommitCohort cohort : cohorts) {
                ops[i++] = cohort.abort();
            }

            /*
             * We are returning all futures as list, not only succeeded ones in
             * order to fail composite future if any of them failed.
             * See Futures.allAsList for this description.
             */
            @SuppressWarnings({ "unchecked", "rawtypes" })
            ListenableFuture<Void> compositeResult = (ListenableFuture) Futures.allAsList(ops);
            return compositeResult;
        }

        /**
         * Change phase / state of transaction from expected value to new value
         *
         * This method checks state and updates state to new state of
         * of this task if current state equals expected state.
         * If expected state and current state are different raises
         * IllegalStateException
         * which means there is probably bug in implementation of commit
         * coordination.
         *
         * If transition is successful, it logs transition on DEBUG level.
         *
         * @param currentExpected
         *            Required phase for change of state
         * @param newState
         *            New Phase which will be entered by transaction.
         * @throws IllegalStateException
         *             If currentState of task does not match expected state
         */
        private void changeStateFrom(final CommitPhase currentExpected, final CommitPhase newState) {
            final boolean success = PHASE_UPDATER.compareAndSet(this, currentExpected, newState);
            Preconditions.checkState(success, "Invalid state transition: Tx: %s expected: %s current: %s target: %s",
                tx.getIdentifier(), currentExpected, currentPhase, newState);

            LOG.debug("Transaction {}: Phase {} Started", tx.getIdentifier(), newState);
        };
    }

}
