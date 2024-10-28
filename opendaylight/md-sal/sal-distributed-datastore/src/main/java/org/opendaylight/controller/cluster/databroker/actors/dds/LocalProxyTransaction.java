/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.FluentFuture;
import java.util.Optional;
import java.util.function.Consumer;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.access.commands.AbortLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.AbstractLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.CommitLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ExistsTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ExistsTransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.IncrementTransactionSequenceRequest;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ReadTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ReadTransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionPurgeRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.controller.cluster.access.concepts.RuntimeRequestException;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.util.AbstractDataTreeModificationCursor;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeModification;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link AbstractProxyTransaction} for dispatching a transaction towards a shard leader which is co-located with
 * the client instance. This class is NOT thread-safe.
 *
 * <p>It requires a {@link DataTreeSnapshot}, which is used to instantiated a new {@link DataTreeModification}.
 * Operations are then performed on this modification and once the transaction is submitted, the modification is sent
 * to the shard leader.
 *
 * <p>This class is not thread-safe as usual with transactions. Since it does not interact with the backend until the
 * transaction is submitted, at which point this class gets out of the picture, this is not a cause for concern.
 */
abstract sealed class LocalProxyTransaction extends AbstractProxyTransaction
        permits LocalReadOnlyProxyTransaction, LocalReadWriteProxyTransaction {
    private static final Logger LOG = LoggerFactory.getLogger(LocalProxyTransaction.class);

    private final @NonNull TransactionIdentifier identifier;

    LocalProxyTransaction(final ProxyHistory parent, final TransactionIdentifier identifier, final boolean isDone) {
        super(parent, isDone);
        this.identifier = requireNonNull(identifier);
    }

    @Override
    public final TransactionIdentifier getIdentifier() {
        return identifier;
    }

    abstract @NonNull DataTreeSnapshot readOnlyView();

    abstract void applyForwardedModifyTransactionRequest(ModifyTransactionRequest request,
            @Nullable Consumer<Response<?, ?>> callback);

    abstract void replayModifyTransactionRequest(ModifyTransactionRequest request,
            @Nullable Consumer<Response<?, ?>> callback, long enqueuedTicks);

    @Override
    FluentFuture<Boolean> doExists(final YangInstanceIdentifier path) {
        final boolean result;
        try {
            result = readOnlyView().readNode(path).isPresent();
        } catch (FailedDataTreeModificationException e) {
            return FluentFutures.immediateFailedFluentFuture(ReadFailedException.MAPPER.apply(e));
        }
        return FluentFutures.immediateBooleanFluentFuture(result);
    }

    @Override
    FluentFuture<Optional<NormalizedNode>> doRead(final YangInstanceIdentifier path) {
        final Optional<NormalizedNode> result;
        try {
            result = readOnlyView().readNode(path);
        } catch (FailedDataTreeModificationException e) {
            return FluentFutures.immediateFailedFluentFuture(ReadFailedException.MAPPER.apply(e));
        }
        return FluentFutures.immediateFluentFuture(result);
    }

    @Override
    final AbortLocalTransactionRequest abortRequest() {
        return new AbortLocalTransactionRequest(identifier, localActor());
    }

    @Override
    void handleReplayedLocalRequest(final AbstractLocalTransactionRequest<?> request,
            final Consumer<Response<?, ?>> callback, final long enqueuedTicks) {
        if (request instanceof AbortLocalTransactionRequest req) {
            enqueueAbort(req, callback, enqueuedTicks);
        } else {
            throw unhandledRequest(request);
        }
    }

    @Override
    void handleReplayedRemoteRequest(final TransactionRequest<?> request, final Consumer<Response<?, ?>> callback,
            final long enqueuedTicks) {
        switch (request) {
            case ModifyTransactionRequest req -> replayModifyTransactionRequest(req, callback, enqueuedTicks);
            case TransactionPurgeRequest req -> enqueuePurge(callback, enqueuedTicks);
            case IncrementTransactionSequenceRequest req -> {
                // Local transactions do not have non-replayable requests which would be visible to the backend,
                // hence we can skip sequence increments.
                LOG.debug("Not replaying {}", req);
            }
            default -> handleReadRequest(request, callback);
        }
    }

    /**
     * Remote-to-local equivalent of {@link #handleReplayedRemoteRequest(TransactionRequest, Consumer, long)},
     * except it is invoked in the forwarding path from
     * {@link RemoteProxyTransaction#forwardToLocal(LocalProxyTransaction, TransactionRequest, Consumer)}.
     *
     * @param request Forwarded request
     * @param callback Callback to be invoked once the request completes
     */
    void handleForwardedRemoteRequest(final TransactionRequest<?> request, final Consumer<Response<?, ?>> callback) {
        switch (request) {
            case ModifyTransactionRequest req -> applyForwardedModifyTransactionRequest(req, callback);
            case TransactionPurgeRequest req -> enqueuePurge(callback);
            default -> handleReadRequest(request, callback);
        }
    }

    @NonNull Response<?, ?> handleExistsRequest(final @NonNull DataTreeSnapshot snapshot,
            final @NonNull ExistsTransactionRequest request) {
        try {
            return new ExistsTransactionSuccess(request.getTarget(), request.getSequence(),
                snapshot.readNode(request.getPath()).isPresent());
        } catch (FailedDataTreeModificationException e) {
            return request.toRequestFailure(new RuntimeRequestException("Failed to access data",
                ReadFailedException.MAPPER.apply(e)));
        }
    }

    @NonNull Response<?, ?> handleReadRequest(final @NonNull DataTreeSnapshot snapshot,
            final @NonNull ReadTransactionRequest request) {
        try {
            return new ReadTransactionSuccess(request.getTarget(), request.getSequence(),
                snapshot.readNode(request.getPath()));
        } catch (FailedDataTreeModificationException e) {
            return request.toRequestFailure(new RuntimeRequestException("Failed to access data",
                ReadFailedException.MAPPER.apply(e)));
        }
    }

    private void handleReadRequest(final TransactionRequest<?> request, final Consumer<Response<?, ?>> callback) {
        // Note we delay completion of read requests to limit the scope at which the client can run, as they have
        // listeners, which we do not want to execute while we are reconnecting.
        switch (request) {
            case ReadTransactionRequest msg -> {
                if (callback != null) {
                    final var response = handleReadRequest(readOnlyView(), msg);
                    executeInActor(() -> callback.accept(response));
                }
            }
            case ExistsTransactionRequest req -> {
                if (callback != null) {
                    final var response = handleExistsRequest(readOnlyView(), req);
                    executeInActor(() -> callback.accept(response));
                }
            }
            default -> throw unhandledRequest(request);
        }
    }

    @Override
    final void forwardToRemote(final RemoteProxyTransaction successor, final TransactionRequest<?> request,
                         final Consumer<Response<?, ?>> callback) {
        switch (request) {
            case CommitLocalTransactionRequest req -> {
                final DataTreeModification mod = req.getModification();

                LOG.debug("Applying modification {} to successor {}", mod, successor);
                mod.applyToCursor(new AbstractDataTreeModificationCursor() {
                    @Override
                    public void write(final PathArgument child, final NormalizedNode data) {
                        successor.write(current().node(child), data);
                    }

                    @Override
                    public void merge(final PathArgument child, final NormalizedNode data) {
                        successor.merge(current().node(child), data);
                    }

                    @Override
                    public void delete(final PathArgument child) {
                        successor.delete(current().node(child));
                    }
                });

                successor.sealOnly();
                final ModifyTransactionRequest successorReq = successor.commitRequest(req.isCoordinated());
                successor.sendRequest(successorReq, callback);
            }
            case AbortLocalTransactionRequest req -> {
                LOG.debug("Forwarding abort {} to successor {}", req, successor);
                successor.abort();
            }
            case TransactionPurgeRequest req -> {
                LOG.debug("Forwarding purge {} to successor {}", req, successor);
                successor.enqueuePurge(callback);
            }
            case ModifyTransactionRequest req -> successor.handleForwardedRequest(req, callback);
            default -> throw unhandledRequest(request);
        }
    }

    @Override
    void forwardToLocal(final LocalProxyTransaction successor, final TransactionRequest<?> request,
            final Consumer<Response<?, ?>> callback) {
        switch (request) {
            case AbortLocalTransactionRequest msg -> successor.sendAbort(msg, callback);
            case TransactionPurgeRequest msg -> successor.enqueuePurge(callback);
            default -> throw unhandledRequest(request);
        }
        LOG.debug("Forwarded request {} to successor {}", request, successor);
    }

    void sendAbort(final AbortLocalTransactionRequest request, final Consumer<Response<?, ?>> callback) {
        sendRequest(request, callback);
    }

    void enqueueAbort(final AbortLocalTransactionRequest request, final Consumer<Response<?, ?>> callback,
            final long enqueuedTicks) {
        enqueueRequest(request, callback, enqueuedTicks);
    }
}
