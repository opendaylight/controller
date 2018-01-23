/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for limiting operations.
 */
public class OperationLimiter  {
    private static final Logger LOG = LoggerFactory.getLogger(OperationLimiter.class);
    private final TransactionIdentifier identifier;
    private final long acquireTimeout;
    private final Semaphore semaphore;
    private final int maxPermits;

    OperationLimiter(final TransactionIdentifier identifier, final int maxPermits, final long acquireTimeoutSeconds) {
        this.identifier = Preconditions.checkNotNull(identifier);

        Preconditions.checkArgument(acquireTimeoutSeconds >= 0);
        this.acquireTimeout = TimeUnit.SECONDS.toNanos(acquireTimeoutSeconds);

        Preconditions.checkArgument(maxPermits >= 0);
        this.maxPermits = maxPermits;
        this.semaphore = new Semaphore(maxPermits);
    }

    boolean acquire() {
        return acquire(1);
    }

    boolean acquire(final int acquirePermits) {
        try {
            if (semaphore.tryAcquire(acquirePermits, acquireTimeout, TimeUnit.NANOSECONDS)) {
                return true;
            }
        } catch (InterruptedException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Interrupted when trying to acquire operation permit for transaction {}", identifier, e);
            } else {
                LOG.warn("Interrupted when trying to acquire operation permit for transaction {}", identifier);
            }
        }

        return false;
    }

    void release() {
        release(1);
    }

    void release(int permits) {
        this.semaphore.release(permits);
    }

    @VisibleForTesting
    TransactionIdentifier getIdentifier() {
        return identifier;
    }

    @VisibleForTesting
    int availablePermits() {
        return semaphore.availablePermits();
    }

    /**
     * Release all the permits.
     */
    public void releaseAll() {
        this.semaphore.release(maxPermits - availablePermits());
    }
}
