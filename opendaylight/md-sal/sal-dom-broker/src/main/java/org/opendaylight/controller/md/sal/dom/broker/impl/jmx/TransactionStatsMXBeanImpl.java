/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.dom.broker.impl.jmx;

import java.util.concurrent.atomic.AtomicLong;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.opendaylight.controller.md.sal.common.util.jmx.MBeanRegistrar;
import org.opendaylight.yangtools.util.DurationStatisticsTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Implementation of the TransactionStatsMXBean interface,
 *
 * @author Thomas Pantelis
 */
public class TransactionStatsMXBeanImpl implements AutoCloseable, TransactionStatsMXBean,
        TransactionStatsTracker {

    private static final Logger LOG = LoggerFactory.getLogger(TransactionStatsMXBeanImpl.class);

    private final AtomicLong successfulReads = new AtomicLong();
    private final AtomicLong failedReads = new AtomicLong();

    private final AtomicLong successfulWrites = new AtomicLong();
    private final AtomicLong failedWrites = new AtomicLong();

    private final AtomicLong successfulDeletes = new AtomicLong();
    private final AtomicLong failedDeletes = new AtomicLong();

    private final AtomicLong canCommitPhaseFailures = new AtomicLong();
    private final AtomicLong preCommitPhaseFailures = new AtomicLong();
    private final AtomicLong commitPhaseFailures = new AtomicLong();
    private final AtomicLong optimisticLockFailures = new AtomicLong();

    private volatile ObjectName objectName;

    @SuppressWarnings("rawtypes")
    private final FutureCallback readStatsUpdater = new FutureCallback()
    {
        @Override
        public void onSuccess(Object notUsed) {
            successfulReads.incrementAndGet();
        }

        @Override
        public void onFailure(Throwable t) {
            failedReads.incrementAndGet();
        }
    };

    // We're using the synchronized version as only 1 thread will update it so no contention will
    // occur. If commits are multi-threaded in the future, we should change this to the
    // concurrent version.
    private final DurationStatisticsTracker commitDurationStatsTracker =
                                                DurationStatisticsTracker.createSynchronized();

    @Override
    public long getTotalCommits() {
        return commitDurationStatsTracker.getTotalDurations();
    }

    @Override
    public String getLongestCommitTime() {
        return commitDurationStatsTracker.getDisplayableLongestDuration();
    }

    @Override
    public String getShortestCommitTime() {
        return commitDurationStatsTracker.getDisplayableShortestDuration();
    }

    @Override
    public String getAverageCommitTime() {
        return commitDurationStatsTracker.getDisplayableAverageDuration();
    }

    @Override
    public long getCanCommitPhaseFailures() {
        return canCommitPhaseFailures.get();
    }

    @Override
    public long getPreCommitPhaseFailures() {
        return preCommitPhaseFailures.get();
    }

    @Override
    public long getCommitPhaseFailures() {
        return commitPhaseFailures.get();
    }

    @Override
    public long getOptimisticLockFailures() {
        return optimisticLockFailures.get();
    }

    @Override
    public long getFailedDeletes() {
        return failedDeletes.get();
    }

    @Override
    public long getSuccessfulDeletes() {
        return successfulDeletes.get();
    }

    @Override
    public long getFailedWrites() {
        return failedWrites.get();
    }

    @Override
    public long getSuccessfulWrites() {
        return successfulWrites.get();
    }

    @Override
    public long getFailedReads() {
        return failedReads.get();
    }

    @Override
    public long getSuccessfulReads() {
        return successfulReads.get();
    }

    @Override
    public synchronized void clearStats() {
        commitDurationStatsTracker.reset();
        successfulReads.set(0);
        failedReads.set(0);
        successfulWrites.set(0);
        failedWrites.set(0);
        successfulDeletes.set(0);
        failedDeletes.set(0);
        canCommitPhaseFailures.set(0);
        preCommitPhaseFailures.set(0);
        commitPhaseFailures.set(0);
        optimisticLockFailures.set(0);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void updateReadStatsAsync(ListenableFuture<?> readFuture) {
        Futures.addCallback(readFuture, readStatsUpdater);
    }

    @Override
    public void addSuccessfulWrites(int n) {
        if(n > 0) {
            successfulWrites.incrementAndGet();
        }
    }

    @Override
    public void incrementFailedWrites() {
        failedWrites.incrementAndGet();
    }

    @Override
    public void addSuccessfulDeletes(int n) {
        if(n > 0) {
            successfulDeletes.incrementAndGet();
        }
    }

    @Override
    public void incrementFailedDeletes() {
        failedDeletes.incrementAndGet();
    }

    @Override
    public void incrementCanCommitPhaseFailures() {
        canCommitPhaseFailures.incrementAndGet();
    }

    @Override
    public void incrementPreCommitPhaseFailures() {
        preCommitPhaseFailures.incrementAndGet();
    }

    @Override
    public void incrementCommitPhaseFailures() {
        commitPhaseFailures.incrementAndGet();
    }

    @Override
    public void incrementOptimisticLockFailures() {
        optimisticLockFailures.incrementAndGet();
    }

    @Override
    public void addCommitDuration(long elapsedTime) {
        commitDurationStatsTracker.addDuration(elapsedTime);
    }

    public void register(String mBeanType) {
        try {
            objectName = MBeanRegistrar.buildMBeanObjectName(
                    "TransactionStats", mBeanType, null);
            MBeanRegistrar.registerMBean(this, objectName);
        } catch(MalformedObjectNameException e) {
            LOG.error("Error building MBean ObjectName", e);
        }
    }

    @Override
    public void close() {
        MBeanRegistrar.unregisterMBean(objectName);
    }
}
