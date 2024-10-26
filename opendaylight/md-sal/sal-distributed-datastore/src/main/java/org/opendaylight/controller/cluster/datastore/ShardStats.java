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
import java.util.concurrent.atomic.LongAdder;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard.ShardStatsMXBean;

/**
 * Update interface for {@link ShardStatsMXBean}.
 */
final class ShardStats {
    final LongAdder readOnlyTransactionCount = new LongAdder();
    final LongAdder readWriteTransactionCount = new LongAdder();
    final LongAdder failedTransactionsCount = new LongAdder();
    final LongAdder failedReadTransactionsCount = new LongAdder();
    final LongAdder abortTransactionsCount = new LongAdder();
    final LongAdder committedTransactionsCount = new LongAdder();

    private final AtomicLong lastCommittedTransactionTime = new AtomicLong(System.nanoTime());

    long incrementCommittedTransactionCount() {
        final var now = System.nanoTime();
        committedTransactionsCount.increment();
        lastCommittedTransactionTime.setRelease(now);
        return now;
    }

    void incrementReadOnlyTransactionCount() {
        readOnlyTransactionCount.increment();
    }

    void incrementReadWriteTransactionCount() {
        readWriteTransactionCount.increment();
    }

    void incrementFailedTransactionsCount() {
        failedTransactionsCount.increment();
    }

    void incrementFailedReadTransactionsCount() {
        failedReadTransactionsCount.increment();
    }

    void incrementAbortTransactionsCount() {
        abortTransactionsCount.increment();
    }

    Instant lastCommittedTransactionTime() {
        final var last = lastCommittedTransactionTime.getAcquire();
        final var now = System.nanoTime();
        return Instant.now().minusNanos(now - last);
    }
}
