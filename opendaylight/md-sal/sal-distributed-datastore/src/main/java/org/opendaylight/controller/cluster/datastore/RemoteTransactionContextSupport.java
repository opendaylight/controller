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
import com.google.common.base.Preconditions;
import java.util.concurrent.TimeUnit;
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

    /**
     * Time interval in between transaction create retries.
     */
    private static final FiniteDuration CREATE_TX_TRY_INTERVAL = FiniteDuration.create(1, TimeUnit.SECONDS);

    private final TransactionProxy parent;
    private final String shardName;

    /**
     * The target primary shard.
     */
    private volatile ActorSelection primaryShard;
    private volatile int createTxTries;

    private final TransactionContextWrapper transactionContextAdapter;

    RemoteTransactionContextSupport(final TransactionContextWrapper transactionContextAdapter, final TransactionProxy parent,
            final String shardName) {
        this.parent = Preconditions.checkNotNull(parent);
        this.shardName = shardName;
        this.transactionContextAdapter = transactionContextAdapter;
        createTxTries = (int) (parent.getActorContext().getDatastoreContext().
                getShardLeaderElectionTimeout().duration().toMillis() /
                CREATE_TX_TRY_INTERVAL.toMillis());
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

    private OperationLimiter getOperationLimiter() {
        return parent.getLimiter();
    }

    private TransactionIdentifier getIdentifier() {
        return parent.getIdentifier();
    }

    /**
     * Sets the target primary shard and initiates a CreateTransaction try.
     */
    void setPrimaryShard(ActorSelection primaryShard, short primaryVersion) {
        this.primaryShard = primaryShard;

        if (getTransactionType() == TransactionType.WRITE_ONLY && primaryVersion >= DataStoreVersions.LITHIUM_VERSION &&
                getActorContext().getDatastoreContext().isWriteOnlyTransactionOptimizationsEnabled()) {
            LOG.debug("Tx {} Primary shard {} found - creating WRITE_ONLY transaction context",
                getIdentifier(), primaryShard);

            // For write-only Tx's we prepare the transaction modifications directly on the shard actor
            // to avoid the overhead of creating a separate transaction actor.
            transactionContextAdapter.executePriorTransactionOperations(createValidTransactionContext(this.primaryShard,
                    this.primaryShard.path().toString(), primaryVersion));
        } else {
            tryCreateTransaction();
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
            getTransactionType().ordinal(), getIdentifier().getChainId()).toSerializable();

        Future<Object> createTxFuture = getActorContext().executeOperationAsync(primaryShard, serializedCreateMessage);

        createTxFuture.onComplete(new OnComplete<Object>() {
            @Override
            public void onComplete(Throwable failure, Object response) {
                onCreateTransactionComplete(failure, response);
            }
        }, getActorContext().getClientDispatcher());
    }

    private void onCreateTransactionComplete(Throwable failure, Object response) {
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

            localTransactionContext = new NoOpTransactionContext(failure, getOperationLimiter());
        } else if (CreateTransactionReply.SERIALIZABLE_CLASS.equals(response.getClass())) {
            localTransactionContext = createValidTransactionContext(
                    CreateTransactionReply.fromSerializable(response));
        } else {
            IllegalArgumentException exception = new IllegalArgumentException(String.format(
                    "Invalid reply type %s for CreateTransaction", response.getClass()));

            localTransactionContext = new NoOpTransactionContext(exception, getOperationLimiter());
        }

        transactionContextAdapter.executePriorTransactionOperations(localTransactionContext);
    }

    private TransactionContext createValidTransactionContext(CreateTransactionReply reply) {
        LOG.debug("Tx {} Received {}", getIdentifier(), reply);

        return createValidTransactionContext(getActorContext().actorSelection(reply.getTransactionPath()),
                reply.getTransactionPath(), reply.getVersion());
    }

    private TransactionContext createValidTransactionContext(ActorSelection transactionActor, String transactionPath,
            short remoteTransactionVersion) {
        // TxActor is always created where the leader of the shard is.
        // Check if TxActor is created in the same node
        boolean isTxActorLocal = getActorContext().isPathLocal(transactionPath);
        final TransactionContext ret;

        if (remoteTransactionVersion < DataStoreVersions.LITHIUM_VERSION) {
            ret = new PreLithiumTransactionContextImpl(transactionPath, transactionActor,
                getActorContext(), isTxActorLocal, remoteTransactionVersion, parent.getLimiter());
        } else {
            ret = new RemoteTransactionContext(transactionActor, getActorContext(),
                isTxActorLocal, remoteTransactionVersion, parent.getLimiter());
        }

        if(parent.getType() == TransactionType.READ_ONLY) {
            TransactionContextCleanup.track(this, ret);
        }

        return ret;
    }
}

