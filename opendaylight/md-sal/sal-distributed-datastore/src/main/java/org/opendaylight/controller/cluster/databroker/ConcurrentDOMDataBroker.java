/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import org.opendaylight.controller.cluster.datastore.exceptions.NoShardLeaderException;
import org.opendaylight.controller.cluster.datastore.exceptions.ShardLeaderNotRespondingException;
import org.opendaylight.controller.md.sal.common.api.data.DataStoreUnavailableException;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.broker.impl.TransactionCommitFailedExceptionMapper;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohort;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohortRegistration;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohortRegistry;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.yangtools.util.DurationStatisticsTracker;
import org.opendaylight.yangtools.util.concurrent.MappingCheckedFuture;
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
public class ConcurrentDOMDataBroker extends AbstractDOMBroker implements DOMDataTreeCommitCohortRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(ConcurrentDOMDataBroker.class);
    private static final String CAN_COMMIT = "CAN_COMMIT";
    private static final String PRE_COMMIT = "PRE_COMMIT";
    private static final String COMMIT = "COMMIT";

    private final DurationStatisticsTracker commitStatsTracker;

    /**
     * This executor is used to execute Future listener callback Runnables async.
     */
    private final Executor clientFutureCallbackExecutor;

    public ConcurrentDOMDataBroker(final Map<LogicalDatastoreType, DOMStore> datastores, Executor listenableFutureExecutor) {
        this(datastores, listenableFutureExecutor, DurationStatisticsTracker.createConcurrent());
    }

    public ConcurrentDOMDataBroker(final Map<LogicalDatastoreType, DOMStore> datastores, Executor listenableFutureExecutor,
            DurationStatisticsTracker commitStatsTracker) {
        super(datastores);
        this.clientFutureCallbackExecutor = Preconditions.checkNotNull(listenableFutureExecutor);
        this.commitStatsTracker = Preconditions.checkNotNull(commitStatsTracker);
    }

    public DurationStatisticsTracker getCommitStatsTracker() {
        return commitStatsTracker;
    }

    @Override
    protected CheckedFuture<Void, TransactionCommitFailedException> submit(DOMDataWriteTransaction transaction,
            Collection<DOMStoreThreePhaseCommitCohort> cohorts) {

        Preconditions.checkArgument(transaction != null, "Transaction must not be null.");
        Preconditions.checkArgument(cohorts != null, "Cohorts must not be null.");
        LOG.debug("Tx: {} is submitted for execution.", transaction.getIdentifier());

        if(cohorts.isEmpty()){
            return Futures.immediateCheckedFuture(null);
        }

        final AsyncNotifyingSettableFuture clientSubmitFuture =
                new AsyncNotifyingSettableFuture(clientFutureCallbackExecutor);

        doCanCommit(clientSubmitFuture, transaction, cohorts);

        return MappingCheckedFuture.create(clientSubmitFuture,
                TransactionCommitFailedExceptionMapper.COMMIT_ERROR_MAPPER);
    }

    private void doCanCommit(final AsyncNotifyingSettableFuture clientSubmitFuture,
            final DOMDataWriteTransaction transaction,
            final Collection<DOMStoreThreePhaseCommitCohort> cohorts) {

        final long startTime = System.nanoTime();

        final Iterator<DOMStoreThreePhaseCommitCohort> cohortIterator = cohorts.iterator();

        // Not using Futures.allAsList here to avoid its internal overhead.
        FutureCallback<Boolean> futureCallback = new FutureCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (result == null || !result) {
                    handleException(clientSubmitFuture, transaction, cohorts,
                            CAN_COMMIT, TransactionCommitFailedExceptionMapper.CAN_COMMIT_ERROR_MAPPER,
                            new TransactionCommitFailedException(
                                            "Can Commit failed, no detailed cause available."));
                } else {
                    if(!cohortIterator.hasNext()) {
                        // All cohorts completed successfully - we can move on to the preCommit phase
                        doPreCommit(startTime, clientSubmitFuture, transaction, cohorts);
                    } else {
                        ListenableFuture<Boolean> canCommitFuture = cohortIterator.next().canCommit();
                        Futures.addCallback(canCommitFuture, this, MoreExecutors.directExecutor());
                    }
                }
            }

            @Override
            public void onFailure(Throwable t) {
                handleException(clientSubmitFuture, transaction, cohorts, CAN_COMMIT,
                        TransactionCommitFailedExceptionMapper.CAN_COMMIT_ERROR_MAPPER, t);
            }
        };

        ListenableFuture<Boolean> canCommitFuture = cohortIterator.next().canCommit();
        Futures.addCallback(canCommitFuture, futureCallback, MoreExecutors.directExecutor());
    }

    private void doPreCommit(final long startTime, final AsyncNotifyingSettableFuture clientSubmitFuture,
            final DOMDataWriteTransaction transaction,
            final Collection<DOMStoreThreePhaseCommitCohort> cohorts) {

        final Iterator<DOMStoreThreePhaseCommitCohort> cohortIterator = cohorts.iterator();

        // Not using Futures.allAsList here to avoid its internal overhead.
        FutureCallback<Void> futureCallback = new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void notUsed) {
                if(!cohortIterator.hasNext()) {
                    // All cohorts completed successfully - we can move on to the commit phase
                    doCommit(startTime, clientSubmitFuture, transaction, cohorts);
                } else {
                    ListenableFuture<Void> preCommitFuture = cohortIterator.next().preCommit();
                    Futures.addCallback(preCommitFuture, this, MoreExecutors.directExecutor());
                }
            }

            @Override
            public void onFailure(Throwable t) {
                handleException(clientSubmitFuture, transaction, cohorts, PRE_COMMIT,
                        TransactionCommitFailedExceptionMapper.PRE_COMMIT_MAPPER, t);
            }
        };

        ListenableFuture<Void> preCommitFuture = cohortIterator.next().preCommit();
        Futures.addCallback(preCommitFuture, futureCallback, MoreExecutors.directExecutor());
    }

    private void doCommit(final long startTime, final AsyncNotifyingSettableFuture clientSubmitFuture,
            final DOMDataWriteTransaction transaction,
            final Collection<DOMStoreThreePhaseCommitCohort> cohorts) {

        final Iterator<DOMStoreThreePhaseCommitCohort> cohortIterator = cohorts.iterator();

        // Not using Futures.allAsList here to avoid its internal overhead.
        FutureCallback<Void> futureCallback = new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void notUsed) {
                if(!cohortIterator.hasNext()) {
                    // All cohorts completed successfully - we're done.
                    commitStatsTracker.addDuration(System.nanoTime() - startTime);

                    clientSubmitFuture.set();
                } else {
                    ListenableFuture<Void> commitFuture = cohortIterator.next().commit();
                    Futures.addCallback(commitFuture, this, MoreExecutors.directExecutor());
                }
            }

            @Override
            public void onFailure(Throwable t) {
                handleException(clientSubmitFuture, transaction, cohorts, COMMIT,
                        TransactionCommitFailedExceptionMapper.COMMIT_ERROR_MAPPER, t);
            }
        };

        ListenableFuture<Void> commitFuture = cohortIterator.next().commit();
        Futures.addCallback(commitFuture, futureCallback, MoreExecutors.directExecutor());
    }

    private static void handleException(final AsyncNotifyingSettableFuture clientSubmitFuture,
            final DOMDataWriteTransaction transaction,
            final Collection<DOMStoreThreePhaseCommitCohort> cohorts,
            final String phase, final TransactionCommitFailedExceptionMapper exMapper,
            final Throwable t) {

        if (clientSubmitFuture.isDone()) {
            // We must have had failures from multiple cohorts.
            return;
        }

        LOG.warn("Tx: {} Error during phase {}, starting Abort", transaction.getIdentifier(), phase, t);
        final Exception e;
        if(t instanceof NoShardLeaderException || t instanceof ShardLeaderNotRespondingException) {
            e = new DataStoreUnavailableException(t.getMessage(), t);
        } else if (t instanceof Exception) {
            e = (Exception)t;
        } else {
            e = new RuntimeException("Unexpected error occurred", t);
        }

        final TransactionCommitFailedException clientException = exMapper.apply(e);

        // Transaction failed - tell all cohorts to abort.

        @SuppressWarnings("unchecked")
        ListenableFuture<Void>[] canCommitFutures = new ListenableFuture[cohorts.size()];
        int i = 0;
        for (DOMStoreThreePhaseCommitCohort cohort : cohorts) {
            canCommitFutures[i++] = cohort.abort();
        }

        ListenableFuture<List<Void>> combinedFuture = Futures.allAsList(canCommitFutures);
        Futures.addCallback(combinedFuture, new FutureCallback<List<Void>>() {
            @Override
            public void onSuccess(List<Void> notUsed) {
                // Propagate the original exception to the client.
                clientSubmitFuture.setException(clientException);
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.error("Tx: {} Error during Abort.", transaction.getIdentifier(), t);

                // Propagate the original exception as that is what caused the Tx to fail and is
                // what's interesting to the client.
                clientSubmitFuture.setException(clientException);
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
     *
     * FIXME: This class should probably be moved to yangtools common utils for re-usability and
     * unified with AsyncNotifyingListenableFutureTask.
     */
    private static class AsyncNotifyingSettableFuture extends AbstractFuture<Void> {

        /**
         * ThreadLocal used to detect if the task completion thread is running the future listener Runnables.
         */
        private static final ThreadLocal<Boolean> ON_TASK_COMPLETION_THREAD_TL = new ThreadLocal<>();

        private final Executor listenerExecutor;

        AsyncNotifyingSettableFuture(Executor listenerExecutor) {
            this.listenerExecutor = Preconditions.checkNotNull(listenerExecutor);
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
        protected boolean setException(Throwable throwable) {
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
                this.delegate = Preconditions.checkNotNull(delegate);
                this.executor = Preconditions.checkNotNull(executor);
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
    public <T extends DOMDataTreeCommitCohort> DOMDataTreeCommitCohortRegistration<T> registerCommitCohort(
            DOMDataTreeIdentifier path, T cohort) {
        DOMStore store = getTxFactories().get(toLegacy(path.getDatastoreType()));
        if (store instanceof DOMDataTreeCommitCohortRegistry) {
            return ((DOMDataTreeCommitCohortRegistry) store).registerCommitCohort(path, cohort);
        }
        throw new UnsupportedOperationException("Commit cohort is not supported for " + path);
    }

    private static LogicalDatastoreType toLegacy(org.opendaylight.mdsal.common.api.LogicalDatastoreType datastoreType) {
        switch (datastoreType) {
            case CONFIGURATION:
                return LogicalDatastoreType.CONFIGURATION;
            case OPERATIONAL:
                return LogicalDatastoreType.OPERATIONAL;
            default:
                throw new IllegalArgumentException("Unsupported data store type: " + datastoreType);
        }
    }

    @Override
    public String toString() {
        return "Clustered ConcurrentDOMDataBroker";
    }
}
