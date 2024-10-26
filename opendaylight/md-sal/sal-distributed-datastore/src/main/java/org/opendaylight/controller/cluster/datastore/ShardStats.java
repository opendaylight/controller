/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.time.Instant;
import org.opendaylight.controller.cluster.datastore.jmx.mbeans.shard.ShardStatsMXBean;

/**
 * Update interface for {@link ShardStatsMXBean}.
 */
final class ShardStats {
    private static final VarHandle RO_COUNT;
    private static final VarHandle RW_COUNT;
    private static final VarHandle FAILED_TX;
    private static final VarHandle FAILED_READ;
    private static final VarHandle ABORTED_COUNT;
    private static final VarHandle COMMITTED_COUNT;
    private static final VarHandle LAST_COMMITTED;

    static {
        final var lookup = MethodHandles.lookup();

        try {
            RO_COUNT = lookup.findVarHandle(ShardStats.class, "readOnlyCount", long.class);
            RW_COUNT = lookup.findVarHandle(ShardStats.class, "readWriteCount", long.class);
            FAILED_TX = lookup.findVarHandle(ShardStats.class, "failedTransactions", long.class);
            FAILED_READ = lookup.findVarHandle(ShardStats.class, "railedReads", long.class);
            ABORTED_COUNT = lookup.findVarHandle(ShardStats.class, "abortedCount", long.class);
            COMMITTED_COUNT = lookup.findVarHandle(ShardStats.class, "committedCount", long.class);
            LAST_COMMITTED = lookup.findVarHandle(ShardStats.class, "lastCommitted", long.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @SuppressWarnings("unused")
    private volatile long readOnlyCount;
    @SuppressWarnings("unused")
    private volatile long readWriteCount;
    @SuppressWarnings("unused")
    private volatile long failedTransactions;
    @SuppressWarnings("unused")
    private volatile long railedReads;
    @SuppressWarnings("unused")
    private volatile long abortedCount;
    @SuppressWarnings("unused")
    private volatile long committedCount;
    @SuppressWarnings("unused")
    private volatile long lastCommitted;

    void incrementCommittedTransactionCount() {
        final var now = System.currentTimeMillis();
        LAST_COMMITTED.setRelease(this, now);
        increment(COMMITTED_COUNT);
    }

    long committedTransactionsCount() {
        return get(COMMITTED_COUNT);
    }

    void incrementReadOnlyTransactionCount() {
        increment(RO_COUNT);
    }

    long readOnlyTransactionCount() {
        return get(RO_COUNT);
    }

    void incrementReadWriteTransactionCount() {
        increment(RW_COUNT);
    }

    long readWriteTransactionCount() {
        return get(RW_COUNT);
    }

    void incrementFailedTransactionsCount() {
        increment(FAILED_TX);
    }

    long failedTransactionsCount() {
        return get(FAILED_TX);
    }

    void incrementFailedReadTransactionsCount() {
        increment(FAILED_READ);
    }

    long failedReadTransactionsCount() {
        return get(FAILED_READ);
    }

    void incrementAbortTransactionsCount() {
        increment(ABORTED_COUNT);
    }

    long abortTransactionsCount() {
        return get(ABORTED_COUNT);
    }

    Instant lastCommittedTransactionTime() {
        return Instant.ofEpochMilli((long) LAST_COMMITTED.getAcquire(this));
    }

    private void increment(final VarHandle vh) {
        vh.getAndAdd(this, 1L);
    }

    private long get(final VarHandle vh) {
        return (long) vh.get(this);
    }
}
