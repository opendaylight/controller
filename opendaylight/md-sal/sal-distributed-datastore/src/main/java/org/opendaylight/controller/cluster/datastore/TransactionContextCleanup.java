/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import java.lang.ref.Cleaner;
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
    private static final Logger LOG = LoggerFactory.getLogger(TransactionContextCleanup.class);

    private static final Cleaner CLEANER = Cleaner.create();

    private static Map<TransactionContext, Cleaner.Cleanable> CACHE = new ConcurrentHashMap<>();

    private TransactionContextCleanup() {

    }

    static void track(final TransactionProxy referent, final TransactionContext cleanup) {
        Cleaner.Cleanable cleanable = CLEANER.register(referent, new Cleanup(cleanup));
        CACHE.put(cleanup, cleanable);
    }

    static void untrack(final TransactionContext cleanup) {
        if (CACHE.containsKey(cleanup)) {
            CACHE.remove(cleanup).clean();
        }
    }

    private static class Cleanup implements Runnable {

        private TransactionContext cleanup;

        Cleanup(TransactionContext cleanup) {
            this.cleanup = cleanup;
        }

        @Override
        public void run() {
            LOG.trace("Cleaning up {} Tx actors", cleanup);
            CACHE.remove(cleanup);
            cleanup.closeTransaction();
        }
    }
}