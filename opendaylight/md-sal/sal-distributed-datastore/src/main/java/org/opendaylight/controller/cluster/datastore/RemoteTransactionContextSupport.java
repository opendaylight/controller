/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import akka.actor.ActorSelection;
import akka.dispatch.OnComplete;
import akka.pattern.AskTimeoutException;
import akka.util.Timeout;
import com.google.common.base.Preconditions;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.datastore.exceptions.NoShardLeaderException;
import org.opendaylight.controller.cluster.datastore.exceptions.ShardLeaderNotRespondingException;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryShardInfo;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

/**
 * Handles creation of TransactionContext instances for remote transactions. This class creates
 * remote transactions, if necessary, by sending CreateTransaction messages with retries, up to a limit,
 * if the shard doesn't have a leader yet. This is done by scheduling a retry task after a short delay.
 * <p>
 * The end result from a completed CreateTransaction message is a TransactionContext that is
 * used to perform transaction operations. Transaction operations that occur before the
 * CreateTransaction completes are cache via a TransactionContextWrapper and executed once the
 * CreateTransaction completes, successfully or not.
 */
final class RemoteTransactionContextSupport {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteTransactionContextSupport.class);

    private static final long CREATE_TX_TRY_INTERVAL_IN_MS = 1000;
    private static final long MAX_CREATE_TX_MSG_TIMEOUT_IN_MS = 5000;

    private final TransactionProxy parent;
    private final String shardName;

    /**
     * The target primary shard.
     */
    private volatile PrimaryShardInfo primaryShardInfo;

    /**
     * The total timeout for creating a tx on the primary shard.
     */
    private volatile long totalCreateTxTimeout;

    private final Timeout createTxMessageTimeout;

    private final TransactionContextWrapper transactionContextWrapper;

    RemoteTransactionContextSupport(final TransactionContextWrapper transactionContextWrapper, final TransactionProxy parent,
            final String shardName) {
        this.parent = Preconditions.checkNotNull(parent);
        this.shardName = shardName;
        this.transactionContextWrapper = transactionContextWrapper;

        // For the total create tx timeout, use 2 times the election timeout. This should be enough time for
        // a leader re-election to occur if we happen to hit it in transition.
        totalCreateTxTimeout = parent.getActorContext().getDatastoreContext().getShardRaftConfig()
                .getElectionTimeOutInterval().toMillis() * 2;

        // We'll use the operationTimeout for the the create Tx message timeout so it can be set appropriately
        // for unit tests but cap it at MAX_CREATE_TX_MSG_TIMEOUT_IN_MS. The operationTimeout could be set
        // larger than the totalCreateTxTimeout in production which we don't want.
        long operationTimeout = parent.getActorContext().getOperationTimeout().duration().toMillis();
        createTxMessageTimeout = new Timeout(Math.min(operationTimeout, MAX_CREATE_TX_MSG_TIMEOUT_IN_MS),
                TimeUnit.MILLISECONDS);
    }

    String getShardName() {
        return shardName;
    }

    private TransactionType getTransactionType() {
        return parent.getType();
    }

    private ActorContext getActorContext() {
        return parent.getActorContext();
    }

    private TransactionIdentifier getIdentifier() {
        return parent.getIdentifier();
    }

    /**
     * Sets the target primary shard and initiates a CreateTransaction try.
     */
    void setPrimaryShard(PrimaryShardInfo primaryShardInfo) {
        this.primaryShardInfo = primaryShardInfo;

        if (getTransactionType() == TransactionType.WRITE_ONLY  &&
                getActorContext().getDatastoreContext().isWriteOnlyTransactionOptimizationsEnabled()) {
            ActorSelection primaryShard = primaryShardInfo.getPrimaryShardActor();

            LOG.debug("Tx {} Primary shard {} found - creating WRITE_ONLY transaction context",
                getIdentifier(), primaryShard);

            // For write-only Tx's we prepare the transaction modifications directly on the shard actor
            // to avoid the overhead of creating a separate transaction actor.
            transactionContextWrapper.executePriorTransactionOperations(createValidTransactionContext(
                    primaryShard, String.valueOf(primaryShard.path()), primaryShardInfo.getPrimaryShardVersion()));
        } else {
            tryCreateTransaction();
        }
    }

    /**
     * Performs a CreateTransaction try async.
     */
    private void tryCreateTransaction() {
        if(LOG.isDebugEnabled()) {
            LOG.debug("Tx {} Primary shard {} found - trying create transaction", getIdentifier(),
                    primaryShardInfo.getPrimaryShardActor());
        }

        Object serializedCreateMessage = new CreateTransaction(getIdentifier().toString(),
                getTransactionType().ordinal(), getIdentifier().getChainId(),
                    primaryShardInfo.getPrimaryShardVersion()).toSerializable();

        Future<Object> createTxFuture = getActorContext().executeOperationAsync(
                primaryShardInfo.getPrimaryShardActor(), serializedCreateMessage, createTxMessageTimeout);

        createTxFuture.onComplete(new OnComplete<Object>() {
            @Override
            public void onComplete(Throwable failure, Object response) {
                onCreateTransactionComplete(failure, response);
            }
        }, getActorContext().getClientDispatcher());
    }

    private void tryFindPrimaryShard() {
        LOG.debug("Tx {} Retrying findPrimaryShardAsync for shard {}", getIdentifier(), shardName);

        this.primaryShardInfo = null;
        Future<PrimaryShardInfo> findPrimaryFuture = getActorContext().findPrimaryShardAsync(shardName);
        findPrimaryFuture.onComplete(new OnComplete<PrimaryShardInfo>() {
            @Override
            public void onComplete(final Throwable failure, final PrimaryShardInfo primaryShardInfo) {
                onFindPrimaryShardComplete(failure, primaryShardInfo);
            }
        }, getActorContext().getClientDispatcher());
    }

    private void onFindPrimaryShardComplete(final Throwable failure, final PrimaryShardInfo primaryShardInfo) {
        if (failure == null) {
            this.primaryShardInfo = primaryShardInfo;
            tryCreateTransaction();
        } else {
            LOG.debug("Tx {}: Find primary for shard {} failed", getIdentifier(), shardName, failure);

            onCreateTransactionComplete(failure, null);
        }
    }

    private void onCreateTransactionComplete(Throwable failure, Object response) {
        // An AskTimeoutException will occur if the local shard forwards to an unavailable remote leader or
        // the cached remote leader actor is no longer available.
        boolean retryCreateTransaction = primaryShardInfo != null &&
                (failure instanceof NoShardLeaderException || failure instanceof AskTimeoutException);
        if(retryCreateTransaction) {
            // Schedule a retry unless we're out of retries. Note: totalCreateTxTimeout is volatile as it may
            // be written by different threads however not concurrently, therefore decrementing it
            // non-atomically here is ok.
            if(totalCreateTxTimeout > 0) {
                long scheduleInterval = CREATE_TX_TRY_INTERVAL_IN_MS;
                if(failure instanceof AskTimeoutException) {
                    // Since we use the createTxMessageTimeout for the CreateTransaction request and it timed
                    // out, subtract it from the total timeout. Also since the createTxMessageTimeout period
                    // has already elapsed, we can immediately schedule the retry (10 ms is virtually immediate).
                    totalCreateTxTimeout -= createTxMessageTimeout.duration().toMillis();
                    scheduleInterval = 10;
                }

                totalCreateTxTimeout -= scheduleInterval;

                LOG.debug("Tx {}: create tx on shard {} failed with exception \"{}\" - scheduling retry in {} ms",
                        getIdentifier(), shardName, failure, scheduleInterval);

                getActorContext().getActorSystem().scheduler().scheduleOnce(
                        FiniteDuration.create(scheduleInterval, TimeUnit.MILLISECONDS),
                        new Runnable() {
                            @Override
                            public void run() {
                                tryFindPrimaryShard();
                            }
                        }, getActorContext().getClientDispatcher());
                return;
            }
        }

        createTransactionContext(failure, response);
    }

    private void createTransactionContext(Throwable failure, Object response) {
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

            Throwable resultingEx = failure;
            if(failure instanceof AskTimeoutException) {
                resultingEx = new ShardLeaderNotRespondingException(String.format(
                        "Could not create a %s transaction on shard %s. The shard leader isn't responding.",
                        parent.getType(), shardName), failure);
            } else if(!(failure instanceof NoShardLeaderException)) {
                resultingEx = new Exception(String.format(
                    "Error creating %s transaction on shard %s", parent.getType(), shardName), failure);
            }

            localTransactionContext = new NoOpTransactionContext(resultingEx, getIdentifier());
        } else if (CreateTransactionReply.isSerializedType(response)) {
            localTransactionContext = createValidTransactionContext(
                    CreateTransactionReply.fromSerializable(response));
        } else {
            IllegalArgumentException exception = new IllegalArgumentException(String.format(
                    "Invalid reply type %s for CreateTransaction", response.getClass()));

            localTransactionContext = new NoOpTransactionContext(exception, getIdentifier());
        }

        transactionContextWrapper.executePriorTransactionOperations(localTransactionContext);
    }

    private TransactionContext createValidTransactionContext(CreateTransactionReply reply) {
        LOG.debug("Tx {} Received {}", getIdentifier(), reply);

        return createValidTransactionContext(getActorContext().actorSelection(reply.getTransactionPath()),
                reply.getTransactionPath(), primaryShardInfo.getPrimaryShardVersion());
    }

    private TransactionContext createValidTransactionContext(ActorSelection transactionActor, String transactionPath,
            short remoteTransactionVersion) {
        final TransactionContext ret = new RemoteTransactionContext(transactionContextWrapper.getIdentifier(),
                transactionActor, getActorContext(), remoteTransactionVersion, transactionContextWrapper.getLimiter());

        if(parent.getType() == TransactionType.READ_ONLY) {
            TransactionContextCleanup.track(parent, ret);
        }

        return ret;
    }
}

