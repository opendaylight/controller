/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import static com.google.common.base.Verify.verify;
import static com.google.common.base.Verify.verifyNotNull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.StampedLock;

/**
 * A lock implementation which allows users to perform optimistic reads and validate them in a fashion similar
 * to {@link StampedLock}. In case a read is contented with a write, the read side will throw
 * an {@link InversibleLockException}, which the caller can catch and use to wait for the write to resolve.
 */
public final class InversibleLock {
    private static final VarHandle LATCH;

    static {
        try {
            LATCH = MethodHandles.lookup().findVarHandle(InversibleLock.class, "latch", CountDownLatch.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final StampedLock lock = new StampedLock();

    @SuppressFBWarnings(value = "UWF_UNWRITTEN_FIELD",
        justification = "https://github.com/spotbugs/spotbugs/issues/2749")
    private volatile CountDownLatch latch;

    /**
     * Creates a new unlocked lock.
     */
    public InversibleLock() {
        // Nothing else
    }

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
            final var local = latch;
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
        verify(LATCH.compareAndSet(this, null, new CountDownLatch(1)));
        return lock.writeLock();
    }

    public void unlockWrite(final long stamp) {
        final var local = verifyNotNull((CountDownLatch) LATCH.getAndSet(this, null));
        lock.unlockWrite(stamp);
        local.countDown();
    }
}
