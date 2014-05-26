/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.sal.common.util.Rpcs;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

/**
 * 
 * Implementation of blocking three phase commit coordinator, which which
 * supports coordination on multiple {@link DOMStoreThreePhaseCommitCohort}.
 * 
 * This implementation does not support cancelation of commit, neither
 * propagation of all exception types to the future.
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

    private static final Function<Iterable<Boolean>, Boolean> AND_FUNCTION = new Function<Iterable<Boolean>, Boolean>() {

        @Override
        public Boolean apply(final Iterable<Boolean> input) {
            return Iterables.all(input, Predicates.equalTo(Boolean.TRUE));
        }
    };

    private final ListeningExecutorService executor;

    public DOMDataCommitCoordinatorImpl(final ListeningExecutorService executor) {
        this.executor = Preconditions.checkNotNull(executor, "executor must not be null.");
    }

    @Override
    public ListenableFuture<RpcResult<TransactionStatus>> submit(final DOMDataWriteTransaction transaction,
            final Iterable<DOMStoreThreePhaseCommitCohort> cohorts, final Optional<DOMDataCommitErrorListener> listener) {
        Preconditions.checkArgument(transaction != null, "Transaction must not be null.");
        Preconditions.checkArgument(cohorts != null, "Cohorts must not be null.");
        Preconditions.checkArgument(listener != null, "Listener must not be null");
        LOG.debug("Tx: {} is submitted for execution.", transaction.getIdentifier());
        return executor.submit(new CommitCoordinationTask(transaction, cohorts, listener));
    }

    private static class CommitCoordinationTask implements Callable<RpcResult<TransactionStatus>> {

        private final DOMDataWriteTransaction tx;
        private final Iterable<DOMStoreThreePhaseCommitCohort> cohorts;
        private final Optional<DOMDataCommitErrorListener> listener;

        public CommitCoordinationTask(final DOMDataWriteTransaction transaction,
                final Iterable<DOMStoreThreePhaseCommitCohort> cohorts,
                final Optional<DOMDataCommitErrorListener> listener) {
            this.tx = Preconditions.checkNotNull(transaction, "transaction must not be null");
            this.cohorts = Preconditions.checkNotNull(cohorts, "cohorts must not be null");
            this.listener = Preconditions.checkNotNull(listener, "listener must not be null");
        }

        @Override
        public RpcResult<TransactionStatus> call() {

            try {
                Boolean canCommit = canCommit().get();

                if (canCommit) {
                    try {
                        preCommit().get();
                        try {
                            commit().get();
                            LOG.debug("Tx: {} Is commited.", tx.getIdentifier());
                            return Rpcs.getRpcResult(true, TransactionStatus.COMMITED,
                                    Collections.<RpcError> emptySet());

                        } catch (InterruptedException | ExecutionException e) {
                            LOG.error("Tx: {} Error during commit", tx.getIdentifier(), e);
                            notifyTxFailed(e);
                        }

                    } catch (InterruptedException | ExecutionException e) {
                        LOG.warn("Tx: {} Error during preCommit, starting Abort", tx.getIdentifier(), e);
                        notifyTxFailed(e);
                    }
                } else {
                    LOG.info("Tx: {} Did not pass canCommit phase.", tx.getIdentifier());
                    abort().get();
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.warn("Tx: {} Error during canCommit, starting Abort", tx.getIdentifier(), e);
                notifyTxFailed(e);
            }
            try {
                abort().get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Tx: {} Error during abort", tx.getIdentifier(), e);
            }
            return Rpcs.getRpcResult(false, TransactionStatus.FAILED, Collections.<RpcError> emptySet());
        }

        private ListenableFuture<Void> preCommit() {
            LOG.debug("Transaction {}: PreCommit Started ", tx.getIdentifier());
            Builder<ListenableFuture<Void>> ops = ImmutableList.builder();
            for (DOMStoreThreePhaseCommitCohort cohort : cohorts) {
                ops.add(cohort.preCommit());
            }
            /*
             * We are returing all futures as list, not only succeeded ones in
             * order to fail composite future if any of them failed.
             * 
             * See Futures.allAsList for this description.
             */
            @SuppressWarnings({ "unchecked", "rawtypes" })
            ListenableFuture<Void> compositeResult = (ListenableFuture) Futures.allAsList(ops.build());
            return compositeResult;
        }

        private ListenableFuture<Void> commit() {
            LOG.debug("Transaction {}: Commit Started ", tx.getIdentifier());
            Builder<ListenableFuture<Void>> ops = ImmutableList.builder();
            for (DOMStoreThreePhaseCommitCohort cohort : cohorts) {
                ops.add(cohort.commit());
            }
            /*
             * We are returing all futures as list, not only succeeded ones in
             * order to fail composite future if any of them failed.
             * 
             * See Futures.allAsList for this description.
             */
            @SuppressWarnings({ "unchecked", "rawtypes" })
            ListenableFuture<Void> compositeResult = (ListenableFuture) Futures.allAsList(ops.build());
            return compositeResult;
        }

        private ListenableFuture<Boolean> canCommit() {
            LOG.debug("Transaction {}: CanCommit Started ", tx.getIdentifier());
            Builder<ListenableFuture<Boolean>> canCommitOperations = ImmutableList.builder();
            for (DOMStoreThreePhaseCommitCohort cohort : cohorts) {
                canCommitOperations.add(cohort.canCommit());
            }
            ListenableFuture<List<Boolean>> allCanCommits = Futures.allAsList(canCommitOperations.build());
            return Futures.transform(allCanCommits, AND_FUNCTION);
        }

        private ListenableFuture<Void> abort() {
            LOG.debug("Transaction {}: Abort Started ", tx.getIdentifier());
            Builder<ListenableFuture<Void>> ops = ImmutableList.builder();
            for (DOMStoreThreePhaseCommitCohort cohort : cohorts) {
                ops.add(cohort.abort());
            }
            /*
             * We are returing all futures as list, not only succeeded ones in
             * order to fail composite future if any of them failed.
             * 
             * See Futures.allAsList for this description.
             */
            @SuppressWarnings({ "unchecked", "rawtypes" })
            ListenableFuture<Void> compositeResult = (ListenableFuture) Futures.allAsList(ops.build());
            return compositeResult;
        };

        private void notifyTxFailed(final Throwable cause) {
            if (listener.isPresent()) {
                try {
                    listener.get().onCommitFailed(tx, cause);
                } catch (Exception e) {
                    LOG.error("Unhandled exception during invoking Error listener {} for transaction {}",
                            listener.get(), tx, e);
                }
            }
        }

    }

}
