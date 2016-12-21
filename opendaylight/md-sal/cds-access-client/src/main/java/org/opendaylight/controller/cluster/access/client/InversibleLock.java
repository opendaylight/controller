/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import com.google.common.annotations.Beta;
import com.google.common.base.Verify;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.StampedLock;

/**
 * A lock implementation which allows users to perform optimistic reads and validate them in a fashion similar
 * to {@link StampedLock}. In case a read is contented with a write, the read side will throw
 * an {@link InversibleLockException}, which the caller can catch and use to wait for the write to resolve.
 *
 * @author Robert Varga
 */
@Beta
public final class InversibleLock {
    private static final AtomicReferenceFieldUpdater<InversibleLock, CountDownLatch> LATCH_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(InversibleLock.class, CountDownLatch.class, "latch");

    private final StampedLock lock = new StampedLock();
    private volatile CountDownLatch latch;

    /**
     * Return a stamp for read validation.
     *
     * @return A stamp, which can be used with {@link #validate(long)}.
     * @throws InversibleLockException if this lock is currently write-locked
     */
    public long optimisticRead() {
        while (true) {
            final long stamp = lock.tryOptimisticRead();
            if (stamp != 0) {
                return stamp;
            }

            // Write-locked. Read the corresponding latch and if present report an exception, which will propagate
            // and force release of locks.
            final CountDownLatch local = latch;
            if (local != null) {
                throw new InversibleLockException(local);
            }

            // No latch present: retry optimistic lock
        }
    }

    public boolean validate(final long stamp) {
        return lock.validate(stamp);
    }

    public long writeLock() {
        final CountDownLatch local = new CountDownLatch(1);
        final boolean taken = LATCH_UPDATER.compareAndSet(this, null, local);
        Verify.verify(taken);

        return lock.writeLock();
    }

    public void unlockWrite(final long stamp) {
        final CountDownLatch local = LATCH_UPDATER.getAndSet(this, null);
        Verify.verifyNotNull(local);
        lock.unlockWrite(stamp);
        local.countDown();
    }

}
