/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;

import javax.annotation.concurrent.GuardedBy;

import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.util.concurrent.MappingCheckedFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

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

    /**
     * Runs AND binary operation between all booleans in supplied iteration of booleans.
     *
     * This method will stop evaluating iterables if first found is false.
     */
    private static final Function<Iterable<Boolean>, Boolean> AND_FUNCTION = new Function<Iterable<Boolean>, Boolean>() {

        @Override
        public Boolean apply(final Iterable<Boolean> input) {
            for(boolean value : input) {
               if(!value) {
                   return Boolean.FALSE;
               }
            }
            return Boolean.TRUE;
        }
    };

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

    @Override
    public CheckedFuture<Void,TransactionCommitFailedException> submit(final DOMDataWriteTransaction transaction,
            final Iterable<DOMStoreThreePhaseCommitCohort> cohorts, final Optional<DOMDataCommitErrorListener> listener) {
        Preconditions.checkArgument(transaction != null, "Transaction must not be null.");
        Preconditions.checkArgument(cohorts != null, "Cohorts must not be null.");
        Preconditions.checkArgument(listener != null, "Listener must not be null");
        LOG.debug("Tx: {} is submitted for execution.", transaction.getIdentifier());

        ListenableFuture<Void> commitFuture = null;
        try {
            commitFuture = executor.submit(new CommitCoordinationTask(transaction, cohorts, listener));
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
     *
     * Implementation of blocking three-phase commit-coordination tasks without
     * support of cancelation.
     *
     */
    private static class CommitCoordinationTask implements Callable<Void> {

        private final DOMDataWriteTransaction tx;
        private final Iterable<DOMStoreThreePhaseCommitCohort> cohorts;

        @GuardedBy("this")
        private CommitPhase currentPhase;

        public CommitCoordinationTask(final DOMDataWriteTransaction transaction,
                final Iterable<DOMStoreThreePhaseCommitCohort> cohorts,
                final Optional<DOMDataCommitErrorListener> listener) {
            this.tx = Preconditions.checkNotNull(transaction, "transaction must not be null");
            this.cohorts = Preconditions.checkNotNull(cohorts, "cohorts must not be null");
            this.currentPhase = CommitPhase.SUBMITTED;
        }

        @Override
        public Void call() throws TransactionCommitFailedException {

            try {
                canCommitBlocking();
                preCommitBlocking();
                commitBlocking();
                return null;
            } catch (TransactionCommitFailedException e) {
                LOG.warn("Tx: {} Error during phase {}, starting Abort", tx.getIdentifier(), currentPhase, e);
                abortBlocking(e);
                throw e;
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
            final Boolean canCommitResult = canCommitAll().checkedGet();
            if (!canCommitResult) {
                throw new TransactionCommitFailedException("Can Commit failed, no detailed cause available.");
            }
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
            preCommitAll().checkedGet();
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
            commitAll().checkedGet();
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
         * @throws TransactionCommitFailedException
         *             on invocation of this method.
         *             originalCa
         * @throws IllegalStateException
         *             if abort failed.
         */
        private void abortBlocking(final TransactionCommitFailedException originalCause)
                throws TransactionCommitFailedException {
            LOG.warn("Tx: {} Error during phase {}, starting Abort", tx.getIdentifier(), currentPhase, originalCause);
            Exception cause = originalCause;
            try {
                abortAsyncAll().get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Tx: {} Error during Abort.", tx.getIdentifier(), e);
                cause = new IllegalStateException("Abort failed.", e);
                cause.addSuppressed(e);
            }
            Throwables.propagateIfPossible(cause, TransactionCommitFailedException.class);
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
         * @return Future which will complete once all cohorts completed
         *         preCommit.
         *         Future throws TransactionCommitFailedException
         *         If any of cohorts failed preCommit
         *
         */
        private CheckedFuture<Void, TransactionCommitFailedException> preCommitAll() {
            changeStateFrom(CommitPhase.CAN_COMMIT, CommitPhase.PRE_COMMIT);
            Builder<ListenableFuture<Void>> ops = ImmutableList.builder();
            for (DOMStoreThreePhaseCommitCohort cohort : cohorts) {
                ops.add(cohort.preCommit());
            }
            /*
             * We are returing all futures as list, not only succeeded ones in
             * order to fail composite future if any of them failed.
             * See Futures.allAsList for this description.
             */
            @SuppressWarnings({ "unchecked", "rawtypes" })
            ListenableFuture<Void> compositeResult = (ListenableFuture) Futures.allAsList(ops.build());
            return MappingCheckedFuture.create(compositeResult,
                                         TransactionCommitFailedExceptionMapper.PRE_COMMIT_MAPPER);
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
         * @return Future which will complete once all cohorts completed
         *         commit.
         *         Future throws TransactionCommitFailedException
         *         If any of cohorts failed preCommit
         *
         */
        private CheckedFuture<Void, TransactionCommitFailedException> commitAll() {
            changeStateFrom(CommitPhase.PRE_COMMIT, CommitPhase.COMMIT);
            Builder<ListenableFuture<Void>> ops = ImmutableList.builder();
            for (DOMStoreThreePhaseCommitCohort cohort : cohorts) {
                ops.add(cohort.commit());
            }
            /*
             * We are returing all futures as list, not only succeeded ones in
             * order to fail composite future if any of them failed.
             * See Futures.allAsList for this description.
             */
            @SuppressWarnings({ "unchecked", "rawtypes" })
            ListenableFuture<Void> compositeResult = (ListenableFuture) Futures.allAsList(ops.build());
            return MappingCheckedFuture.create(compositeResult,
                                     TransactionCommitFailedExceptionMapper.COMMIT_ERROR_MAPPER);
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
         * @return Future which will complete once all cohorts completed
         *         preCommit.
         *         Future throws TransactionCommitFailedException
         *         If any of cohorts failed preCommit
         *
         */
        private CheckedFuture<Boolean, TransactionCommitFailedException> canCommitAll() {
            changeStateFrom(CommitPhase.SUBMITTED, CommitPhase.CAN_COMMIT);
            Builder<ListenableFuture<Boolean>> canCommitOperations = ImmutableList.builder();
            for (DOMStoreThreePhaseCommitCohort cohort : cohorts) {
                canCommitOperations.add(cohort.canCommit());
            }
            ListenableFuture<List<Boolean>> allCanCommits = Futures.allAsList(canCommitOperations.build());
            ListenableFuture<Boolean> allSuccessFuture = Futures.transform(allCanCommits, AND_FUNCTION);
            return MappingCheckedFuture.create(allSuccessFuture,
                                       TransactionCommitFailedExceptionMapper.CAN_COMMIT_ERROR_MAPPER);

        }

        /**
         *
         * Invokes abort on underlying cohorts and returns future which
         * completes
         * once all abort on cohorts are completed.
         *
         * @return Future which will complete once all cohorts completed
         *         abort.
         *
         */
        private ListenableFuture<Void> abortAsyncAll() {
            changeStateFrom(currentPhase, CommitPhase.ABORT);
            Builder<ListenableFuture<Void>> ops = ImmutableList.builder();
            for (DOMStoreThreePhaseCommitCohort cohort : cohorts) {
                ops.add(cohort.abort());
            }
            /*
             * We are returing all futures as list, not only succeeded ones in
             * order to fail composite future if any of them failed.
             * See Futures.allAsList for this description.
             */
            @SuppressWarnings({ "unchecked", "rawtypes" })
            ListenableFuture<Void> compositeResult = (ListenableFuture) Futures.allAsList(ops.build());
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
        private synchronized void changeStateFrom(final CommitPhase currentExpected, final CommitPhase newState) {
            Preconditions.checkState(currentPhase.equals(currentExpected),
                    "Invalid state transition: Tx: %s current state: %s new state: %s", tx.getIdentifier(),
                    currentPhase, newState);
            LOG.debug("Transaction {}: Phase {} Started ", tx.getIdentifier(), newState);
            currentPhase = newState;
        };

    }

}
