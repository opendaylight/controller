/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.dom.broker.impl;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.util.DurationStatisticsTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of blocking three-phase commit-coordination tasks without
 * support of cancellation.
 */
final class CommitCoordinationTask implements Callable<Void> {
    private enum Phase {
        canCommit,
        preCommit,
        doCommit,
    }

    private static final Logger LOG = LoggerFactory.getLogger(CommitCoordinationTask.class);
    private final Collection<DOMStoreThreePhaseCommitCohort> cohorts;
    private final DurationStatisticsTracker commitStatTracker;
    private final DOMDataWriteTransaction tx;

    public CommitCoordinationTask(final DOMDataWriteTransaction transaction,
            final Collection<DOMStoreThreePhaseCommitCohort> cohorts,
            final DurationStatisticsTracker commitStatTracker) {
        this.tx = Preconditions.checkNotNull(transaction, "transaction must not be null");
        this.cohorts = Preconditions.checkNotNull(cohorts, "cohorts must not be null");
        this.commitStatTracker = commitStatTracker;
    }

    @Override
    public Void call() throws TransactionCommitFailedException {
        final long startTime = commitStatTracker != null ? System.nanoTime() : 0;

        Phase phase = Phase.canCommit;

        try {
            LOG.debug("Transaction {}: canCommit Started", tx.getIdentifier());
            canCommitBlocking();

            phase = Phase.preCommit;
            LOG.debug("Transaction {}: preCommit Started", tx.getIdentifier());
            preCommitBlocking();

            phase = Phase.doCommit;
            LOG.debug("Transaction {}: doCommit Started", tx.getIdentifier());
            commitBlocking();

            LOG.debug("Transaction {}: doCommit completed", tx.getIdentifier());
            return null;
        } catch (final TransactionCommitFailedException e) {
            LOG.warn("Tx: {} Error during phase {}, starting Abort", tx.getIdentifier(), phase, e);
            abortBlocking(e);
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
        for (final ListenableFuture<?> canCommit : canCommitAll()) {
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
        final ListenableFuture<?>[] ops = new ListenableFuture<?>[cohorts.size()];
        int i = 0;
        for (final DOMStoreThreePhaseCommitCohort cohort : cohorts) {
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
            for(final ListenableFuture<?> future : preCommitFutures) {
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
        final ListenableFuture<?>[] ops = new ListenableFuture<?>[cohorts.size()];
        int i = 0;
        for (final DOMStoreThreePhaseCommitCohort cohort : cohorts) {
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
            for(final ListenableFuture<?> future : commitFutures) {
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
     */
    private ListenableFuture<?>[] commitAll() {
        final ListenableFuture<?>[] ops = new ListenableFuture<?>[cohorts.size()];
        int i = 0;
        for (final DOMStoreThreePhaseCommitCohort cohort : cohorts) {
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
    private void abortBlocking(final TransactionCommitFailedException originalCause) throws TransactionCommitFailedException {
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
     * Invokes abort on underlying cohorts and returns future which
     * completes once all abort on cohorts are completed.
     *
     * @return Future which will complete once all cohorts completed
     *         abort.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private ListenableFuture<Void> abortAsyncAll() {

        final ListenableFuture<?>[] ops = new ListenableFuture<?>[cohorts.size()];
        int i = 0;
        for (final DOMStoreThreePhaseCommitCohort cohort : cohorts) {
            ops[i++] = cohort.abort();
        }

        /*
         * We are returning all futures as list, not only succeeded ones in
         * order to fail composite future if any of them failed.
         * See Futures.allAsList for this description.
         */
        return (ListenableFuture) Futures.allAsList(ops);
    }
}
