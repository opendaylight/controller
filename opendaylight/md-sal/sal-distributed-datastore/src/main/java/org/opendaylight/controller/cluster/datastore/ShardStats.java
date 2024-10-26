/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard.ShardStatsMXBean;

/**
 * Update interface for {@link ShardStatsMXBean}.
 */
final class ShardStats {
    private final AtomicLong readOnlyTransactionCount = new AtomicLong();
    private final AtomicLong readWriteTransactionCount = new AtomicLong();
    private final AtomicLong failedTransactionsCount = new AtomicLong();
    private final AtomicLong failedReadTransactionsCount = new AtomicLong();
    private final AtomicLong abortTransactionsCount = new AtomicLong();
    private final AtomicLong committedTransactionsCount = new AtomicLong();
    private final AtomicLong lastCommitted = new AtomicLong();

    void incrementCommittedTransactionCount() {
        final var now = System.currentTimeMillis();
        lastCommitted.setRelease(now);
        committedTransactionsCount.getAndIncrement();
    }

    long committedTransactionsCount() {
        return committedTransactionsCount.get();
    }

    void incrementReadOnlyTransactionCount() {
        readOnlyTransactionCount.getAndIncrement();
    }

    long readOnlyTransactionCount() {
        return readOnlyTransactionCount.get();
    }

    void incrementReadWriteTransactionCount() {
        readWriteTransactionCount.getAndIncrement();
    }

    long readWriteTransactionCount() {
        return readWriteTransactionCount.get();
    }

    void incrementFailedTransactionsCount() {
        failedTransactionsCount.getAndIncrement();
    }

    long failedTransactionsCount() {
        return failedTransactionsCount.get();
    }

    void incrementFailedReadTransactionsCount() {
        failedReadTransactionsCount.getAndIncrement();
    }

    long failedReadTransactionsCount() {
        return failedReadTransactionsCount.get();
    }

    void incrementAbortTransactionsCount() {
        abortTransactionsCount.getAndIncrement();
    }

    long abortTransactionsCount() {
        return abortTransactionsCount.get();
    }

    Instant lastCommittedTransactionTime() {
        return Instant.ofEpochMilli(lastCommitted.getAcquire());
    }
}
