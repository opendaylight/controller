/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.dom.broker.impl;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.InsufficientCapacityException;
import com.lmax.disruptor.PhasedBackoffWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.yangtools.util.DurationStatisticsTracker;
import org.opendaylight.yangtools.util.concurrent.DefaultSettableFuture;
import org.opendaylight.yangtools.util.concurrent.MappingCheckedFuture;
import org.opendaylight.yangtools.util.concurrent.SettableFuture;
import org.opendaylight.yangtools.util.concurrent.ThreadOffloadingSettableFuture;
import org.opendaylight.yangtools.util.concurrent.ThreadProtectingSettableFuture;
import org.opendaylight.yangtools.util.concurrent.TrackingThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of blocking three phase commit coordinator, which which
 * supports coordination on multiple {@link DOMStoreThreePhaseCommitCohort}.
 *
 * This implementation does not support cancellation of commit,
 *
 * In order to advance to next phase of three phase commit all subtasks of
 * previous step must be finish.
 *
 * This executor does not have an upper bound on subtask timeout.
 */
public class SerializedDOMDataBroker extends AbstractDOMDataBroker {
    private static final Logger LOG = LoggerFactory.getLogger(SerializedDOMDataBroker.class);
    private static final WaitStrategy DEFAULT_STRATEGY = PhasedBackoffWaitStrategy.withLock(1L, 30L, TimeUnit.MILLISECONDS);

    private final DurationStatisticsTracker commitStatsTracker = DurationStatisticsTracker.createConcurrent();
    private final Disruptor<CommitCoordinationEvent> disruptor;
    private final TrackingThreadFactory threadFactory;
    private final ExecutorService commitExecutor;
    private final Executor notificationExecutor;

    private final EventHandler<CommitCoordinationEvent> coordinateCommit = new EventHandler<CommitCoordinationEvent>() {
        @Override
        public void onEvent(final CommitCoordinationEvent event, final long sequence, final boolean endOfBatch) {
            try {
                event.coordinateCommit();
                event.success();
            } catch (Exception e) {
                event.failure(e);
            } finally {
                commitStatsTracker.addDuration(System.nanoTime() - event.getStartTime());
            }
        }
    };

    /**
     * Construct DOMDataCommitCoordinator which uses supplied executor to
     * process commit coordinations.
     */
    @SuppressWarnings("unchecked")
    public SerializedDOMDataBroker(final Map<LogicalDatastoreType, DOMStore> datastores,
            final Executor notificationExecutor, final TrackingThreadFactory threadFactory, final int queueDepth) {
        super(datastores);

        this.notificationExecutor = Preconditions.checkNotNull(notificationExecutor, "executor must not be null.");
        this.threadFactory = Preconditions.checkNotNull(threadFactory, "thread factory must not be null.");
        this.commitExecutor = Executors.newSingleThreadExecutor(threadFactory);

        disruptor = new Disruptor<CommitCoordinationEvent>(CommitCoordinationEvent.FACTORY, queueDepth, commitExecutor,
                ProducerType.MULTI, DEFAULT_STRATEGY);
        disruptor.handleEventsWith(coordinateCommit);
        disruptor.start();
    }

    public DurationStatisticsTracker getCommitStatsTracker() {
        return commitStatsTracker;
    }

    @Override
    protected CheckedFuture<Void,TransactionCommitFailedException> submit(final DOMDataWriteTransaction transaction,
            final Collection<DOMStoreThreePhaseCommitCohort> cohorts) {
        Preconditions.checkArgument(transaction != null, "Transaction must not be null.");
        Preconditions.checkArgument(cohorts != null, "Cohorts must not be null.");
        LOG.debug("Tx: {} is submitted for execution.", transaction.getIdentifier());


        final long seq;
        try {
            seq = disruptor.getRingBuffer().tryNext();
        } catch (InsufficientCapacityException e) {
            LOG.error("Ring buffer {} is full, transaction submit failed", disruptor, e);
            return Futures.immediateFailedCheckedFuture(
                new TransactionCommitFailedException(
                    "Could not submit the commit task - the commit queue capacity has been exceeded.", e));
        }

        // Base future
        final SettableFuture<Void> simple = DefaultSettableFuture.create();

        // Do not allow get() from notification thread
        final SettableFuture<Void> protect = ThreadProtectingSettableFuture.create(simple, threadFactory);

        // Do run listeners on the commit thread
        final SettableFuture<Void> offload = ThreadOffloadingSettableFuture.create(protect, threadFactory,
            notificationExecutor);

        final CommitCoordinationEvent event = disruptor.get(seq);
        event.initialize(transaction, cohorts, offload);
        disruptor.getRingBuffer().publish(seq);

        return MappingCheckedFuture.create(offload,
            TransactionCommitFailedExceptionMapper.COMMIT_ERROR_MAPPER);
    }

    @Override
    public void close() {
        try {
            super.close();
        } finally {
            disruptor.shutdown();
            commitExecutor.shutdown();
        }
    }
}
