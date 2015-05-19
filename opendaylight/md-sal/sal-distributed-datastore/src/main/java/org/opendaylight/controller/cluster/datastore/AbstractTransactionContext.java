/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;

abstract class AbstractTransactionContext implements TransactionContext {
    private final OperationLimiter limiter;
    private boolean handoffComplete;

    protected AbstractTransactionContext(OperationLimiter limiter) {
        this.limiter = Preconditions.checkNotNull(limiter);
    }

    protected final TransactionIdentifier getIdentifier() {
        return limiter.getIdentifier();
    }

    protected final OperationLimiter getLimiter() {
        return limiter;
    }

    protected final boolean isOperationHandoffComplete() {
        return handoffComplete;
    }

    protected final void acquireOperation() {
        if (handoffComplete) {
            limiter.throttleOperation();
        }
    }

    protected final void releaseOperation() {
        if (!handoffComplete) {
            limiter.release();
        }
    }

    @Override
    public final void operationHandoffComplete() {
        handoffComplete = true;
    }
}