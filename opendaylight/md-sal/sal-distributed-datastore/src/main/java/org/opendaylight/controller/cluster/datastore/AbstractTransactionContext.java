/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractTransactionContext implements TransactionContext {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractTransactionContext.class);
    private final TransactionIdentifier transactionIdentifier;
    private long modificationCount = 0;
    private boolean handOffComplete;
    private final short transactionVersion;

    protected AbstractTransactionContext(TransactionIdentifier transactionIdentifier) {
        this(transactionIdentifier, DataStoreVersions.CURRENT_VERSION);
    }

    protected AbstractTransactionContext(TransactionIdentifier transactionIdentifier,
            short transactionVersion) {
        this.transactionIdentifier = transactionIdentifier;
        this.transactionVersion = transactionVersion;
    }

    /**
     * Get the transaction identifier associated with this context.
     *
     * @return Transaction identifier.
     */
    @Nonnull protected final TransactionIdentifier getIdentifier() {
        return transactionIdentifier;
    }

    protected final void incrementModificationCount() {
        modificationCount++;
    }

    protected final void logModificationCount() {
        LOG.debug("Total modifications on Tx {} = [ {} ]", getIdentifier(), modificationCount);
    }

    @Override
    public final void operationHandOffComplete() {
        handOffComplete = true;
    }

    protected boolean isOperationHandOffComplete(){
        return handOffComplete;
    }

    @Override
    public boolean usesOperationLimiting() {
        return false;
    }

    @Override
    public short getTransactionVersion() {
        return transactionVersion;
    }
}
