/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Preconditions;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class Throttler {
    private static final Logger LOG = LoggerFactory.getLogger(Throttler.class);
    private final ActorContext actorContext;
    private OperationCompleter operationCompleter;
    private Semaphore operationLimiter;
    private volatile boolean initialized;

    Throttler(final ActorContext actorContext) {
        this.actorContext = Preconditions.checkNotNull(actorContext);
    }
    
    ActorContext getActorContext() {
        return actorContext;
    }

    TransactionIdentifier getIdentifier() {
        // TODO Auto-generated method stub
        return null;
    }

    Semaphore getOperationLimiter() {
        return operationLimiter;
    }

    void throttleOperation() {
        throttleOperation(1);
    }

    void ensureInitializied() {
        Preconditions.checkState(initialized, "Transaction %s was not propertly initialized.", getIdentifier());
    }

    void throttleOperation(int acquirePermits) {
        if (!initialized) {
            // Note : Currently mailbox-capacity comes from akka.conf and not from the config-subsystem
            operationLimiter = new Semaphore(actorContext.getTransactionOutstandingOperationLimit());
            operationCompleter = new OperationCompleter(operationLimiter);

            // Make sure we write this last because it's volatile and will also publish the non-volatile writes
            // above as well so they'll be visible to other threads.
            initialized = true;
        }

        try {
            if(!operationLimiter.tryAcquire(acquirePermits,
                    actorContext.getDatastoreContext().getOperationTimeoutInSeconds(), TimeUnit.SECONDS)){
                LOG.warn("Failed to acquire operation permit for transaction {}", getIdentifier());
            }
        } catch (InterruptedException e) {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Interrupted when trying to acquire operation permit for transaction " + getIdentifier(), e);
            } else {
                LOG.warn("Interrupted when trying to acquire operation permit for transaction {}", getIdentifier());
            }
        }
    }
}
