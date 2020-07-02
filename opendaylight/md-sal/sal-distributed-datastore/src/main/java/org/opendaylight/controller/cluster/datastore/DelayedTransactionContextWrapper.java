/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static com.google.common.base.Preconditions.checkState;

import akka.actor.ActorSelection;
import akka.dispatch.Futures;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.SortedSet;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;
import scala.concurrent.Promise;

/**
 * Delayed implementation of TransactionContextWrapper. Operations destined for the target
 * TransactionContext instance are cached until the TransactionContext instance becomes
 * available at which time they are executed.
 *
 * @author Thomas Pantelis
 */
final class DelayedTransactionContextWrapper extends AbstractTransactionContextWrapper {
    private static final Logger LOG = LoggerFactory.getLogger(DelayedTransactionContextWrapper.class);

    /**
     * The list of transaction operations to execute once the TransactionContext becomes available.
     */
    @GuardedBy("queuedTxOperations")
    private final List<Entry<TransactionOperation, Boolean>> queuedTxOperations = new ArrayList<>();

    /**
     * The resulting TransactionContext.
     */
    private volatile TransactionContext transactionContext;
    @GuardedBy("queuedTxOperations")
    private TransactionContext deferredTransactionContext;
    @GuardedBy("queuedTxOperations")
    private boolean pendingEnqueue;

    DelayedTransactionContextWrapper(@NonNull final TransactionIdentifier identifier,
                                     @NonNull final ActorUtils actorUtils, @NonNull final String shardName) {
        super(identifier, actorUtils, shardName);
    }

    @Override
    TransactionContext getTransactionContext() {
        return transactionContext;
    }

    @Override
    void maybeExecuteTransactionOperation(final TransactionOperation op) {
        final TransactionContext localContext = transactionContext;
        if (localContext != null) {
            op.invoke(localContext, null);
        } else {
            // The shard Tx hasn't been created yet so add the Tx operation to the Tx Future
            // callback to be executed after the Tx is created.
            enqueueTransactionOperation(op);
        }
    }

    @Override
    Future<ActorSelection> readyTransaction(final Optional<SortedSet<String>> participatingShardNames) {
        // avoid the creation of a promise and a TransactionOperation
        final TransactionContext localContext = transactionContext;
        if (localContext != null) {
            return localContext.readyTransaction(null, participatingShardNames);
        }

        final Promise<ActorSelection> promise = Futures.promise();
        enqueueTransactionOperation(new TransactionOperation() {
            @Override
            public void invoke(final TransactionContext newTransactionContext, final Boolean havePermit) {
                promise.completeWith(newTransactionContext.readyTransaction(havePermit, participatingShardNames));
            }
        });

        return promise.future();
    }

    /**
     * Adds a TransactionOperation to be executed once the TransactionContext becomes available. This method is called
     * only after the caller has checked (without synchronizing with executePriorTransactionOperations()) that the
     * context is not available.
     */
    private void enqueueTransactionOperation(final TransactionOperation operation) {
        // We have three things to do here:
        // - synchronize with executePriorTransactionOperations() so that logical operation ordering is maintained
        // - acquire a permit for the operation if we still need to enqueue it
        // - enqueue the operation
        //
        // Since each operation needs to acquire a permit exactly once and the limiter is shared between us and the
        // TransactionContext, we need to know whether an operation has a permit before we enqueue it. Further
        // complications are:
        // - this method may be called from the thread invoking executePriorTransactionOperations()
        // - user may be violating API contract of using the transaction from a single thread

        // As a first step, we will synchronize on the queue and check if the handoff has completed. While we have
        // the lock, we will assert that we will be enqueing another operation.
        final TransactionContext contextOnEntry;
        synchronized (queuedTxOperations) {
            contextOnEntry = transactionContext;
            if (contextOnEntry == null) {
                checkState(pendingEnqueue == false, "Concurrent access to transaction %s detected", getIdentifier());
                pendingEnqueue = true;
            }
        }

        // Short-circuit if there is a context
        if (contextOnEntry != null) {
            operation.invoke(transactionContext, null);
            return;
        }

        boolean cleanupEnqueue = true;
        TransactionContext finishHandoff = null;
        try {
            // Acquire the permit,
            final boolean havePermit = getLimiter().acquire();
            if (!havePermit) {
                LOG.warn("Failed to acquire enqueue operation permit for transaction {} on shard {}", getIdentifier(),
                        getShardName());
            }

            // Ready to enqueue, take the lock again and append the operation
            synchronized (queuedTxOperations) {
                LOG.debug("Tx {} Queuing TransactionOperation", getIdentifier());
                queuedTxOperations.add(new SimpleImmutableEntry<>(operation, havePermit));
                pendingEnqueue = false;
                cleanupEnqueue = false;
                finishHandoff = deferredTransactionContext;
                deferredTransactionContext = null;
            }
        } finally {
            if (cleanupEnqueue) {
                synchronized (queuedTxOperations) {
                    pendingEnqueue = false;
                    finishHandoff = deferredTransactionContext;
                    deferredTransactionContext = null;
                }
            }
            if (finishHandoff != null) {
                executePriorTransactionOperations(finishHandoff);
            }
        }
    }

    void executePriorTransactionOperations(final TransactionContext localTransactionContext) {
        while (true) {
            // Access to queuedTxOperations and transactionContext must be protected and atomic
            // (ie synchronized) with respect to #addTxOperationOnComplete to handle timing
            // issues and ensure no TransactionOperation is missed and that they are processed
            // in the order they occurred.

            // We'll make a local copy of the queuedTxOperations list to handle re-entrancy
            // in case a TransactionOperation results in another transaction operation being
            // queued (eg a put operation from a client read Future callback that is notified
            // synchronously).
            final Collection<Entry<TransactionOperation, Boolean>> operationsBatch;
            synchronized (queuedTxOperations) {
                if (queuedTxOperations.isEmpty()) {
                    if (!pendingEnqueue) {
                        // We're done invoking the TransactionOperations so we can now publish the TransactionContext.
                        localTransactionContext.operationHandOffComplete();

                        // This is null-to-non-null transition after which we are releasing the lock and not doing
                        // any further processing.
                        transactionContext = localTransactionContext;
                    } else {
                        deferredTransactionContext = localTransactionContext;
                    }
                    return;
                }

                operationsBatch = new ArrayList<>(queuedTxOperations);
                queuedTxOperations.clear();
            }

            // Invoke TransactionOperations outside the sync block to avoid unnecessary blocking. A slight down-side is
            // that we need to re-acquire the lock below but this should be negligible.
            for (Entry<TransactionOperation, Boolean> oper : operationsBatch) {
                final Boolean permit = oper.getValue();
                if (permit.booleanValue() && !localTransactionContext.usesOperationLimiting()) {
                    // If the context is not using limiting we need to release operations as we are queueing them, so
                    // user threads are not charged for them.
                    getLimiter().release();
                }
                oper.getKey().invoke(localTransactionContext, permit);
            }
        }
    }

}
