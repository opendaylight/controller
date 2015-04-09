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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.opendaylight.controller.cluster.datastore.identifiers.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.messages.CloseTransaction;
import org.opendaylight.controller.cluster.datastore.utils.ActorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Future;

/**
 * Holder class tracking resources related to a transaction and ensuring they are
 * cleaned up as appropriate.
 */
final class TransactionResources {
    private static enum State {
        OPEN,
        READY,
        CLOSED,
    }

    private static final Logger LOG = LoggerFactory.getLogger(TransactionResources.class);
    /**
     * Stores the create transaction results per shard.
     */
    private final Map<String, TransactionFutureCallback> txFutureCallbackMap = new HashMap<>();
    private final TransactionIdentifier identifier;
    private final ActorContext actorContext;
    private volatile State state = State.OPEN;

    /**
     * Stores the remote Tx actors for each requested data store path to be used by the
     * PhantomReference to close the remote Tx's. This is only used for read-only Tx's. The
     * remoteTransactionActorsMB volatile serves as a memory barrier to publish updates to the
     * remoteTransactionActors list so they will be visible to the thread accessing the
     * PhantomReference.
     */
    private List<ActorSelection> remoteTransactionActors;
    private volatile AtomicBoolean remoteTransactionActorsMB;

    TransactionResources(final ActorContext actorContext, final TransactionIdentifier identifier) {
        this.actorContext = Preconditions.checkNotNull(actorContext);
        this.identifier = Preconditions.checkNotNull(identifier);
    }

    private boolean seal(final State newState) {
        if (state == State.OPEN) {
            state = newState;
            return true;
        } else {
            return false;
        }
    }

    void ensureOpen() {
        Preconditions.checkState(state == State.OPEN, "Transaction is sealed - further modifications are not allowed");
    }

    /**
     * Invoked when the resources need to be tracked.
     *
     * @param proxy Parent proxy
     * @param actor Actor which needs to be tracked
     */
    void track(final TransactionProxy proxy, final ActorSelection actor) {
        if (remoteTransactionActorsMB == null) {
            remoteTransactionActors = new ArrayList<>();
            remoteTransactionActorsMB = new AtomicBoolean();

            TransactionProxyCleanupPhantomReference.track(proxy);
        }

        // Add the actor to the remoteTransactionActors list for access by the
        // cleanup PhantonReference.
        remoteTransactionActors.add(actor);

        // Write to the memory barrier volatile to publish the above update to the
        // remoteTransactionActors list for thread visibility.
        remoteTransactionActorsMB.set(true);
    }

    /**
     * Invoked when the transaction is committed, such that no futher cleanup
     * necessary.
     *
     * @return Collection of callbacks which have been recorded.
     */
    Collection<TransactionFutureCallback> ready() {
        final boolean success = seal(State.READY);
        Preconditions.checkState(success, "Transaction %s is %s, it cannot be readied", identifier, state);

        return txFutureCallbackMap.values();
    }

    /**
     * Invoked when the transaction is closed without committing the potential changes. Performs
     * immediate cleanup and prevents cleanup when the phantom reference is cleaned up.
     */
    void close() {
        if (!seal(State.CLOSED)) {
            if (state == State.CLOSED) {
                // Idempotent no-op as per AutoCloseable recommendation
                return;
            }

            throw new IllegalStateException(String.format("Transaction %s is ready, it cannot be closed", identifier));
        }

        for (TransactionFutureCallback txFutureCallback : txFutureCallbackMap.values()) {
            txFutureCallback.enqueueTransactionOperation(new TransactionOperation() {
                @Override
                public void invoke(TransactionContext transactionContext) {
                    transactionContext.closeTransaction();
                }
            });
        }

        txFutureCallbackMap.clear();

        if (remoteTransactionActorsMB != null) {
            remoteTransactionActors.clear();
            remoteTransactionActorsMB.set(true);
        }
    }

    /**
     * Invoked from {@link TransactionProxyCleanupPhantomReference} to ensure
     * resources are cleaned up.
     */
    void cleanup() {
        if (remoteTransactionActorsMB != null) {
            LOG.trace("Cleaning up {} Tx actors for TransactionProxy {}",
                remoteTransactionActors.size(), identifier);

            // Access the memory barrier volatile to ensure all previous updates to the
            // remoteTransactionActors list are visible to this thread.
            if (remoteTransactionActorsMB.get()) {
                for(ActorSelection actor : remoteTransactionActors) {
                    LOG.trace("Sending CloseTransaction to {}", actor);
                    actorContext.sendOperationAsync(actor, CloseTransaction.INSTANCE.toSerializable());
                }
            }
        }
    }

    TransactionFutureCallback getTxFutureCallback(final TransactionProxy proxy, final String shardName) {
        final TransactionFutureCallback existing = txFutureCallbackMap.get(shardName);
        if (existing != null) {
            return existing;
        }

        Future<ActorSelection> findPrimaryFuture = proxy.sendFindPrimaryShardAsync(shardName);
        final TransactionFutureCallback ret = new TransactionFutureCallback(proxy, shardName);
        txFutureCallbackMap.put(shardName, ret);

        findPrimaryFuture.onComplete(new OnComplete<ActorSelection>() {
            @Override
            public void onComplete(Throwable failure, ActorSelection primaryShard) {
                if (failure != null) {
                    ret.createTransactionContext(failure, null);
                } else {
                    ret.setPrimaryShard(primaryShard);
                }
            }
        }, actorContext.getClientDispatcher());

        return ret;
    }
}
