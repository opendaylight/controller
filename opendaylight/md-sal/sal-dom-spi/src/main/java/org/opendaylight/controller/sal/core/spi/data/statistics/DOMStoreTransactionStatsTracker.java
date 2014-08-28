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
    private final AtomicLong totalSuccessfulReadCount = new AtomicLong();
    private final AtomicLong totalFailedReadCount = new AtomicLong();
    private final AtomicLong totalSuccessfulWriteCount = new AtomicLong();
    private final AtomicLong totalFailedWriteCount = new AtomicLong();
    private final AtomicLong totalSuccessfulDeleteCount = new AtomicLong();
    private final AtomicLong totalFailedDeleteCount = new AtomicLong();
    private final AtomicLong totalSuccessfulCommitCount = new AtomicLong();
    private final AtomicLong totalFailedCommitCount = new AtomicLong();
    private final AtomicLong totalFailedCanCommitCount = new AtomicLong();
    private final AtomicLong totalFailedPreCommitCount = new AtomicLong();

    public void incrementReadOnlyTransactionCount() {
        readOnlyTransactionCount.incrementAndGet();
    }

    public void incrementReadWriteTransactionCount() {
        readWriteTransactionCount.incrementAndGet();
    }

    public void incrementWriteOnlyTransactionCount() {
        writeOnlyTransactionCount.incrementAndGet();
    }

    public void incrementTotalSuccessfulReadCount() {
        totalSuccessfulReadCount.incrementAndGet();
    }

    public void incrementTotalFailedReadCount() {
        totalFailedReadCount.incrementAndGet();
    }

    public void incrementTotalSuccessfulWriteCount() {
        totalSuccessfulWriteCount.incrementAndGet();
    }

    public void incrementTotalFailedWriteCount() {
        totalFailedWriteCount.incrementAndGet();
    }

    public void incrementTotalSuccessfulDeleteCount() {
        totalSuccessfulDeleteCount.incrementAndGet();
    }

    public void incrementTotalFailedDeleteCount() {
        totalFailedDeleteCount.incrementAndGet();
    }

    public void incrementTotalSuccessfulCommitCount() {
        totalSuccessfulCommitCount.incrementAndGet();
    }

    public void incrementTotalFailedCommitCount() {
        totalFailedCommitCount.incrementAndGet();
    }

    public void incrementTotalFailedCanCommitCount() {
        totalFailedCanCommitCount.incrementAndGet();
    }

    public void incrementTotalFailedPreCommitCount() {
        totalFailedPreCommitCount.incrementAndGet();
    }

    public long getTotalSuccessfulReadCount() {
        return totalSuccessfulReadCount.get();
    }

    public long getTotalFailedReadCount() {
        return totalFailedReadCount.get();
    }

    public long getTotalSuccessfulWriteCount() {
        return totalSuccessfulWriteCount.get();
    }

    public long getTotalFailedWriteCount() {
        return totalFailedWriteCount.get();
    }

    public long getTotalSuccessfulDeleteCount() {
        return totalSuccessfulDeleteCount.get();
    }

    public long getTotalFailedDeleteCount() {
        return totalFailedDeleteCount.get();
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

    public long getTotalSuccessfulCommitCount() {
        return totalSuccessfulCommitCount.get();
    }

    public long getTotalFailedCommitCount() {
        return totalFailedCommitCount.get();
    }

    public long getTotalFailedCanCommitCount() {
        return totalFailedCanCommitCount.get();
    }

    public long getTotalFailedPreCommitCount() {
        return totalFailedPreCommitCount.get();
    }
}
