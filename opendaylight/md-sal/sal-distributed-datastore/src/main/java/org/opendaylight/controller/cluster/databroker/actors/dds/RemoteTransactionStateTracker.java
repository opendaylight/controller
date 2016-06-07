/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import com.google.common.base.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.opendaylight.controller.cluster.access.commands.ExistsTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ExistsTransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequestBuilder;
import org.opendaylight.controller.cluster.access.commands.ReadTransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionDelete;
import org.opendaylight.controller.cluster.access.commands.TransactionMerge;
import org.opendaylight.controller.cluster.access.commands.TransactionModification;
import org.opendaylight.controller.cluster.access.commands.TransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionWrite;
import org.opendaylight.controller.cluster.access.concepts.RequestFailure;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class tracking messages exchanged with a particular remote backend shard. This implementation is used also when the
 * location of the shard leader is not currently known, as the logic relies on queueing implenented in base client actor,
 * which will route the requests once the leader becomes known.
 *
 * This class is not safe to access from multiple application threads, as is usual for transactions. Its internal state
 * transitions based on backend responses are thread-safe.
 *
 * @author Robert Varga
 */
final class RemoteTransactionStateTracker extends AbstractTransactionStateTracker {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteTransactionStateTracker.class);

    // FIXME: make this tuneable
    private static final int REQUEST_MAX_MODIFICATIONS = 1000;

    private final ModifyTransactionRequestBuilder builder;

    private boolean builderBusy;
    private long sequence;

    RemoteTransactionStateTracker(final DistributedDataStoreClientBehavior client,
        final TransactionIdentifier identifier) {
        super(client);
        builder = new ModifyTransactionRequestBuilder(identifier, client.self());
    }

    @Override
    public TransactionIdentifier getIdentifier() {
        return builder.getIdentifier();
    }

    @Override
    void delete(final YangInstanceIdentifier path) {
        appendModification(new TransactionDelete(path));
    }

    @Override
    void merge(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        appendModification(new TransactionMerge(path, data));
    }

    @Override
    void write(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        appendModification(new TransactionWrite(path, data));
    }

    @Override
    CompletionStage<Boolean> exists(final YangInstanceIdentifier path) {
        ensureFlushedBuider();

        final CompletableFuture<Boolean> future = new CompletableFuture<>();
        client().sendRequest(new ExistsTransactionRequest(getIdentifier(), nextSequence(), client().self(), path),
            t -> completeExists(future, t));

        return future;
    }

    @Override
    CompletionStage<Optional<NormalizedNode<?, ?>>> read(final YangInstanceIdentifier path) {
        ensureFlushedBuider();

        final CompletableFuture<Optional<NormalizedNode<?, ?>>> future = new CompletableFuture<>();
        client().sendRequest(new ExistsTransactionRequest(getIdentifier(), nextSequence(), client().self(), path),
            t -> completeRead(future, t));
        return future;
    }

    @Override
    void abort() {
        ensureInitializedBuider();
        builder.setAbort();
        flushBuilder();
    }

    private long nextSequence() {
        return sequence++;
    }

    private void ensureInitializedBuider() {
        if (!builderBusy) {
            builder.setSequence(nextSequence());
            builderBusy = true;
        }
    }

    private void ensureFlushedBuider() {
        if (builderBusy) {
            flushBuilder();
        }
    }

    private void flushBuilder() {
        client().sendRequest(builder.build(), this::completeModify);
        builderBusy = false;
    }

    private void appendModification(final TransactionModification modification) {
        ensureInitializedBuider();

        builder.addModification(modification);
        if (builder.size() >= REQUEST_MAX_MODIFICATIONS) {
            flushBuilder();
        }
    }

    private void completeModify(final Response<?, ?> response) {
        LOG.debug("Modification request completed with {}", response);

        if (response instanceof TransactionSuccess) {
            // Happy path no-op
        } else {
            recordFailedResponse(response);
        }
    }

    private Exception recordFailedResponse(final Response<?, ?> response) {
        final Exception failure;
        if (response instanceof RequestFailure) {
            failure = ((RequestFailure<?, ?>) response).getCause();
        } else {
            LOG.warn("Unhandled response {}", response);
            failure = new IllegalArgumentException("Unhandled response " + response.getClass());
        }

        // FIXME: record failure and decide when/how to report it.

        return failure;
    }

    private void failFuture(final CompletableFuture<?> future, final Response<?, ?> response) {
        future.completeExceptionally(recordFailedResponse(response));
    }

    private void completeExists(final CompletableFuture<Boolean> future, final Response<?, ?> response) {
        LOG.debug("Exists request completed with {}", response);

        if (response instanceof ExistsTransactionSuccess) {
            future.complete(((ExistsTransactionSuccess) response).getExists());
        } else {
            failFuture(future, response);
        }
    }

    private void completeRead(final CompletableFuture<Optional<NormalizedNode<?, ?>>> future, final Response<?, ?> response) {
        LOG.trace("Read request completed with {}", response);

        if (response instanceof ReadTransactionSuccess) {
            future.complete(((ReadTransactionSuccess) response).getData());
        } else {
            failFuture(future, response);
        }
    }
}
