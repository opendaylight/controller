/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static org.opendaylight.mdsal.dom.broker.TransactionCommitFailedExceptionMapper.CAN_COMMIT_ERROR_MAPPER;
import static org.opendaylight.mdsal.dom.broker.TransactionCommitFailedExceptionMapper.COMMIT_ERROR_MAPPER;
import static org.opendaylight.mdsal.dom.broker.TransactionCommitFailedExceptionMapper.PRE_COMMIT_MAPPER;

import akka.pattern.AskTimeoutException;
import com.google.common.annotations.Beta;
import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import org.opendaylight.controller.cluster.datastore.exceptions.NoShardLeaderException;
import org.opendaylight.controller.cluster.datastore.exceptions.ShardLeaderNotRespondingException;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.DataStoreUnavailableException;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.TransactionCommitFailedException;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.dom.broker.TransactionCommitFailedExceptionMapper;
import org.opendaylight.mdsal.dom.spi.store.DOMStore;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.util.DurationStatisticsTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ConcurrentDOMDataBroker commits transactions concurrently. The 3
 * commit phases (canCommit, preCommit, and commit) are performed serially and non-blocking
 * (ie async) per transaction but multiple transaction commits can run concurrent.
 *
 * @author Thomas Pantelis
 */
@Beta
public class ConcurrentDOMDataBroker extends AbstractDOMBroker {
    private static final Logger LOG = LoggerFactory.getLogger(ConcurrentDOMDataBroker.class);
    private static final String CAN_COMMIT = "CAN_COMMIT";
    private static final String PRE_COMMIT = "PRE_COMMIT";
    private static final String COMMIT = "COMMIT";

    private final DurationStatisticsTracker commitStatsTracker;
    private static DatastoreExceptionTracker datastoreExceptionTracker = DatastoreExceptionTracker.getInstance();

    /**
     * This executor is used to execute Future listener callback Runnables async.
     */
    private final Executor clientFutureCallbackExecutor;

    public ConcurrentDOMDataBroker(final Map<LogicalDatastoreType, DOMStore> datastores,
            final Executor listenableFutureExecutor) {
        this(datastores, listenableFutureExecutor, DurationStatisticsTracker.createConcurrent());
    }

    public ConcurrentDOMDataBroker(final Map<LogicalDatastoreType, DOMStore> datastores,
            final Executor listenableFutureExecutor, final DurationStatisticsTracker commitStatsTracker) {
        super(datastores);
        this.clientFutureCallbackExecutor = requireNonNull(listenableFutureExecutor);
        this.commitStatsTracker = requireNonNull(commitStatsTracker);
    }

    public DurationStatisticsTracker getCommitStatsTracker() {
        return commitStatsTracker;
    }

    @Override
    protected FluentFuture<? extends CommitInfo> commit(final DOMDataTreeWriteTransaction transaction,
            final Collection<DOMStoreThreePhaseCommitCohort> cohorts) {

        checkArgument(transaction != null, "Transaction must not be null.");
        checkArgument(cohorts != null, "Cohorts must not be null.");
        LOG.debug("Tx: {} is submitted for execution.", transaction.getIdentifier());

        if (cohorts.isEmpty()) {
            return CommitInfo.emptyFluentFuture();
        }

        final AsyncNotifyingSettableFuture clientSubmitFuture =
                new AsyncNotifyingSettableFuture(clientFutureCallbackExecutor);

        doCanCommit(clientSubmitFuture, transaction, cohorts);

        return FluentFuture.from(clientSubmitFuture).transform(ignored -> CommitInfo.empty(),
                MoreExecutors.directExecutor());
    }

