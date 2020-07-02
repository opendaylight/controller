/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import akka.actor.ActorSelection;
import akka.dispatch.OnComplete;
import akka.pattern.AskTimeoutException;
import akka.util.Timeout;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.exceptions.NoShardLeaderException;
import org.opendaylight.controller.cluster.datastore.exceptions.ShardLeaderNotRespondingException;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransaction;
import org.opendaylight.controller.cluster.datastore.messages.CreateTransactionReply;
import org.opendaylight.controller.cluster.datastore.messages.PrimaryShardInfo;
import org.opendaylight.controller.cluster.datastore.utils.ActorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

/**
 * Handles creation of TransactionContext instances for remote transactions. This class creates
 * remote transactions, if necessary, by sending CreateTransaction messages with retries, up to a limit,
 * if the shard doesn't have a leader yet. This is done by scheduling a retry task after a short delay.
 * <p/>
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

    private final DelayedTransactionContextWrapper transactionContextWrapper;

    RemoteTransactionContextSupport(final DelayedTransactionContextWrapper transactionContextWrapper,
            final TransactionProxy parent, final String shardName) {
        this.parent = requireNonNull(parent);
        this.shardName = shardName;
        this.transactionContextWrapper = transactionContextWrapper;

        // For the total create tx timeout, use 2 times the election timeout. This should be enough time for
        // a leader re-election to occur if we happen to hit it in transition.
        totalCreateTxTimeout = parent.getActorUtils().getDatastoreContext().getShardRaftConfig()
                .getElectionTimeOutInterval().toMillis() * 2;

        // We'll use the operationTimeout for the the create Tx message timeout so it can be set appropriately
        // for unit tests but cap it at MAX_CREATE_TX_MSG_TIMEOUT_IN_MS. The operationTimeout could be set
        // larger than the totalCreateTxTimeout in production which we don't want.
        long operationTimeout = parent.getActorUtils().getOperationTimeout().duration().toMillis();
        createTxMessageTimeout = new Timeout(Math.min(operationTimeout, MAX_CREATE_TX_MSG_TIMEOUT_IN_MS),
                TimeUnit.MILLISECONDS);
    }

    String getShardName() {
        return shardName;
    }

    private TransactionType getTransactionType() {
        return parent.getType();
    }

    private ActorUtils getActorUtils() {
        return parent.getActorUtils();
    }

    private TransactionIdentifier getIdentifier() {
        return parent.getIdentifier();
    }

    /**
     * Sets the target primary shard and initiates a CreateTransaction try.
     */
    void setPrimaryShard(final PrimaryShardInfo newPrimaryShardInfo) {
        this.primaryShardInfo = newPrimaryShardInfo;

        if (getTransactionType() == TransactionType.WRITE_ONLY
                && getActorUtils().getDatastoreContext().isWriteOnlyTransactionOptimizationsEnabled()) {
            ActorSelection primaryShard = newPrimaryShardInfo.getPrimaryShardActor();

            LOG.debug("Tx {} Primary shard {} found - creating WRITE_ONLY transaction context",
                getIdentifier(), primaryShard);

            // For write-only Tx's we prepare the transaction modifications directly on the shard actor
            // to avoid the overhead of creating a separate transaction actor.
            transactionContextWrapper.executePriorTransactionOperations(createValidTransactionContext(
                    primaryShard, String.valueOf(primaryShard.path()), newPrimaryShardInfo.getPrimaryShardVersion()));
        } else {
            tryCreateTransaction();
        }
    }

    /**
      Performs a CreateTransaction try async.
     */
    private void tryCreateTransaction() {
        LOG.debug("Tx {} Primary shard {} found - trying create transaction", getIdentifier(),
                primaryShardInfo.getPrimaryShardActor());

        Object serializedCreateMessage = new CreateTransaction(getIdentifier(), getTransactionType().ordinal(),
                    primaryShardInfo.getPrimaryShardVersion()).toSerializable();

        Future<Object> createTxFuture = getActorUtils().executeOperationAsync(
                primaryShardInfo.getPrimaryShardActor(), serializedCreateMessage, createTxMessageTimeout);

        createTxFuture.onComplete(new OnComplete<Object>() {
            @Override
            public void onComplete(final Throwable failure, final Object response) {
                onCreateTransactionComplete(failure, response);
            }
        }, getActorUtils().getClientDispatcher());
    }

    private void tryFindPrimaryShard() {
        LOG.debug("Tx {} Retrying findPrimaryShardAsync for shard {}", getIdentifier(), shardName);

        this.primaryShardInfo = null;
        Future<PrimaryShardInfo> findPrimaryFuture = getActorUtils().findPrimaryShardAsync(shardName);
        findPrimaryFuture.onComplete(new OnComplete<PrimaryShardInfo>() {
            @Override
            public void onComplete(final Throwable failure, final PrimaryShardInfo newPrimaryShardInfo) {
                onFindPrimaryShardComplete(failure, newPrimaryShardInfo);
            }
        }, getActorUtils().getClientDispatcher());
    }

    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification = "https://github.com/spotbugs/spotbugs/issues/811")
    private void onFindPrimaryShardComplete(final Throwable failure, final PrimaryShardInfo newPrimaryShardInfo) {
        if (failure == null) {
            this.primaryShardInfo = newPrimaryShardInfo;
            tryCreateTransaction();
        } else {
            LOG.debug("Tx {}: Find primary for shard {} failed", getIdentifier(), shardName, failure);

            onCreateTransactionComplete(failure, null);
        }
    }

    private void onCreateTransactionComplete(final Throwable failure, final Object response) {
        // An AskTimeoutException will occur if the local shard forwards to an unavailable remote leader or
        // the cached remote leader actor is no longer available.
        boolean retryCreateTransaction = primaryShardInfo != null
                && (failure instanceof NoShardLeaderException || failure instanceof AskTimeoutException);

        // Schedule a retry unless we're out of retries. Note: totalCreateTxTimeout is volatile as it may
        // be written by different threads however not concurrently, therefore decrementing it
        // non-atomically here is ok.
        if (retryCreateTransaction && totalCreateTxTimeout > 0) {
            long scheduleInterval = CREATE_TX_TRY_INTERVAL_IN_MS;
            if (failure instanceof AskTimeoutException) {
                // Since we use the createTxMessageTimeout for the CreateTransaction request and it timed
                // out, subtract it from the total timeout. Also since the createTxMessageTimeout period
                // has already elapsed, we can immediately schedule the retry (10 ms is virtually immediate).
                totalCreateTxTimeout -= createTxMessageTimeout.duration().toMillis();
                scheduleInterval = 10;
            }

            totalCreateTxTimeout -= scheduleInterval;

            LOG.debug("Tx {}: create tx on shard {} failed with exception \"{}\" - scheduling retry in {} ms",
                    getIdentifier(), shardName, failure, scheduleInterval);

            getActorUtils().getActorSystem().scheduler().scheduleOnce(
                    FiniteDuration.create(scheduleInterval, TimeUnit.MILLISECONDS),
                    this::tryFindPrimaryShard, getActorUtils().getClientDispatcher());
            return;
        }

        createTransactionContext(failure, response);
    }

    private void createTransactionContext(final Throwable failure, final Object response) {
        // Create the TransactionContext from the response or failure. Store the new
        // TransactionContext locally until we've completed invoking the
        // TransactionOperations. This avoids thread timing issues which could cause
        // out-of-order TransactionOperations. Eg, on a modification operation, if the
        // TransactionContext is non-null, then we directly call the TransactionContext.
        // However, at the same time, the code may be executing the cached
        // TransactionOperations. So to avoid thus timing, we don't publish the
        // TransactionContext until after we've executed all cached TransactionOperations.
        TransactionContext localTransactionContext;
        if (failure != null) {
            LOG.debug("Tx {} Creating NoOpTransaction because of error", getIdentifier(), failure);

            Throwable resultingEx = failure;
            if (failure instanceof AskTimeoutException) {
                resultingEx = new ShardLeaderNotRespondingException(String.format(
                        "Could not create a %s transaction on shard %s. The shard leader isn't responding.",
                        parent.getType(), shardName), failure);
            } else if (!(failure instanceof NoShardLeaderException)) {
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

    private TransactionContext createValidTransactionContext(final CreateTransactionReply reply) {
        LOG.debug("Tx {} Received {}", getIdentifier(), reply);

        return createValidTransactionContext(getActorUtils().actorSelection(reply.getTransactionPath()),
                reply.getTransactionPath(), primaryShardInfo.getPrimaryShardVersion());
    }

    private TransactionContext createValidTransactionContext(final ActorSelection transactionActor,
            final String transactionPath, final short remoteTransactionVersion) {
        final TransactionContext ret = new RemoteTransactionContext(transactionContextWrapper.getIdentifier(),
                transactionActor, getActorUtils(), remoteTransactionVersion, transactionContextWrapper.getLimiter());

        if (parent.getType() == TransactionType.READ_ONLY) {
            TransactionContextCleanup.track(parent, ret);
        }

        return ret;
    }
}

