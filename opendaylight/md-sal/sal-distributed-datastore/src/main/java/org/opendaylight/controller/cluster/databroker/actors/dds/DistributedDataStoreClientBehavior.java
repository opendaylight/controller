/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import akka.actor.ActorRef;
import akka.actor.Status;
import com.google.common.base.Verify;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.Nonnull;
import org.opendaylight.controller.cluster.access.commands.CommitLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.access.concepts.WritableIdentifier;
import org.opendaylight.controller.cluster.datastore.actors.client.ClientActorBehavior;
import org.opendaylight.controller.cluster.datastore.actors.client.ClientActorContext;
import org.opendaylight.controller.cluster.datastore.actors.client.BackendInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ClientActorBehavior} acting as an intermediary between the backend actors and the DistributedDataStore
 * frontend.
 *
 * This class is not visible outside of this package because it breaks the actor containment. Services provided to
 * Java world outside of actor containment are captured in {@link DistributedDataStoreClient}.
 *
 * IMPORTANT: this class breaks actor containment via methods implementing {@link DistributedDataStoreClient} contract.
 *            When touching internal state, be mindful of the execution context from which execution context, Actor
 *            or POJO, is the state being accessed or modified.
 *
 * THREAD SAFETY: this class must always be kept thread-safe, so that both the Actor System thread and the application
 *                threads can run concurrently. All state transitions must be made in a thread-safe manner. When in
 *                doubt, feel free to synchronize on this object.
 *
 * PERFORMANCE: this class lies in a performance-critical fast path. All code needs to be concise and efficient, but
 *              performance must not come at the price of correctness. Any optimizations need to be carefully analyzed
 *              for correctness and performance impact.
 *
 * TRADE-OFFS: part of the functionality runs in application threads without switching contexts, which makes it ideal
 *             for performing work and charging applications for it. That has two positive effects:
 *             - CPU usage is distributed across applications, minimizing work done in the actor thread
 *             - CPU usage provides back-pressure towards the application.
 *
 * @author Robert Varga
 */
final class DistributedDataStoreClientBehavior extends ClientActorBehavior implements DistributedDataStoreClient {
    private static final Logger LOG = LoggerFactory.getLogger(DistributedDataStoreClientBehavior.class);

    private final Map<TransactionIdentifier, ClientSingleTransaction> transactions = new HashMap<>();
    private final Map<LocalHistoryIdentifier, ClientLocalHistory> histories = new HashMap<>();
    private final SingleClientHistory singleHistory;
    private long nextHistoryId = 1;
    private long nextTransactionId = 0;

    DistributedDataStoreClientBehavior(final ClientActorContext context) {
        super(context);
        singleHistory = new SingleClientHistory(new LocalHistoryIdentifier(getIdentifier(), 0));
    }

    //
    //
    // Methods below are invoked from the client actor thread
    //
    //

    @Override
    protected void haltClient(final Throwable cause) {
        // FIXME: Add state flushing here once we have state
    }

    private ClientActorBehavior createLocalHistory(final CompletableFuture<ClientLocalHistory> future) {
        final LocalHistoryIdentifier historyId = new LocalHistoryIdentifier(getIdentifier(), nextHistoryId++);
        final ClientLocalHistory history = new ClientLocalHistory(this, historyId);
        LOG.debug("{}: creating a new local history {}", persistenceId(), history);

        Verify.verify(histories.put(historyId, history) == null);
        future.complete(history);
        return this;
    }

    private ClientActorBehavior createTransaction(final CompletableFuture<ClientSingleTransaction> future) {
        final TransactionIdentifier txId = new TransactionIdentifier(singleHistory.getIdentifier(), nextTransactionId++);
        final ClientSingleTransaction tx = new ClientSingleTransaction(this, singleHistory, txId);
        LOG.debug("{}: creating a new transaction {}", persistenceId(), tx);

        Verify.verify(transactions.put(txId, tx) == null);
        future.complete(tx);
        return this;
    }


    private ClientActorBehavior shutdown() {
        // FIXME: Add shutdown procedures here
        return null;
    }

    @Override
    protected ClientActorBehavior onCommand(final Object command) {
        if (command instanceof GetClientRequest) {
            ((GetClientRequest) command).getReplyTo().tell(new Status.Success(this), ActorRef.noSender());
        } else {
            LOG.warn("{}: ignoring unhandled command {}", persistenceId(), command);
        }

        return this;
    }

    @Override
    protected <I extends WritableIdentifier, T extends Request<I, T>> Request<I, ?> updateRequest(
            final @Nonnull BackendInfo info, final @Nonnull T request) {
        if (CommitLocalTransactionRequest.class.isInstance(request)) {
            // FIXME: check for locality and convert to remote
            throw new UnsupportedOperationException("FIXME: this conversion needs to be implemented");
        }

        return super.updateRequest(info, request);
    }

    //
    //
    // Methods below are invoked from application threads
    //
    //

    @Override
    public CompletionStage<ClientLocalHistory> createLocalHistory() {
        final CompletableFuture<ClientLocalHistory> future = new CompletableFuture<>();
        context().executeInActor(() -> createLocalHistory(future));
        return future;
    }

    @Override
    public CompletionStage<ClientSingleTransaction> createTransaction() {
        final CompletableFuture<ClientSingleTransaction> future = new CompletableFuture<>();
        context().executeInActor(() -> createTransaction(future));
        return future;
    }

    @Override
    public void close() {
        context().executeInActor(this::shutdown);
    }

}