    private void doCanCommit(final AsyncNotifyingSettableFuture clientSubmitFuture,
            final DOMDataTreeWriteTransaction transaction,
            final Collection<DOMStoreThreePhaseCommitCohort> cohorts) {

        final long startTime = System.nanoTime();

        final Iterator<DOMStoreThreePhaseCommitCohort> cohortIterator = cohorts.iterator();

        // Not using Futures.allAsList here to avoid its internal overhead.
        FutureCallback<Boolean> futureCallback = new FutureCallback<Boolean>() {
            @Override
            public void onSuccess(final Boolean result) {
                if (result == null || !result) {
                    handleException(clientSubmitFuture, transaction, cohorts, CAN_COMMIT, CAN_COMMIT_ERROR_MAPPER,
                            new TransactionCommitFailedException("Can Commit failed, no detailed cause available."));
                } else if (!cohortIterator.hasNext()) {
                    // All cohorts completed successfully - we can move on to the preCommit phase
                    doPreCommit(startTime, clientSubmitFuture, transaction, cohorts);
                } else {
                    Futures.addCallback(cohortIterator.next().canCommit(), this, MoreExecutors.directExecutor());
                }
            }

            @Override
            public void onFailure(final Throwable failure) {
                handleException(clientSubmitFuture, transaction, cohorts, CAN_COMMIT, CAN_COMMIT_ERROR_MAPPER, failure);
            }
        };

        Futures.addCallback(cohortIterator.next().canCommit(), futureCallback, MoreExecutors.directExecutor());
    }

    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification = "https://github.com/spotbugs/spotbugs/issues/811")
    private void doPreCommit(final long startTime, final AsyncNotifyingSettableFuture clientSubmitFuture,
            final DOMDataTreeWriteTransaction transaction,
            final Collection<DOMStoreThreePhaseCommitCohort> cohorts) {

        final Iterator<DOMStoreThreePhaseCommitCohort> cohortIterator = cohorts.iterator();

        // Not using Futures.allAsList here to avoid its internal overhead.
        FutureCallback<Void> futureCallback = new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void notUsed) {
                if (!cohortIterator.hasNext()) {
                    // All cohorts completed successfully - we can move on to the commit phase
                    doCommit(startTime, clientSubmitFuture, transaction, cohorts);
                } else {
                    ListenableFuture<Void> preCommitFuture = cohortIterator.next().preCommit();
                    Futures.addCallback(preCommitFuture, this, MoreExecutors.directExecutor());
                }
            }

            @Override
            public void onFailure(final Throwable failure) {
                handleException(clientSubmitFuture, transaction, cohorts, PRE_COMMIT, PRE_COMMIT_MAPPER, failure);
            }
        };

        ListenableFuture<Void> preCommitFuture = cohortIterator.next().preCommit();
        Futures.addCallback(preCommitFuture, futureCallback, MoreExecutors.directExecutor());
    }

    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification = "https://github.com/spotbugs/spotbugs/issues/811")
    private void doCommit(final long startTime, final AsyncNotifyingSettableFuture clientSubmitFuture,
            final DOMDataTreeWriteTransaction transaction,
            final Collection<DOMStoreThreePhaseCommitCohort> cohorts) {

        final Iterator<DOMStoreThreePhaseCommitCohort> cohortIterator = cohorts.iterator();

        // Not using Futures.allAsList here to avoid its internal overhead.
        FutureCallback<Void> futureCallback = new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void notUsed) {
                if (!cohortIterator.hasNext()) {
                    // All cohorts completed successfully - we're done.
                    commitStatsTracker.addDuration(System.nanoTime() - startTime);

                    clientSubmitFuture.set();
                } else {
                    ListenableFuture<Void> commitFuture = cohortIterator.next().commit();
                    Futures.addCallback(commitFuture, this, MoreExecutors.directExecutor());
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                handleException(clientSubmitFuture, transaction, cohorts, COMMIT, COMMIT_ERROR_MAPPER, throwable);
            }
        };

        ListenableFuture<Void> commitFuture = cohortIterator.next().commit();
        Futures.addCallback(commitFuture, futureCallback, MoreExecutors.directExecutor());
    }

    @SuppressFBWarnings(value = { "BC_UNCONFIRMED_CAST_OF_RETURN_VALUE", "UPM_UNCALLED_PRIVATE_METHOD" },
            justification = "Pertains to the assignment of the 'clientException' var. FindBugs flags this as an "
                + "uncomfirmed cast but the generic type in TransactionCommitFailedExceptionMapper is "
                + "TransactionCommitFailedException and thus should be deemed as confirmed."
                + "Also https://github.com/spotbugs/spotbugs/issues/811")
    private static void handleException(final AsyncNotifyingSettableFuture clientSubmitFuture,
            final DOMDataTreeWriteTransaction transaction,
            final Collection<DOMStoreThreePhaseCommitCohort> cohorts,
            final String phase, final TransactionCommitFailedExceptionMapper exMapper,
            final Throwable throwable) {

        if (clientSubmitFuture.isDone()) {
            // We must have had failures from multiple cohorts.
            return;
        }
        // The AskTimeoutException is handled specially, that the count is been maintained and monitored.
        // Alarm will be raised if the count hits the configured threshold level.
        if (throwable instanceof AskTimeoutException) {
            String transactionClassName = transaction.getClass().getName();
            String exceptionCounterKey = datastoreExceptionTracker.getExceptionTrackerCounterName(throwable,
                    transactionClassName);
            datastoreExceptionTracker.incrementAskTimeoutExceptionCounter(exceptionCounterKey);
        }

        // Use debug instead of warn level here because this exception gets propagate back to the caller via the Future
        LOG.debug("Tx: {} Error during phase {}, starting Abort", transaction.getIdentifier(), phase, throwable);

        // Transaction failed - tell all cohorts to abort.
        @SuppressWarnings("unchecked")
        ListenableFuture<Void>[] canCommitFutures = new ListenableFuture[cohorts.size()];
        int index = 0;
        for (DOMStoreThreePhaseCommitCohort cohort : cohorts) {
            canCommitFutures[index++] = cohort.abort();
        }

        // Propagate the original exception
        final Exception e;
        if (throwable instanceof NoShardLeaderException || throwable instanceof ShardLeaderNotRespondingException) {
            e = new DataStoreUnavailableException(throwable.getMessage(), throwable);
        } else if (throwable instanceof Exception) {
            e = (Exception)throwable;
        } else {
            e = new RuntimeException("Unexpected error occurred", throwable);
        }
        clientSubmitFuture.setException(exMapper.apply(e));

        ListenableFuture<List<Void>> combinedFuture = Futures.allAsList(canCommitFutures);
        Futures.addCallback(combinedFuture, new FutureCallback<List<Void>>() {
            @Override
            public void onSuccess(final List<Void> notUsed) {
                // Propagate the original exception to the client.
                LOG.debug("Tx: {} aborted successfully", transaction.getIdentifier());
            }

            @Override
            public void onFailure(final Throwable failure) {
                LOG.error("Tx: {} Error during Abort.", transaction.getIdentifier(), failure);
            }
        }, MoreExecutors.directExecutor());
    }

    /**
     * A settable future that uses an {@link Executor} to execute listener callback Runnables,
     * registered via {@link #addListener}, asynchronously when this future completes. This is
     * done to guarantee listener executions are off-loaded onto another thread to avoid blocking
     * the thread that completed this future, as a common use case is to pass an executor that runs
     * tasks in the same thread as the caller (ie MoreExecutors#sameThreadExecutor)
     * to {@link #addListener}.
     * FIXME: This class should probably be moved to yangtools common utils for re-usability and
     * unified with AsyncNotifyingListenableFutureTask.
     */
    private static class AsyncNotifyingSettableFuture extends AbstractFuture<Void> {

        /**
         * ThreadLocal used to detect if the task completion thread is running the future listener Runnables.
         */
        private static final ThreadLocal<Boolean> ON_TASK_COMPLETION_THREAD_TL = new ThreadLocal<>();

        private final Executor listenerExecutor;

        AsyncNotifyingSettableFuture(final Executor listenerExecutor) {
            this.listenerExecutor = requireNonNull(listenerExecutor);
        }

        @Override
        public void addListener(final Runnable listener, final Executor executor) {
            // Wrap the listener Runnable in a DelegatingRunnable. If the specified executor is one
            // that runs tasks in the same thread as the caller submitting the task
            // (e.g. {@link com.google.common.util.concurrent.MoreExecutors#sameThreadExecutor}) and
            // the listener is executed from the #set methods, then the DelegatingRunnable will detect
            // this via the ThreadLocal and submit the listener Runnable to the listenerExecutor.
            //
            // On the other hand, if this task is already complete, the call to ExecutionList#add in
            // superclass will execute the listener Runnable immediately and, since the ThreadLocal
            // won't be set, the DelegatingRunnable will run the listener Runnable inline.
            super.addListener(new DelegatingRunnable(listener, listenerExecutor), executor);
        }

        boolean set() {
            ON_TASK_COMPLETION_THREAD_TL.set(Boolean.TRUE);
            try {
                return super.set(null);
            } finally {
                ON_TASK_COMPLETION_THREAD_TL.set(null);
            }
        }

        @Override
        protected boolean setException(final Throwable throwable) {
            ON_TASK_COMPLETION_THREAD_TL.set(Boolean.TRUE);
            try {
                return super.setException(throwable);
            } finally {
                ON_TASK_COMPLETION_THREAD_TL.set(null);
            }
        }

        private static final class DelegatingRunnable implements Runnable {
            private final Runnable delegate;
            private final Executor executor;

            DelegatingRunnable(final Runnable delegate, final Executor executor) {
                this.delegate = requireNonNull(delegate);
                this.executor = requireNonNull(executor);
            }

            @Override
            public void run() {
                if (ON_TASK_COMPLETION_THREAD_TL.get() != null) {
                    // We're running on the task completion thread so off-load to the executor.
                    LOG.trace("Submitting ListenenableFuture Runnable from thread {} to executor {}",
                            Thread.currentThread().getName(), executor);
                    executor.execute(delegate);
                } else {
                    // We're not running on the task completion thread so run the delegate inline.
                    LOG.trace("Executing ListenenableFuture Runnable on this thread: {}",
                            Thread.currentThread().getName());
                    delegate.run();
                }
            }
        }
    }

    @Override
    public String toString() {
        return "Clustered ConcurrentDOMDataBroker";
    }
}
