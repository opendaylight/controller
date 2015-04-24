/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSelection;
import akka.dispatch.OnComplete;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.cluster.datastore.compat.PreLithiumTransactionContextImpl;
import org.opendaylight.controller.cluster.datastore.exceptions.NoShardLeaderException;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransactionReply;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

/**
 * Implements a Future OnComplete callback for a CreateTransaction message. This class handles
 * retries, up to a limit, if the shard doesn't have a leader yet. This is done by scheduling a
 * retry task after a short delay.
 * <p>
 * The end result from a completed CreateTransaction message is a TransactionContext that is
 * used to perform transaction operations. Transaction operations that occur before the
 * CreateTransaction completes are cache and executed once the CreateTransaction completes,
 * successfully or not.
 */
final class TransactionFutureCallback extends OnComplete<Object> {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionFutureCallback.class);

    /**
     * Time interval in between transaction create retries.
     */
    private static final FiniteDuration CREATE_TX_TRY_INTERVAL = FiniteDuration.create(1, TimeUnit.SECONDS);

    /**
     * The list of transaction operations to execute once the CreateTransaction completes.
     */
    @GuardedBy("txOperationsOnComplete")
    private final List<TransactionOperation> txOperationsOnComplete = Lists.newArrayList();
    private final RemoteTransactionContext context;
    private final String shardName;

    /**
     * The TransactionContext resulting from the CreateTransaction reply.
     */
    private volatile TransactionContext transactionContext;

    /**
     * The target primary shard.
     */
    private volatile ActorSelection primaryShard;
    private volatile int createTxTries;

    TransactionFutureCallback(final RemoteTransactionContext context, final String shardName) {
        this.context = Preconditions.checkNotNull(context);
        this.shardName = shardName;
        createTxTries = (int) (context.getActorContext().getDatastoreContext().
                getShardLeaderElectionTimeout().duration().toMillis() /
                CREATE_TX_TRY_INTERVAL.toMillis());
    }

    String getShardName() {
        return shardName;
    }

    TransactionContext getTransactionContext() {
        return transactionContext;
    }

    private TransactionType getTransactionType() {
        return context.getTransactionType();
    }

    private TransactionIdentifier getIdentifier() {
        return context.getIdentifier();
    }

    private ActorContext getActorContext() {
        return context.getActorContext();
    }

    private Semaphore getOperationLimiter() {
        return context.getOperationLimiter();
    }

    /**
     * Sets the target primary shard and initiates a CreateTransaction try.
     */
    void setPrimaryShard(ActorSelection primaryShard) {
        this.primaryShard = primaryShard;

        if (getTransactionType() == TransactionType.WRITE_ONLY &&
                getActorContext().getDatastoreContext().isWriteOnlyTransactionOptimizationsEnabled()) {
            LOG.debug("Tx {} Primary shard {} found - creating WRITE_ONLY transaction context",
                getIdentifier(), primaryShard);

            // For write-only Tx's we prepare the transaction modifications directly on the shard actor
            // to avoid the overhead of creating a separate transaction actor.
            // FIXME: can't assume the shard version is LITHIUM_VERSION - need to obtain it somehow.
            executeTxOperatonsOnComplete(createValidTransactionContext(this.primaryShard,
                    this.primaryShard.path().toString(), DataStoreVersions.LITHIUM_VERSION));
        } else {
            tryCreateTransaction();
        }
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

    /**
     * Performs a CreateTransaction try async.
     */
    private void tryCreateTransaction() {
        if(LOG.isDebugEnabled()) {
            LOG.debug("Tx {} Primary shard {} found - trying create transaction", getIdentifier(), primaryShard);
        }

        Object serializedCreateMessage = new CreateTransaction(getIdentifier().toString(),
            getTransactionType().ordinal(), context.getTransactionChainId()).toSerializable();

        Future<Object> createTxFuture = getActorContext().executeOperationAsync(primaryShard, serializedCreateMessage);

        createTxFuture.onComplete(this, getActorContext().getClientDispatcher());
    }

    @Override
    public void onComplete(Throwable failure, Object response) {
        if(failure instanceof NoShardLeaderException) {
            // There's no leader for the shard yet - schedule and try again, unless we're out
            // of retries. Note: createTxTries is volatile as it may be written by different
            // threads however not concurrently, therefore decrementing it non-atomically here
            // is ok.
            if(--createTxTries > 0) {
                LOG.debug("Tx {} Shard {} has no leader yet - scheduling create Tx retry",
                    getIdentifier(), shardName);

                getActorContext().getActorSystem().scheduler().scheduleOnce(CREATE_TX_TRY_INTERVAL,
                        new Runnable() {
                            @Override
                            public void run() {
                                tryCreateTransaction();
                            }
                        }, getActorContext().getClientDispatcher());
                return;
            }
        }

        createTransactionContext(failure, response);
    }

    void createTransactionContext(Throwable failure, Object response) {
        // Create the TransactionContext from the response or failure. Store the new
        // TransactionContext locally until we've completed invoking the
        // TransactionOperations. This avoids thread timing issues which could cause
        // out-of-order TransactionOperations. Eg, on a modification operation, if the
        // TransactionContext is non-null, then we directly call the TransactionContext.
        // However, at the same time, the code may be executing the cached
        // TransactionOperations. So to avoid thus timing, we don't publish the
        // TransactionContext until after we've executed all cached TransactionOperations.
        TransactionContext localTransactionContext;
        if(failure != null) {
            LOG.debug("Tx {} Creating NoOpTransaction because of error", getIdentifier(), failure);

            localTransactionContext = new NoOpTransactionContext(failure, getIdentifier(), getOperationLimiter());
        } else if (CreateTransactionReply.SERIALIZABLE_CLASS.equals(response.getClass())) {
            localTransactionContext = createValidTransactionContext(
                    CreateTransactionReply.fromSerializable(response));
        } else {
            IllegalArgumentException exception = new IllegalArgumentException(String.format(
                    "Invalid reply type %s for CreateTransaction", response.getClass()));

            localTransactionContext = new NoOpTransactionContext(exception, getIdentifier(), getOperationLimiter());
        }

        executeTxOperatonsOnComplete(localTransactionContext);
    }

    private void executeTxOperatonsOnComplete(TransactionContext localTransactionContext) {
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

    private TransactionContext createValidTransactionContext(CreateTransactionReply reply) {
        LOG.debug("Tx {} Received {}", getIdentifier(), reply);

        return createValidTransactionContext(getActorContext().actorSelection(reply.getTransactionPath()),
                reply.getTransactionPath(), reply.getVersion());
    }

    private TransactionContext createValidTransactionContext(ActorSelection transactionActor, String transactionPath, short remoteTransactionVersion) {
        // TxActor is always created where the leader of the shard is.
        // Check if TxActor is created in the same node
        boolean isTxActorLocal = getActorContext().isPathLocal(transactionPath);
        final TransactionContext ret;

        if (remoteTransactionVersion < DataStoreVersions.LITHIUM_VERSION) {
            ret = new PreLithiumTransactionContextImpl(transactionPath, transactionActor, getIdentifier(),
                getActorContext(), isTxActorLocal, remoteTransactionVersion, context.getCompleter());
        } else {
            ret = new TransactionContextImpl(transactionActor, getIdentifier(), getActorContext(),
                isTxActorLocal, remoteTransactionVersion, context.getCompleter());
        }

        TransactionContextCleanup.track(this, ret);
        return ret;
    }
}
