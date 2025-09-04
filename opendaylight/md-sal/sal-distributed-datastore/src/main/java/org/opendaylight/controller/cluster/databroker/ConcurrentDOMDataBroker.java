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
import static org.opendaylight.mdsal.dom.spi.TransactionCommitFailedExceptionMapper.CAN_COMMIT_ERROR_MAPPER;
import static org.opendaylight.mdsal.dom.spi.TransactionCommitFailedExceptionMapper.COMMIT_ERROR_MAPPER;
import static org.opendaylight.mdsal.dom.spi.TransactionCommitFailedExceptionMapper.PRE_COMMIT_MAPPER;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.util.concurrent.AbstractFuture;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Map;
import java.util.concurrent.Executor;
import org.opendaylight.controller.cluster.datastore.exceptions.NoShardLeaderException;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.DataStoreUnavailableException;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.TransactionCommitFailedException;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.dom.spi.AbstractDOMDataBroker;
import org.opendaylight.mdsal.dom.spi.TransactionCommitFailedExceptionMapper;
import org.opendaylight.mdsal.dom.spi.store.DOMStore;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.util.DurationStatisticsTracker;
import org.opendaylight.yangtools.yang.common.Empty;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
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
@Component(service = DOMDataBroker.class, property = "type=default")
public class ConcurrentDOMDataBroker extends AbstractDOMDataBroker {
    private static final Logger LOG = LoggerFactory.getLogger(ConcurrentDOMDataBroker.class);
    private static final String CAN_COMMIT = "CAN_COMMIT";
    private static final String PRE_COMMIT = "PRE_COMMIT";
    private static final String COMMIT = "COMMIT";

    private final DurationStatisticsTracker commitStatsTracker;

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
        clientFutureCallbackExecutor = requireNonNull(listenableFutureExecutor);
        this.commitStatsTracker = requireNonNull(commitStatsTracker);
    }

    @Activate
    public ConcurrentDOMDataBroker(@Reference final DataBrokerCommitExecutor commitExecutor,
            @Reference(target = "(type=distributed-config)") final DOMStore configDatastore,
            @Reference(target = "(type=distributed-operational)") final DOMStore operDatastore) {
        this(Map.of(
            LogicalDatastoreType.CONFIGURATION, configDatastore, LogicalDatastoreType.OPERATIONAL, operDatastore),
            commitExecutor.executor(), commitExecutor.commitStatsTracker());
        LOG.info("DOM Data Broker started");
    }

    @Override
    @Deactivate
    public void close() {
        LOG.info("DOM Data Broker stopping");
        super.close();
        LOG.info("DOM Data Broker stopped");
    }

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(this).toString();
    }

    @Override
    protected FluentFuture<? extends CommitInfo> commit(final DOMDataTreeWriteTransaction transaction,
            final DOMStoreThreePhaseCommitCohort cohort) {

        checkArgument(transaction != null, "Transaction must not be null.");
        checkArgument(cohort != null, "Cohorts must not be null.");
        LOG.debug("Tx: {} is submitted for execution.", transaction.getIdentifier());

        final var clientSubmitFuture = new AsyncNotifyingSettableFuture(clientFutureCallbackExecutor);
        doCanCommit(clientSubmitFuture, transaction, cohort);
        return FluentFuture.from(clientSubmitFuture);
    }

    private void doCanCommit(final AsyncNotifyingSettableFuture clientSubmitFuture,
            final DOMDataTreeWriteTransaction transaction,
            final DOMStoreThreePhaseCommitCohort cohort) {
        final long startTime = System.nanoTime();

        Futures.addCallback(cohort.canCommit(), new FutureCallback<>() {
            @Override
            public void onSuccess(final Boolean result) {
                if (result == null || !result) {
                    onFailure(new TransactionCommitFailedException("Can Commit failed, no detailed cause available."));
                } else {
                    doPreCommit(startTime, clientSubmitFuture, transaction, cohort);
                }
            }

            @Override
            public void onFailure(final Throwable failure) {
                handleException(clientSubmitFuture, transaction, cohort, CAN_COMMIT, CAN_COMMIT_ERROR_MAPPER, failure);
            }
        }, MoreExecutors.directExecutor());
    }

    private void doPreCommit(final long startTime, final AsyncNotifyingSettableFuture clientSubmitFuture,
            final DOMDataTreeWriteTransaction transaction, final DOMStoreThreePhaseCommitCohort cohort) {
        Futures.addCallback(cohort.preCommit(), new FutureCallback<>() {
            @Override
            public void onSuccess(final Empty result) {
                doCommit(startTime, clientSubmitFuture, transaction, cohort);
            }

            @Override
            public void onFailure(final Throwable failure) {
                handleException(clientSubmitFuture, transaction, cohort, PRE_COMMIT, PRE_COMMIT_MAPPER, failure);
            }
        }, MoreExecutors.directExecutor());
    }

    private void doCommit(final long startTime, final AsyncNotifyingSettableFuture clientSubmitFuture,
            final DOMDataTreeWriteTransaction transaction, final DOMStoreThreePhaseCommitCohort cohort) {
        Futures.addCallback(cohort.commit(), new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                commitStatsTracker.addDuration(System.nanoTime() - startTime);
                clientSubmitFuture.set();
            }

            @Override
            public void onFailure(final Throwable throwable) {
                handleException(clientSubmitFuture, transaction, cohort, COMMIT, COMMIT_ERROR_MAPPER, throwable);
            }
        }, MoreExecutors.directExecutor());
    }

    private static void handleException(final AsyncNotifyingSettableFuture clientSubmitFuture,
            final DOMDataTreeWriteTransaction transaction, final DOMStoreThreePhaseCommitCohort cohort,
            final String phase, final TransactionCommitFailedExceptionMapper exMapper, final Throwable throwable) {
        if (clientSubmitFuture.isDone()) {
            // We must have had failures from multiple cohorts.
            return;
        }

        // Use debug instead of warn level here because this exception gets propagate back to the caller via the Future
        LOG.debug("Tx: {} Error during phase {}, starting Abort", transaction.getIdentifier(), phase, throwable);

        // Propagate the original exception
        final Exception e;
        if (throwable instanceof NoShardLeaderException) {
            e = new DataStoreUnavailableException(throwable.getMessage(), throwable);
        } else if (throwable instanceof Exception ex) {
            e = ex;
        } else {
            e = new RuntimeException("Unexpected error occurred", throwable);
        }
        clientSubmitFuture.setException(exMapper.apply(e));

        // abort
        Futures.addCallback(cohort.abort(), new FutureCallback<Empty>() {
            @Override
            public void onSuccess(final Empty result) {
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
    private static class AsyncNotifyingSettableFuture extends AbstractFuture<CommitInfo> {
        /**
         * ThreadLocal used to detect if the task completion thread is running the future listener Runnables.
         */
        private static final ThreadLocal<Empty> ON_TASK_COMPLETION_THREAD_TL = new ThreadLocal<>();

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
            ON_TASK_COMPLETION_THREAD_TL.set(Empty.value());
            try {
                return super.set(CommitInfo.empty());
            } finally {
                ON_TASK_COMPLETION_THREAD_TL.set(null);
            }
        }

        @Override
        protected boolean setException(final Throwable throwable) {
            ON_TASK_COMPLETION_THREAD_TL.set(Empty.value());
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

            @Override
            public String toString() {
                return MoreObjects.toStringHelper(this).add("delegate", delegate).toString();
            }
        }
    }
}
