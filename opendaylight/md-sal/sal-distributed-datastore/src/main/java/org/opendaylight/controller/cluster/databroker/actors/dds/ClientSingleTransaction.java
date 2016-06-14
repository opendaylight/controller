/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import org.opendaylight.controller.cluster.access.commands.ExistsTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ExistsTransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.ReadTransactionSuccess;
import org.opendaylight.controller.cluster.access.concepts.RequestFailure;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Client-side view of a free-standing transaction.
 *
 * This interface is used by the world outside of the actor system and in the actor system it is manifested via
 * its client actor. That requires some state transfer with {@link DistributedDataStoreClientBehavior}. In order to
 * reduce request latency, all messages are carbon-copied (and enqueued first) to the client actor.
 *
 * It is internally composed of multiple {@link TransactionStateTracker}s, each responsible for a component shard.
 *
 * @author Robert Varga
 */
@Beta
public final class ClientSingleTransaction implements AutoCloseable {
    private static final AtomicIntegerFieldUpdater<ClientSingleTransaction> STATE_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(ClientSingleTransaction.class, "state");
    private static final int IDLE_STATE = 0;
    private static final int CLOSED_STATE = 1;

    private final Map<Long, TransactionStateTracker> trackers = new HashMap<>();
    private final TransactionIdentifier transactionId;
    private final AbstractClientHistory parent;

    private volatile int state = IDLE_STATE;

    ClientSingleTransaction(final DistributedDataStoreClientBehavior client, final AbstractClientHistory parent,
        final TransactionIdentifier transactionId) {
        this.transactionId = Preconditions.checkNotNull(transactionId);
        this.parent = Preconditions.checkNotNull(parent);
    }

    private void checkNotClosed() {
        Preconditions.checkState(state != CLOSED_STATE, "Transaction %s is closed", transactionId);
    }

    @Override
    public void close() {
        if (STATE_UPDATER.compareAndSet(this, IDLE_STATE, CLOSED_STATE)) {
            // FIXME: signal close to both client actor and backend actor
        } else if (state != CLOSED_STATE) {
            throw new IllegalStateException("Cannot close history with an open transaction");
        }
    }

    private TransactionStateTracker ensureTracker(final Long shard) {
        TransactionStateTracker ret = trackers.get(shard);
        if (ret == null) {
            ret = new TransactionStateTracker(parent.getHistoryForCookie(shard), transactionId.getTransactionId());
            trackers.put(shard, ret);
        }
        return ret;
    }

    public CompletionStage<Boolean> invokeExists(final Long shard, final YangInstanceIdentifier path) {
        checkNotClosed();
        return invokeExists(ensureTracker(shard), path);
    }

    public CompletionStage<Optional<NormalizedNode<?, ?>>> invokeRead(final Long shard, final YangInstanceIdentifier path) {
        checkNotClosed();
        return invokeRead(ensureTracker(shard), path);
    }

    private static void completeExists(final CompletableFuture<Boolean> future, final Response<?, ?> response) {
        if (response instanceof RequestFailure) {
            future.completeExceptionally(((RequestFailure<?, ?>) response).getCause());
        } else if (response instanceof ExistsTransactionSuccess) {
            future.complete(((ExistsTransactionSuccess) response).getExists());
        } else {
            future.completeExceptionally(new IllegalArgumentException("Unhandled reply " + response));
        }
    }

    private CompletionStage<Boolean> invokeExists(final TransactionStateTracker tracker, final YangInstanceIdentifier path) {
        final CompletableFuture<Boolean> future = new CompletableFuture<>();
        parent.getClient().sendRequest(new ExistsTransactionRequest(tracker.getIdentifier(), tracker.nextSequence(),
            parent.getClient().self(), path), t -> completeExists(future, t));
        return future;
    }

    private static void completeRead(final CompletableFuture<Optional<NormalizedNode<?, ?>>> future, final Response<?, ?> response) {
        if (response instanceof RequestFailure) {
            future.completeExceptionally(((RequestFailure<?, ?>) response).getCause());
        } else if (response instanceof ReadTransactionSuccess) {
            future.complete(((ReadTransactionSuccess) response).getData());
        } else {
            future.completeExceptionally(new IllegalArgumentException("Unhandled reply " + response));
        }
    }

    private CompletionStage<Optional<NormalizedNode<?, ?>>> invokeRead(final TransactionStateTracker tracker,
            final YangInstanceIdentifier path) {
        final CompletableFuture<Optional<NormalizedNode<?, ?>>> future = new CompletableFuture<>();
        parent.getClient().sendRequest(new ExistsTransactionRequest(tracker.getIdentifier(), tracker.nextSequence(),
            parent.getClient().self(), path), t -> completeRead(future, t));
        return future;
    }
}
