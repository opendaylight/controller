/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A helper class that wraps an eventual TransactionContext instance. Operations destined for the target
 * TransactionContext instance are cached until the TransactionContext instance is available.
 *
 * @author Thomas Pantelis
 */
class TransactionContextAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionContextAdapter.class);

    /**
     * The list of transaction operations to execute once the CreateTransaction completes.
     */
    @GuardedBy("txOperationsOnComplete")
    private final List<TransactionOperation> txOperationsOnComplete = Lists.newArrayList();

    /**
     * The resulting TransactionContext.
     */
    private volatile TransactionContext transactionContext;

    private final TransactionIdentifier identifier;

    TransactionContextAdapter(TransactionIdentifier identifier) {
        this.identifier = identifier;
    }

    TransactionContext getTransactionContext() {
        return transactionContext;
    }

    TransactionIdentifier getIdentifier() {
        return identifier;
    }

    /**
     * Adds a TransactionOperation to be executed after the CreateTransaction completes.
     */
    private void addTxOperationOnComplete(TransactionOperation operation) {
        boolean invokeOperation = true;
        synchronized(txOperationsOnComplete) {
            if(transactionContext == null) {
                LOG.debug("Tx {} Adding operation on complete", getIdentifier());

                invokeOperation = false;
                txOperationsOnComplete.add(operation);
            }
        }

        if(invokeOperation) {
            operation.invoke(transactionContext);
        }
    }

    void enqueueTransactionOperation(final TransactionOperation op) {

        if (transactionContext != null) {
            op.invoke(transactionContext);
        } else {
            // The shard Tx hasn't been created yet so add the Tx operation to the Tx Future
            // callback to be executed after the Tx is created.
            addTxOperationOnComplete(op);
        }
    }

    void executeTxOperationsOnComplete(TransactionContext localTransactionContext) {
        while(true) {
            // Access to txOperationsOnComplete and transactionContext must be protected and atomic
            // (ie synchronized) with respect to #addTxOperationOnComplete to handle timing
            // issues and ensure no TransactionOperation is missed and that they are processed
            // in the order they occurred.

            // We'll make a local copy of the txOperationsOnComplete list to handle re-entrancy
            // in case a TransactionOperation results in another transaction operation being
            // queued (eg a put operation from a client read Future callback that is notified
            // synchronously).
            Collection<TransactionOperation> operationsBatch = null;
            synchronized(txOperationsOnComplete) {
                if(txOperationsOnComplete.isEmpty()) {
                    // We're done invoking the TransactionOperations so we can now publish the
                    // TransactionContext.
                    transactionContext = localTransactionContext;
                    break;
                }

                operationsBatch = new ArrayList<>(txOperationsOnComplete);
                txOperationsOnComplete.clear();
            }

            // Invoke TransactionOperations outside the sync block to avoid unnecessary blocking.
            // A slight down-side is that we need to re-acquire the lock below but this should
            // be negligible.
            for(TransactionOperation oper: operationsBatch) {
                oper.invoke(localTransactionContext);
            }
        }
    }
}
