/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractTransactionContext implements TransactionContext {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractTransactionContext.class);
    private final OperationLimiter limiter;
    private long modificationCount = 0;
    private boolean handoffComplete;

    protected AbstractTransactionContext(final OperationLimiter limiter) {
        this.limiter = Preconditions.checkNotNull(limiter);
    }

    /**
     * Get the transaction identifier associated with this context.
     *
     * @return Transaction identifier.
     */
    @Nonnull protected final TransactionIdentifier getIdentifier() {
        return limiter.getIdentifier();
    }

    /**
     * Return the operation limiter associated with this context.
     * @return Operation limiter.
     */
    @Nonnull protected final OperationLimiter getLimiter() {
        return limiter;
    }

    /**
     * Indicate whether all operations have been handed off by the {@link TransactionContextWrapper}.
     *
     * @return True if this context is responsible for throttling.
     */
    protected final boolean isOperationHandoffComplete() {
        return handoffComplete;
    }

    /**
     * Acquire operation from the limiter if the handoff has completed. If
     * the handoff is still ongoing, this method does nothing.
     */
    protected final void acquireOperation() {
        if (handoffComplete) {
            limiter.acquire();
        }
    }

    /**
     * Acquire operation from the limiter if the handoff has NOT completed. If
     * the handoff has completed, this method does nothing.
     */
    protected final void releaseOperation() {
        if (!handoffComplete) {
            limiter.release();
        }
    }

    protected final void incrementModificationCount() {
        modificationCount++;
    }

    protected final void logModificationCount() {
        LOG.debug("Total modifications on Tx {} = [ {} ]", getIdentifier(), modificationCount);
    }

    @Override
    public final void operationHandoffComplete() {
        handoffComplete = true;
    }
}
