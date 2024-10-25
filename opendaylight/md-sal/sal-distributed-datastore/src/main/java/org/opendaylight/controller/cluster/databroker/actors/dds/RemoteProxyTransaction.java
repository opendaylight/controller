/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static com.google.common.base.Verify.verify;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Consumer;
import org.opendaylight.controller.cluster.access.client.RequestTimeoutException;
import org.opendaylight.controller.cluster.access.commands.AbortLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.AbstractLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.AbstractReadTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.CommitLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ExistsTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ExistsTransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.IncrementTransactionSequenceRequest;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequestBuilder;
import org.opendaylight.controller.cluster.access.commands.PersistenceProtocol;
import org.opendaylight.controller.cluster.access.commands.ReadTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ReadTransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionAbortRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionDelete;
import org.opendaylight.controller.cluster.access.commands.TransactionDoCommitRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionMerge;
import org.opendaylight.controller.cluster.access.commands.TransactionModification;
import org.opendaylight.controller.cluster.access.commands.TransactionPreCommitRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionPurgeRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionWrite;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.RequestFailure;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.util.AbstractDataTreeModificationCursor;
import org.opendaylight.mdsal.common.api.DataStoreUnavailableException;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeModification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link AbstractProxyTransaction} for dispatching a transaction towards a shard leader whose location is currently
 * not known or is known to be not co-located with the client.
 *
 * <p>It packages operations and sends them via the client actor queue to the shard leader. That queue is responsible
 * for maintaining any submitted operations until the leader is discovered.
 *
 * <p>This class is not safe to access from multiple application threads, as is usual for transactions. Its internal
 * state transitions based on backend responses are thread-safe.
 */
