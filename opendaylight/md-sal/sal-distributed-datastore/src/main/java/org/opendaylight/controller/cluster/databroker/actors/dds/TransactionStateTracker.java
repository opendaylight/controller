/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import akka.actor.ActorRef;
import com.google.common.base.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import org.opendaylight.controller.cluster.access.commands.ExistsTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ExistsTransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.ReadTransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.RequestFailure;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Class tracking messages exchanged with a particular backend shard.
 *
 * @author Robert Varga
 */
final class TransactionStateTracker implements Identifiable<TransactionIdentifier> {
    private final TransactionIdentifier identifier;
    private long sequence;

    TransactionStateTracker(final LocalHistoryIdentifier historyId, final long transactionId) {
        this.identifier = new TransactionIdentifier(historyId, transactionId);
    }

    @Override
    public TransactionIdentifier getIdentifier() {
        return identifier;
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

    CompletionStage<Boolean> invokeExists(final ActorRef replyTo, final YangInstanceIdentifier path) {
        final CompletableFuture<Boolean> future = new CompletableFuture<>();
        sendRequest(new ExistsTransactionRequest(identifier, sequence++, replyTo, path), t -> completeExists(future, t));
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

    CompletionStage<Optional<NormalizedNode<?, ?>>> invokeRead(final ActorRef replyTo, final YangInstanceIdentifier path) {
        final CompletableFuture<Optional<NormalizedNode<?, ?>>> future = new CompletableFuture<>();
        sendRequest(new ExistsTransactionRequest(identifier, sequence++, replyTo, path), t -> completeRead(future, t));
        return future;
    }

    private void sendRequest(final TransactionRequest<?> request, final Consumer<Response<?, ?>> completer) {
        // TODO Auto-generated method stub
    }
}
