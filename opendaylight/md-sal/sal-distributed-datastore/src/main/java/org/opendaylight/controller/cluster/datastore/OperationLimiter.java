/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.dispatch.OnComplete;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.BatchedModificationsReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for limiting operations. It extends {@link OnComplete}, so we can plug it seamlessly
 * into akka to release permits as futures complete.
 */
public class OperationLimiter extends OnComplete<Object> {
    private static final Logger LOG = LoggerFactory.getLogger(OperationLimiter.class);
    private final TransactionIdentifier identifier;
    private final long acquireTimeout;
    private final Semaphore semaphore;

    OperationLimiter(final TransactionIdentifier identifier, final int maxPermits, final int acquireTimeoutSeconds) {
        this.identifier = Preconditions.checkNotNull(identifier);

        Preconditions.checkArgument(acquireTimeoutSeconds >= 0);
        this.acquireTimeout = TimeUnit.SECONDS.toNanos(acquireTimeoutSeconds);

        Preconditions.checkArgument(maxPermits >= 0);
        this.semaphore = new Semaphore(maxPermits);
    }

    void acquire() {
        acquire(1);
    }

    private void acquire(final int acquirePermits) {
        try {
            if (!semaphore.tryAcquire(acquirePermits, acquireTimeout, TimeUnit.NANOSECONDS)) {
                LOG.warn("Failed to acquire operation permit for transaction {}", identifier);
            }
        } catch (InterruptedException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Interrupted when trying to acquire operation permit for transaction {}", identifier, e);
            } else {
                LOG.warn("Interrupted when trying to acquire operation permit for transaction {}", identifier);
            }
        }
    }

    void release() {
        this.semaphore.release();
    }

    @Override
    public void onComplete(final Throwable throwable, final Object message) {
        if (message instanceof BatchedModificationsReply) {
            this.semaphore.release(((BatchedModificationsReply)message).getNumBatched());
        } else {
            this.semaphore.release();
        }
    }

    public TransactionIdentifier getIdentifier() {
        return identifier;
    }

    @VisibleForTesting
    Semaphore getSemaphore() {
        return semaphore;
    }
}
