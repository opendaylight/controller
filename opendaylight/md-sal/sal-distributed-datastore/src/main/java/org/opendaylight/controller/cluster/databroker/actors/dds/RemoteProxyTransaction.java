/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.function.Consumer;
import org.opendaylight.controller.cluster.access.commands.AbstractReadTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ExistsTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ExistsTransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequestBuilder;
import org.opendaylight.controller.cluster.access.commands.ReadTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ReadTransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionDelete;
import org.opendaylight.controller.cluster.access.commands.TransactionMerge;
import org.opendaylight.controller.cluster.access.commands.TransactionModification;
import org.opendaylight.controller.cluster.access.commands.TransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionWrite;
import org.opendaylight.controller.cluster.access.concepts.RequestFailure;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.yangtools.util.concurrent.MappingCheckedFuture;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link AbstractProxyTransaction} for dispatching a transaction towards a shard leader whose location is currently
 * not known or is known to be not co-located with the client.
 *
 * It packages operations and sends them via the client actor queue to the shard leader. That queue is responsible for
 * maintaining any submitted operations until the leader is discovered.
 *
 * This class is not safe to access from multiple application threads, as is usual for transactions. Its internal state
 * transitions based on backend responses are thread-safe.
 *
 * @author Robert Varga
 */
final class RemoteProxyTransaction extends AbstractProxyTransaction {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteProxyTransaction.class);

    // FIXME: make this tuneable
    private static final int REQUEST_MAX_MODIFICATIONS = 1000;

    private final ModifyTransactionRequestBuilder builder;

    private boolean builderBusy;

    private volatile Exception operationFailure;

    RemoteProxyTransaction(final DistributedDataStoreClientBehavior client,
        final TransactionIdentifier identifier) {
        super(client);
        builder = new ModifyTransactionRequestBuilder(identifier, client.self());
    }

    @Override
    public TransactionIdentifier getIdentifier() {
        return builder.getIdentifier();
    }

    @Override
    void doDelete(final YangInstanceIdentifier path) {
        appendModification(new TransactionDelete(path));
    }

    @Override
    void doMerge(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        appendModification(new TransactionMerge(path, data));
    }

    @Override
    void doWrite(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        appendModification(new TransactionWrite(path, data));
    }

    private <T> CheckedFuture<T, ReadFailedException> sendReadRequest(final AbstractReadTransactionRequest<?> request,
            final Consumer<Response<?, ?>> completer, final ListenableFuture<T> future) {
        // Check if a previous operation failed. If it has, do not bother sending anything and report a failure
        final Exception local = operationFailure;
        if (local != null) {
            return Futures.immediateFailedCheckedFuture(new ReadFailedException("Previous operation failed", local));
        }

        // Make sure we send any modifications before issuing a read
        ensureFlushedBuider();
        client().sendRequest(nextSequence(), request, completer);
        return MappingCheckedFuture.create(future, ReadFailedException.MAPPER);
    }

    @Override
    CheckedFuture<Boolean, ReadFailedException> doExists(final YangInstanceIdentifier path) {
        final SettableFuture<Boolean> future = SettableFuture.create();
        return sendReadRequest(new ExistsTransactionRequest(getIdentifier(), client().self(), path),
            t -> completeExists(future, t), future);
    }

    @Override
    CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> doRead(final YangInstanceIdentifier path) {
        final SettableFuture<Optional<NormalizedNode<?, ?>>> future = SettableFuture.create();
        return sendReadRequest(new ReadTransactionRequest(getIdentifier(), client().self(), path),
            t -> completeRead(future, t), future);
    }

    @Override
    void doAbort() {
        ensureInitializedBuider();
        builder.setAbort();
        flushBuilder();
    }

    private void ensureInitializedBuider() {
        if (!builderBusy) {
            builderBusy = true;
        }
    }

    private void ensureFlushedBuider() {
        if (builderBusy) {
            flushBuilder();
        }
    }

    private void flushBuilder() {
        client().sendRequest(nextSequence(), builder.build(), this::completeModify);
        builderBusy = false;
    }

    private void appendModification(final TransactionModification modification) {
        if (operationFailure == null) {
            ensureInitializedBuider();

            builder.addModification(modification);
            if (builder.size() >= REQUEST_MAX_MODIFICATIONS) {
                flushBuilder();
            }
        } else {
            LOG.debug("Transaction {} failed, not attempting further transactions", getIdentifier());
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

        if (operationFailure == null) {
            LOG.debug("Transaction {} failed", getIdentifier(), failure);
            operationFailure = failure;
        }
        return failure;
    }

    private void failFuture(final SettableFuture<?> future, final Response<?, ?> response) {
        future.setException(recordFailedResponse(response));
    }

    private void completeExists(final SettableFuture<Boolean> future, final Response<?, ?> response) {
        LOG.debug("Exists request completed with {}", response);

        if (response instanceof ExistsTransactionSuccess) {
            future.set(((ExistsTransactionSuccess) response).getExists());
        } else {
            failFuture(future, response);
        }
    }

    private void completeRead(final SettableFuture<Optional<NormalizedNode<?, ?>>> future, final Response<?, ?> response) {
        LOG.debug("Read request completed with {}", response);

        if (response instanceof ReadTransactionSuccess) {
            future.set(((ReadTransactionSuccess) response).getData());
        } else {
            failFuture(future, response);
        }
    }

    @Override
    ModifyTransactionRequest doCommit(final boolean coordinated) {
        ensureInitializedBuider();
        builder.setCommit(coordinated);

        final ModifyTransactionRequest ret = builder.build();
        builderBusy = false;
        return ret;
    }

    @Override
    void doSeal() {
        // No-op
    }
}
