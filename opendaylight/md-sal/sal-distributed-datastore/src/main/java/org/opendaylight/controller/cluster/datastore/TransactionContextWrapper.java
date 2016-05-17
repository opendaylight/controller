/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSelection;
import akka.dispatch.Futures;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;
import scala.concurrent.Promise;

/**
 * A helper class that wraps an eventual TransactionContext instance. Operations destined for the target
 * TransactionContext instance are cached until the TransactionContext instance becomes available at which
 * time they are executed.
 *
 * @author Thomas Pantelis
 */
class TransactionContextWrapper {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionContextWrapper.class);

    /**
     * The list of transaction operations to execute once the TransactionContext becomes available.
     */
    @GuardedBy("queuedTxOperations")
    private final List<TransactionOperation> queuedTxOperations = Lists.newArrayList();

    private final TransactionIdentifier<?> identifier;

    /**
     * The resulting TransactionContext.
     */
    private volatile TransactionContext transactionContext;

    private final OperationLimiter limiter;

    TransactionContextWrapper(final TransactionIdentifier<?> identifier, final ActorContext actorContext) {
        this.identifier = Preconditions.checkNotNull(identifier);
        this.limiter = new OperationLimiter(identifier,
                actorContext.getDatastoreContext().getShardBatchedModificationCount() + 1, // 1 extra permit for the ready operation
                TimeUnit.MILLISECONDS.toSeconds(actorContext.getDatastoreContext().getOperationTimeoutInMillis()));
    }

    TransactionContext getTransactionContext() {
        return transactionContext;
    }

    TransactionIdentifier<?> getIdentifier() {
        return identifier;
    }

    /**
     * Adds a TransactionOperation to be executed once the TransactionContext becomes available.
     */
    private void enqueueTransactionOperation(final TransactionOperation operation) {
        final boolean invokeOperation;
        synchronized (queuedTxOperations) {
            if (transactionContext == null) {
                LOG.debug("Tx {} Queuing TransactionOperation", getIdentifier());

                queuedTxOperations.add(operation);
                invokeOperation = false;
            }  else {
                invokeOperation = true;
            }
        }

        if (invokeOperation) {
            operation.invoke(transactionContext);
        } else {
            limiter.acquire();
        }
    }

    void maybeExecuteTransactionOperation(final TransactionOperation op) {

        if (transactionContext != null) {
            op.invoke(transactionContext);
        } else {
            // The shard Tx hasn't been created yet so add the Tx operation to the Tx Future
            // callback to be executed after the Tx is created.
            enqueueTransactionOperation(op);
        }
    }

    void executePriorTransactionOperations(final TransactionContext localTransactionContext) {
        while(true) {
            // Access to queuedTxOperations and transactionContext must be protected and atomic
            // (ie synchronized) with respect to #addTxOperationOnComplete to handle timing
            // issues and ensure no TransactionOperation is missed and that they are processed
            // in the order they occurred.

            // We'll make a local copy of the queuedTxOperations list to handle re-entrancy
            // in case a TransactionOperation results in another transaction operation being
            // queued (eg a put operation from a client read Future callback that is notified
            // synchronously).
            Collection<TransactionOperation> operationsBatch = null;
            synchronized (queuedTxOperations) {
                if (queuedTxOperations.isEmpty()) {
                    // We're done invoking the TransactionOperations so we can now publish the
                    // TransactionContext.
                    localTransactionContext.operationHandOffComplete();
                    if(!localTransactionContext.usesOperationLimiting()){
                        limiter.releaseAll();
                    }
                    transactionContext = localTransactionContext;
                    break;
                }

                operationsBatch = new ArrayList<>(queuedTxOperations);
                queuedTxOperations.clear();
            }

            // Invoke TransactionOperations outside the sync block to avoid unnecessary blocking.
            // A slight down-side is that we need to re-acquire the lock below but this should
            // be negligible.
            for (TransactionOperation oper : operationsBatch) {
                oper.invoke(localTransactionContext);
            }
        }
    }

    Future<ActorSelection> readyTransaction() {
        // avoid the creation of a promise and a TransactionOperation
        if (transactionContext != null) {
            return transactionContext.readyTransaction();
        }

        final Promise<ActorSelection> promise = Futures.promise();
        enqueueTransactionOperation(new TransactionOperation() {
            @Override
            public void invoke(TransactionContext transactionContext) {
                promise.completeWith(transactionContext.readyTransaction());
            }
        });

        return promise.future();
    }

    public OperationLimiter getLimiter() {
        return limiter;
    }


}