final class RemoteProxyTransaction extends AbstractProxyTransaction {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteProxyTransaction.class);

    private final ModifyTransactionRequestBuilder builder;
    private final boolean sendReadyOnSeal;
    private final boolean snapshotOnly;
    private final int maxModifications;

    private boolean builderBusy;

    private volatile Exception operationFailure;

    RemoteProxyTransaction(final ProxyHistory parent, final TransactionIdentifier identifier,
            final boolean snapshotOnly, final boolean sendReadyOnSeal, final boolean isDone) {
        super(parent, isDone);
        this.snapshotOnly = snapshotOnly;
        this.sendReadyOnSeal = sendReadyOnSeal;
        builder = ModifyTransactionRequest.builder(identifier, localActor());
        maxModifications = parent.parent().actorUtils().getDatastoreContext().getShardBatchedModificationCount();
    }

    @Override
    boolean isSnapshotOnly() {
        return snapshotOnly;
    }

    @Override
    public TransactionIdentifier getIdentifier() {
        return builder.getIdentifier();
    }

    @Override
    void doDelete(final YangInstanceIdentifier path) {
        appendModification(new TransactionDelete(path), OptionalLong.empty());
    }

    @Override
    void doMerge(final YangInstanceIdentifier path, final NormalizedNode data) {
        appendModification(new TransactionMerge(path, data), OptionalLong.empty());
    }

    @Override
    void doWrite(final YangInstanceIdentifier path, final NormalizedNode data) {
        appendModification(new TransactionWrite(path, data), OptionalLong.empty());
    }

    private <T> FluentFuture<T> sendReadRequest(final AbstractReadTransactionRequest<?> request,
            final Consumer<Response<?, ?>> completer, final ListenableFuture<T> future) {
        // Check if a previous operation failed. If it has, do not bother sending anything and report a failure
        final Exception local = operationFailure;
        if (local != null) {
            return FluentFutures.immediateFailedFluentFuture(
                    new ReadFailedException("Previous operation failed", local));
        }

        // Make sure we send any modifications before issuing a read
        ensureFlushedBuider();
        sendRequest(request, completer);
        return FluentFuture.from(future);
    }

    @Override
    FluentFuture<Boolean> doExists(final YangInstanceIdentifier path) {
        final var future = SettableFuture.<Boolean>create();
        return sendReadRequest(new ExistsTransactionRequest(getIdentifier(), nextSequence(), localActor(), path,
            isSnapshotOnly()), t -> completeExists(path, future, t), future);
    }

    @Override
    FluentFuture<Optional<NormalizedNode>> doRead(final YangInstanceIdentifier path) {
        final var future = SettableFuture.<Optional<NormalizedNode>>create();
        return sendReadRequest(new ReadTransactionRequest(getIdentifier(), nextSequence(), localActor(), path,
            isSnapshotOnly()), t -> completeRead(path, future, t), future);
    }

    private void ensureInitializedBuilder() {
        if (!builderBusy) {
            builder.setSequence(nextSequence());
            builderBusy = true;
        }
    }

    private void ensureFlushedBuider() {
        ensureFlushedBuider(OptionalLong.empty());
    }

    private void ensureFlushedBuider(final OptionalLong enqueuedTicks) {
        if (builderBusy) {
            flushBuilder(enqueuedTicks);
        }
    }

    private void flushBuilder(final OptionalLong enqueuedTicks) {
        final var request = builder.build();
        builderBusy = false;

        sendModification(request, enqueuedTicks);
    }

    private void sendModification(final TransactionRequest<?> request, final OptionalLong enqueuedTicks) {
        if (enqueuedTicks.isPresent()) {
            enqueueRequest(request, response -> completeModify(request, response), enqueuedTicks.orElseThrow());
        } else {
            sendRequest(request, response -> completeModify(request, response));
        }
    }

    private void appendModification(final TransactionModification modification) {
        appendModification(modification, OptionalLong.empty());
    }

    private void appendModification(final TransactionModification modification, final OptionalLong enqueuedTicks) {
        if (operationFailure == null) {
            ensureInitializedBuilder();

            builder.addModification(modification);
            if (builder.size() >= maxModifications) {
                flushBuilder(enqueuedTicks);
            }
        } else {
            LOG.debug("Transaction {} failed, not attempting further transactions", getIdentifier());
        }
    }

    private void completeModify(final TransactionRequest<?> request, final Response<?, ?> response) {
        LOG.debug("Modification request {} completed with {}", request, response);

        if (response instanceof TransactionSuccess) {
            // Happy path
            recordSuccessfulRequest(request);
        } else {
            recordFailedResponse(response);
        }
    }

    private Exception recordFailedResponse(final Response<?, ?> response) {
        final Exception failure;
        if (response instanceof RequestFailure<?, ?> requestFailure) {
            final RequestException cause = requestFailure.getCause();
            failure = cause instanceof RequestTimeoutException
                    ? new DataStoreUnavailableException(cause.getMessage(), cause) : cause;
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

    private void failReadFuture(final SettableFuture<?> future, final String message,
            final Response<?, ?> response) {
        future.setException(new ReadFailedException(message, recordFailedResponse(response)));
    }

    private void completeExists(final YangInstanceIdentifier path, final SettableFuture<Boolean> future,
            final Response<?, ?> response) {
        LOG.debug("Exists request for {} completed with {}", path, response);

        if (response instanceof ExistsTransactionSuccess success) {
            future.set(success.getExists());
        } else {
            failReadFuture(future, "Error executing exists request for path " + path, response);
        }

        recordFinishedRequest(response);
    }

    private void completeRead(final YangInstanceIdentifier path, final SettableFuture<Optional<NormalizedNode>> future,
            final Response<?, ?> response) {
        LOG.debug("Read request for {} completed with {}", path, response);

        if (response instanceof ReadTransactionSuccess success) {
            future.set(success.getData());
        } else {
            failReadFuture(future, "Error reading data for path " + path, response);
        }

        recordFinishedRequest(response);
    }

    @Override
    ModifyTransactionRequest abortRequest() {
        ensureInitializedBuilder();
        builder.setAbort();
        builderBusy = false;
        return builder.build();
    }

    @Override
    ModifyTransactionRequest commitRequest(final boolean coordinated) {
        ensureInitializedBuilder();
        builder.setCommit(coordinated);
        builderBusy = false;
        return builder.build();
    }

    private ModifyTransactionRequest readyRequest() {
        ensureInitializedBuilder();
        builder.setReady();
        builderBusy = false;
        return builder.build();
    }

    @Override
    boolean sealAndSend(final OptionalLong enqueuedTicks) {
        if (sendReadyOnSeal) {
            ensureInitializedBuilder();
            builder.setReady();
            flushBuilder(enqueuedTicks);
        }
        return super.sealAndSend(enqueuedTicks);
    }

    @Override
    Optional<ModifyTransactionRequest> flushState() {
        if (!builderBusy) {
            return Optional.empty();
        }

        final ModifyTransactionRequest request = builder.build();
        builderBusy = false;
        return Optional.of(request);
    }

    @Override
    void forwardToRemote(final RemoteProxyTransaction successor, final TransactionRequest<?> request,
            final Consumer<Response<?, ?>> callback) {
        successor.handleForwardedRequest(request, callback);
    }

    void handleForwardedRequest(final TransactionRequest<?> request, final Consumer<Response<?, ?>> callback) {
        switch (request) {
            case ModifyTransactionRequest modify -> handleForwardedModifyTransactionRequest(callback, modify);
            case ReadTransactionRequest read -> {
                ensureFlushedBuider();
                sendRequest(new ReadTransactionRequest(getIdentifier(), nextSequence(), localActor(),
                    read.getPath(), isSnapshotOnly()), resp -> {
                        recordFinishedRequest(resp);
                        callback.accept(resp);
                    });
            }
            case ExistsTransactionRequest exists -> {
                ensureFlushedBuider();
                sendRequest(new ExistsTransactionRequest(getIdentifier(), nextSequence(), localActor(),
                    exists.getPath(), isSnapshotOnly()), resp -> {
                        recordFinishedRequest(resp);
                        callback.accept(resp);
                    });
            }
            case TransactionPreCommitRequest preCommit -> {
                ensureFlushedBuider();
                final var tmp = new TransactionPreCommitRequest(getIdentifier(), nextSequence(), localActor());
                sendRequest(tmp, resp -> {
                    recordSuccessfulRequest(tmp);
                    callback.accept(resp);
                });
            }
            case TransactionDoCommitRequest doCommit -> {
                ensureFlushedBuider();
                sendRequest(new TransactionDoCommitRequest(getIdentifier(), nextSequence(), localActor()), callback);
            }
            case TransactionAbortRequest abort -> {
                ensureFlushedBuider();
                sendDoAbort(callback);
            }
            case TransactionPurgeRequest purge -> enqueuePurge(callback);
            default -> throw unhandledRequest(request);
        }
    }

    private void handleForwardedModifyTransactionRequest(final Consumer<Response<?, ?>> callback,
            final ModifyTransactionRequest req) {
        req.getModifications().forEach(this::appendModification);

        final Optional<PersistenceProtocol> maybeProto = req.getPersistenceProtocol();
        if (maybeProto.isPresent()) {
            // Persistence protocol implies we are sealed, propagate the marker, but hold off doing other actions
            // until we know what we are going to do.
            if (markSealed()) {
                if (!sealOnly()) {
                    LOG.debug("Proxy {} has a successor, which should receive seal through a separate request", this);
                }
            }

            final TransactionRequest<?> tmp;
            switch (maybeProto.orElseThrow()) {
                case ABORT:
                    tmp = abortRequest();
                    sendRequest(tmp, resp -> {
                        completeModify(tmp, resp);
                        callback.accept(resp);
                    });
                    break;
                case SIMPLE:
                    tmp = commitRequest(false);
                    sendRequest(tmp, resp -> {
                        completeModify(tmp, resp);
                        callback.accept(resp);
                    });
                    break;
                case THREE_PHASE:
                    tmp = commitRequest(true);
                    sendRequest(tmp, resp -> {
                        recordSuccessfulRequest(tmp);
                        callback.accept(resp);
                    });
                    break;
                case READY:
                    tmp = readyRequest();
                    sendRequest(tmp, resp -> {
                        recordSuccessfulRequest(tmp);
                        callback.accept(resp);
                    });
                    break;
                default:
                    throw new IllegalArgumentException("Unhandled protocol " + maybeProto.orElseThrow());
            }
        }
    }

    @Override
    void forwardToLocal(final LocalProxyTransaction successor, final TransactionRequest<?> request,
            final Consumer<Response<?, ?>> callback) {
        successor.handleForwardedRemoteRequest(request, callback);
    }

    @Override
    void handleReplayedLocalRequest(final AbstractLocalTransactionRequest<?> request,
            final Consumer<Response<?, ?>> callback, final long enqueuedTicks) {
        if (request instanceof CommitLocalTransactionRequest commitRequest) {
            replayLocalCommitRequest(commitRequest, callback, enqueuedTicks);
        } else if (request instanceof AbortLocalTransactionRequest) {
            enqueueRequest(abortRequest(), callback, enqueuedTicks);
        } else {
            throw unhandledRequest(request);
        }
    }

    private void replayLocalCommitRequest(final CommitLocalTransactionRequest request,
            final Consumer<Response<?, ?>> callback, final long enqueuedTicks) {
        final DataTreeModification mod = request.getModification();
        final OptionalLong optTicks = OptionalLong.of(enqueuedTicks);

        mod.applyToCursor(new AbstractDataTreeModificationCursor() {
            @Override
            public void write(final PathArgument child, final NormalizedNode data) {
                appendModification(new TransactionWrite(current().node(child), data), optTicks);
            }

            @Override
            public void merge(final PathArgument child, final NormalizedNode data) {
                appendModification(new TransactionMerge(current().node(child), data), optTicks);
            }

            @Override
            public void delete(final PathArgument child) {
                appendModification(new TransactionDelete(current().node(child)), optTicks);
            }
        });

        enqueueRequest(commitRequest(request.isCoordinated()), callback, enqueuedTicks);
    }

    @Override
    void handleReplayedRemoteRequest(final TransactionRequest<?> request, final Consumer<Response<?, ?>> callback,
            final long enqueuedTicks) {
        final Consumer<Response<?, ?>> cb = callback != null ? callback : resp -> { /* NOOP */ };
        final OptionalLong optTicks = OptionalLong.of(enqueuedTicks);

        switch (request) {
            case ModifyTransactionRequest modify -> handleReplayedModifyTransactionRequest(enqueuedTicks, cb, modify);
            case ReadTransactionRequest read -> {
                ensureFlushedBuider(optTicks);
                enqueueRequest(new ReadTransactionRequest(getIdentifier(), nextSequence(), localActor(), read.getPath(),
                    isSnapshotOnly()), resp -> {
                        recordFinishedRequest(resp);
                        cb.accept(resp);
                    }, enqueuedTicks);
            }
            case ExistsTransactionRequest exists -> {
                ensureFlushedBuider(optTicks);
                enqueueRequest(new ExistsTransactionRequest(getIdentifier(), nextSequence(), localActor(),
                    exists.getPath(), isSnapshotOnly()), resp -> {
                        recordFinishedRequest(resp);
                        cb.accept(resp);
                    }, enqueuedTicks);
            }
            case TransactionPreCommitRequest preCommit -> {
                ensureFlushedBuider(optTicks);
                final var tmp = new TransactionPreCommitRequest(getIdentifier(), nextSequence(), localActor());
                enqueueRequest(tmp, resp -> {
                    recordSuccessfulRequest(tmp);
                    cb.accept(resp);
                }, enqueuedTicks);
            }
            case TransactionDoCommitRequest doCommit -> {
                ensureFlushedBuider(optTicks);
                enqueueRequest(new TransactionDoCommitRequest(getIdentifier(), nextSequence(), localActor()), callback,
                    enqueuedTicks);
            }
            case TransactionAbortRequest abort -> {
                ensureFlushedBuider(optTicks);
                enqueueDoAbort(callback, enqueuedTicks);
            }
            case TransactionPurgeRequest purge -> enqueuePurge(callback, enqueuedTicks);
            case IncrementTransactionSequenceRequest req -> {
                ensureFlushedBuider(optTicks);
                enqueueRequest(new IncrementTransactionSequenceRequest(getIdentifier(), nextSequence(), localActor(),
                    snapshotOnly, req.getIncrement()), callback, enqueuedTicks);
                incrementSequence(req.getIncrement());
            }
            default -> throw unhandledRequest(request);
        }
    }

    private void handleReplayedModifyTransactionRequest(final long enqueuedTicks, final Consumer<Response<?, ?>> cb,
            final ModifyTransactionRequest req) {
        req.getModifications().forEach(this::appendModification);

        final Optional<PersistenceProtocol> maybeProto = req.getPersistenceProtocol();
        if (maybeProto.isPresent()) {
            // Persistence protocol implies we are sealed, propagate the marker, but hold off doing other actions
            // until we know what we are going to do.
            if (markSealed()) {
                verify(sealOnly(), "Attempted to replay seal on %s", this);
            }

            final TransactionRequest<?> tmp;
            switch (maybeProto.orElseThrow()) {
                case ABORT:
                    tmp = abortRequest();
                    enqueueRequest(tmp, resp -> {
                        completeModify(tmp, resp);
                        cb.accept(resp);
                    }, enqueuedTicks);
                    break;
                case SIMPLE:
                    tmp = commitRequest(false);
                    enqueueRequest(tmp, resp -> {
                        completeModify(tmp, resp);
                        cb.accept(resp);
                    }, enqueuedTicks);
                    break;
                case THREE_PHASE:
                    tmp = commitRequest(true);
                    enqueueRequest(tmp, resp -> {
                        recordSuccessfulRequest(tmp);
                        cb.accept(resp);
                    }, enqueuedTicks);
                    break;
                case READY:
                    tmp = readyRequest();
                    enqueueRequest(tmp, resp -> {
                        recordSuccessfulRequest(tmp);
                        cb.accept(resp);
                    }, enqueuedTicks);
                    break;
                default:
                    throw new IllegalArgumentException("Unhandled protocol " + maybeProto.orElseThrow());
            }
        }
    }
}
