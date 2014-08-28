/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.sal.core.spi.data.statistics;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Class that tracks various transaction statistics for a data store.
 *
 * @author Thomas Pantelis
 */
public class DOMStoreTransactionStatsTracker {

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

    public long getSuccessfulReadCount() {
        return successfulReadCount.get();
    }

    public long getFailedReadCount() {
        return failedReadCount.get();
    }

    public long getSuccessfulWriteCount() {
        return successfulWriteCount.get();
    }

    public long getFailedWriteCount() {
        return failedWriteCount.get();
    }

    public long getSuccessfulDeleteCount() {
        return successfulDeleteCount.get();
    }

    public long getFailedDeleteCount() {
        return failedDeleteCount.get();
    }

    public long getReadOnlyTransactionCount() {
        return readOnlyTransactionCount.get();
    }

    public long getReadWriteTransactionCount() {
        return readWriteTransactionCount.get();
    }

    public long getWriteOnlyTransactionCount() {
        return writeOnlyTransactionCount.get();
    }

    public long getSuccessfulCommitCount() {
        return successfulCommitCount.get();
    }

    public long getFailedCommitCount() {
        return failedCommitCount.get();
    }

    public long getCanCommitOptimisticLockFailedCount() {
        return canCommitOptimisticLockFailedCount.get();
    }

    public long getCanCommitDataValidationFailedCount() {
        return canCommitDataValidationFailedCount.get();
    }

    public long getFailedPreCommitCount() {
        return failedPreCommitCount.get();
    }

    public long getSubmittedWriteTransactionCount() {
        return submittedWriteTransactionCount.get();
    }

    public void clear() {
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
