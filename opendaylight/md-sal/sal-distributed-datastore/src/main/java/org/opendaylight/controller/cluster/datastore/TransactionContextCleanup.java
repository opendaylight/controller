/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import java.lang.ref.Cleaner;
import java.lang.ref.Cleaner.Cleanable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class that closes remote transactions for a TransactionContext when it's
 * garbage collected. This is used for read-only transactions as they're not explicitly closed
 * by clients. So the only way to detect that a transaction is no longer in use and it's safe
 * to clean up is when it's garbage collected. It's inexact as to when an instance will be GC'ed
 * but TransactionProxy instances should generally be short-lived enough to avoid being moved
 * to the old generation space and thus should be cleaned up in a timely manner as the GC
 * runs on the young generation (eden, swap1...) space much more frequently.
 */
final class TransactionContextCleanup {
    private static final class Cleanup implements Runnable {
        private final TransactionContext cleanup;

        Cleanup(final TransactionContext cleanup) {
            this.cleanup = requireNonNull(cleanup);
        }

        @Override
        public void run() {
            LOG.trace("Cleaning up {} Tx actors", cleanup);
            TRACKING.remove(cleanup);
            cleanup.closeTransaction();
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(TransactionContextCleanup.class);
    private static final Cleaner CLEANER = Cleaner.create();
    private static final Map<TransactionContext, Cleanable> TRACKING = new ConcurrentHashMap<>();

    private TransactionContextCleanup() {
        // Hidden on purpose
    }

    static void track(final TransactionProxy referent, final TransactionContext cleanup) {
        TRACKING.put(cleanup, CLEANER.register(referent, new Cleanup(cleanup)));
    }

    static void untrack(final TransactionContext referent) {
        final Cleanable cleanup = TRACKING.remove(referent);
        if (cleanup != null) {
            cleanup.clean();
        }
    }
}
