/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.dom.store.impl.jmx;

import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.controller.sal.core.spi.data.statistics.DOMStoreTransactionStatsMXBean;

/**
 * Implementation of the DOMStoreTransactionStatsMXBean interface.
 *
 * @author Thomas Pantelis
 */
public class InMemoryDataStoreTransactionStatsTracker implements DOMStoreTransactionStatsMXBean {

    private final AtomicLong readOnlyTransactionCount = new AtomicLong();
    private final AtomicLong readWriteTransactionCount = new AtomicLong();
    private final AtomicLong writeOnlyTransactionCount = new AtomicLong();
    private final AtomicLong successfulReadCount = new AtomicLong();
    private final AtomicLong failedReadCount = new AtomicLong();
    private final AtomicLong successfulWriteCount = new AtomicLong();
    private final AtomicLong failedWriteCount = new AtomicLong();
    private final AtomicLong successfulDeleteCount = new AtomicLong();
    private final AtomicLong failedDeleteCount = new AtomicLong();
    private final AtomicLong successfulCommitCount = new AtomicLong();
    private final AtomicLong failedCommitCount = new AtomicLong();
    private final AtomicLong canCommitOptimisticLockFailedCount = new AtomicLong();
    private final AtomicLong canCommitDataValidationFailedCount = new AtomicLong();
    private final AtomicLong failedPreCommitCount = new AtomicLong();
    private final AtomicLong submittedWriteTransactionCount = new AtomicLong();

    public void incrementReadOnlyTransactionCount() {
        readOnlyTransactionCount.incrementAndGet();
    }

    public void incrementReadWriteTransactionCount() {
        readWriteTransactionCount.incrementAndGet();
    }

    public void incrementWriteOnlyTransactionCount() {
        writeOnlyTransactionCount.incrementAndGet();
    }

    public void incrementSuccessfulReadCount() {
        successfulReadCount.incrementAndGet();
    }

    public void incrementFailedReadCount() {
        failedReadCount.incrementAndGet();
    }

    public void incrementSuccessfulWriteCount() {
        successfulWriteCount.incrementAndGet();
    }

    public void incrementFailedWriteCount() {
        failedWriteCount.incrementAndGet();
    }

    public void incrementSuccessfulDeleteCount() {
        successfulDeleteCount.incrementAndGet();
    }

    public void incrementFailedDeleteCount() {
        failedDeleteCount.incrementAndGet();
    }

    public void incrementSuccessfulCommitCount() {
        successfulCommitCount.incrementAndGet();
    }

    public void incrementFailedCommitCount() {
        failedCommitCount.incrementAndGet();
    }

    public void incrementCanCommitOptimisticLockFailedCount() {
        canCommitOptimisticLockFailedCount.incrementAndGet();
        incrementFailedCommitCount();
    }

    public void incrementCanCommitDataValidationFailedCount() {
        canCommitDataValidationFailedCount.incrementAndGet();
        incrementFailedCommitCount();
    }

    public void incrementFailedPreCommitCount() {
        failedPreCommitCount.incrementAndGet();
        incrementFailedCommitCount();
    }

    public void incrementSubmittedWriteTransactionCount() {
        submittedWriteTransactionCount.incrementAndGet();
    }

    @Override
    public long getSuccessfulReadCount() {
        return successfulReadCount.get();
    }

    @Override
    public long getFailedReadCount() {
        return failedReadCount.get();
    }

    @Override
    public long getSuccessfulWriteCount() {
        return successfulWriteCount.get();
    }

    @Override
    public long getFailedWriteCount() {
        return failedWriteCount.get();
    }

    @Override
    public long getSuccessfulDeleteCount() {
        return successfulDeleteCount.get();
    }

    @Override
    public long getFailedDeleteCount() {
        return failedDeleteCount.get();
    }

    @Override
    public long getReadOnlyTransactionCount() {
        return readOnlyTransactionCount.get();
    }

    @Override
    public long getReadWriteTransactionCount() {
        return readWriteTransactionCount.get();
    }

    @Override
    public long getWriteOnlyTransactionCount() {
        return writeOnlyTransactionCount.get();
    }

    @Override
    public long getSuccessfulCommitCount() {
        return successfulCommitCount.get();
    }

    @Override
    public long getFailedCommitCount() {
        return failedCommitCount.get();
    }

    @Override
    public long getCanCommitOptimisticLockFailedCount() {
        return canCommitOptimisticLockFailedCount.get();
    }

    @Override
    public long getCanCommitDataValidationFailedCount() {
        return canCommitDataValidationFailedCount.get();
    }

    @Override
    public long getFailedPreCommitCount() {
        return failedPreCommitCount.get();
    }

    @Override
    public long getSubmittedWriteTransactionCount() {
        return submittedWriteTransactionCount.get();
    }

    @Override
    public synchronized void clearStats() {
        readOnlyTransactionCount.set(0);
        readWriteTransactionCount.set(0);
        writeOnlyTransactionCount.set(0);
        successfulReadCount.set(0);
        failedReadCount.set(0);
        successfulWriteCount.set(0);
        failedWriteCount.set(0);
        successfulDeleteCount.set(0);
        failedDeleteCount.set(0);
        successfulCommitCount.set(0);
        failedCommitCount.set(0);
        canCommitOptimisticLockFailedCount.set(0);
        canCommitDataValidationFailedCount.set(0);
        failedPreCommitCount.set(0);
        submittedWriteTransactionCount.set(0);
    }
}
