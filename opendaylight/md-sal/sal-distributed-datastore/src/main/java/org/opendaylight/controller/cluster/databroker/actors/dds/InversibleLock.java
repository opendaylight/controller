/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import com.google.common.base.Verify;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.StampedLock;

final class InversibleLock {
    private static final AtomicReferenceFieldUpdater<InversibleLock, CountDownLatch> LATCH_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(InversibleLock.class, CountDownLatch.class, "latch");

    private final StampedLock lock = new StampedLock();
    private volatile CountDownLatch latch;

    long optimisticRead() {
        while (true) {
            final long stamp = lock.tryOptimisticRead();
            if (stamp != 0) {
                return stamp;
            }

            // Write-locked. Read the corresponding latch and if present report an exception, which will propagate
            // and force release of locks.
            final CountDownLatch local = latch;
            if (local != null) {
                throw new InversibleLockException(latch);
            }

            // No latch present: retry optimistic lock
        }
    }

    long writeLock() {
        final CountDownLatch local = new CountDownLatch(1);
        final boolean taken = LATCH_UPDATER.compareAndSet(this, null, local);
        Verify.verify(taken);

        return lock.writeLock();
    }

    void unlock(final long stamp) {
        final CountDownLatch local = LATCH_UPDATER.getAndSet(this, null);
        Verify.verifyNotNull(local);
        lock.unlock(stamp);
        local.countDown();
    }

    boolean validate(final long stamp) {
        return lock.validate(stamp);
    }
}
