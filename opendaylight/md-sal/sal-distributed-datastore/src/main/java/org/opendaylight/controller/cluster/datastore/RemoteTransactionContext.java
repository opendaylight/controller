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

final class RemoteTransactionContext {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteTransactionContext.class);
    private final TransactionProxy parent;
    private final OperationCompleter operationCompleter;
    private final Semaphore operationLimiter;

    RemoteTransactionContext(final TransactionProxy parent) {
        this.parent = Preconditions.checkNotNull(parent);
        // Note : Currently mailbox-capacity comes from akka.conf and not from the config-subsystem
        operationLimiter = new Semaphore(parent.getActorContext().getTransactionOutstandingOperationLimit());
        operationCompleter = new OperationCompleter(operationLimiter);
    }

    ActorContext getActorContext() {
        return parent.getActorContext();
    }

    TransactionIdentifier getIdentifier() {
        return parent.getIdentifier();
    }

    String getTransactionChainId() {
        // FIXME: extract transaction chain ID form TransactionIdentifier
        return null;
    }

    TransactionType getTransactionType() {
        return parent.getType();
    }

    Semaphore getOperationLimiter() {
        return operationLimiter;
    }

    OperationCompleter getCompleter() {
        return operationCompleter;
    }

    void throttleOperation() {
        throttleOperation(1);
    }

    void throttleOperation(int acquirePermits) {
        try {
            if(!operationLimiter.tryAcquire(acquirePermits,
                parent.getActorContext().getDatastoreContext().getOperationTimeoutInSeconds(), TimeUnit.SECONDS)){
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
