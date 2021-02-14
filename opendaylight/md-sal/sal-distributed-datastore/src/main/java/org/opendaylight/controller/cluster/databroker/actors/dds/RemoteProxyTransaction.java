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
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link AbstractProxyTransaction} for dispatching a transaction towards a shard leader whose location is currently
 * not known or is known to be not co-located with the client.
 *
 * <p>
 * It packages operations and sends them via the client actor queue to the shard leader. That queue is responsible for
 * maintaining any submitted operations until the leader is discovered.
 *
 * <p>
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
    private final boolean sendReadyOnSeal;
    private final boolean snapshotOnly;

    private boolean builderBusy;

    private volatile Exception operationFailure;

    RemoteProxyTransaction(final ProxyHistory parent, final TransactionIdentifier identifier,
            final boolean snapshotOnly, final boolean sendReadyOnSeal, final boolean isDone) {
        super(parent, isDone);
        this.snapshotOnly = snapshotOnly;
        this.sendReadyOnSeal = sendReadyOnSeal;
        builder = new ModifyTransactionRequestBuilder(identifier, localActor());
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
        final SettableFuture<Boolean> future = SettableFuture.create();
        return sendReadRequest(new ExistsTransactionRequest(getIdentifier(), nextSequence(), localActor(), path,
            isSnapshotOnly()), t -> completeExists(path, future, t), future);
    }

    @Override
    FluentFuture<Optional<NormalizedNode>> doRead(final YangInstanceIdentifier path) {
        final SettableFuture<Optional<NormalizedNode>> future = SettableFuture.create();
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
        final ModifyTransactionRequest request = builder.build();
        builderBusy = false;

        sendModification(request, enqueuedTicks);
    }

    private void sendModification(final TransactionRequest<?> request, final OptionalLong enqueuedTicks) {
        if (enqueuedTicks.isPresent()) {
            enqueueRequest(request, response -> completeModify(request, response), enqueuedTicks.getAsLong());
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
            if (builder.size() >= REQUEST_MAX_MODIFICATIONS) {
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
        if (response instanceof RequestFailure) {
            final RequestException cause = ((RequestFailure<?, ?>) response).getCause();
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

        if (response instanceof ExistsTransactionSuccess) {
            future.set(((ExistsTransactionSuccess) response).getExists());
        } else {
            failReadFuture(future, "Error executing exists request for path " + path, response);
        }

        recordFinishedRequest(response);
    }

    private void completeRead(final YangInstanceIdentifier path, final SettableFuture<Optional<NormalizedNode>> future,
            final Response<?, ?> response) {
        LOG.debug("Read request for {} completed with {}", path, response);

        if (response instanceof ReadTransactionSuccess) {
            future.set(((ReadTransactionSuccess) response).getData());
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
        if (request instanceof ModifyTransactionRequest) {
            handleForwardedModifyTransactionRequest(callback, (ModifyTransactionRequest) request);
        } else if (request instanceof ReadTransactionRequest) {
            ensureFlushedBuider();
            sendRequest(new ReadTransactionRequest(getIdentifier(), nextSequence(), localActor(),
                ((ReadTransactionRequest) request).getPath(), isSnapshotOnly()), resp -> {
                    recordFinishedRequest(resp);
                    callback.accept(resp);
                });
        } else if (request instanceof ExistsTransactionRequest) {
            ensureFlushedBuider();
            sendRequest(new ExistsTransactionRequest(getIdentifier(), nextSequence(), localActor(),
                ((ExistsTransactionRequest) request).getPath(), isSnapshotOnly()), resp -> {
                    recordFinishedRequest(resp);
                    callback.accept(resp);
                });
        } else if (request instanceof TransactionPreCommitRequest) {
            ensureFlushedBuider();
            final TransactionRequest<?> tmp = new TransactionPreCommitRequest(getIdentifier(), nextSequence(),
                localActor());
            sendRequest(tmp, resp -> {
                recordSuccessfulRequest(tmp);
                callback.accept(resp);
            });
        } else if (request instanceof TransactionDoCommitRequest) {
            ensureFlushedBuider();
            sendRequest(new TransactionDoCommitRequest(getIdentifier(), nextSequence(), localActor()), callback);
        } else if (request instanceof TransactionAbortRequest) {
            ensureFlushedBuider();
            sendDoAbort(callback);
        } else if (request instanceof TransactionPurgeRequest) {
            enqueuePurge(callback);
        } else {
            throw new IllegalArgumentException("Unhandled request {}" + request);
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
            switch (maybeProto.get()) {
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
                    throw new IllegalArgumentException("Unhandled protocol " + maybeProto.get());
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
        if (request instanceof CommitLocalTransactionRequest) {
            replayLocalCommitRequest((CommitLocalTransactionRequest) request, callback, enqueuedTicks);
        } else if (request instanceof AbortLocalTransactionRequest) {
            enqueueRequest(abortRequest(), callback, enqueuedTicks);
        } else {
            throw new IllegalStateException("Unhandled request " + request);
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

        if (request instanceof ModifyTransactionRequest) {
            handleReplayedModifyTransactionRequest(enqueuedTicks, cb, (ModifyTransactionRequest) request);
        } else if (request instanceof ReadTransactionRequest) {
            ensureFlushedBuider(optTicks);
            enqueueRequest(new ReadTransactionRequest(getIdentifier(), nextSequence(), localActor(),
                ((ReadTransactionRequest) request).getPath(), isSnapshotOnly()), resp -> {
                    recordFinishedRequest(resp);
                    cb.accept(resp);
                }, enqueuedTicks);
        } else if (request instanceof ExistsTransactionRequest) {
            ensureFlushedBuider(optTicks);
            enqueueRequest(new ExistsTransactionRequest(getIdentifier(), nextSequence(), localActor(),
                ((ExistsTransactionRequest) request).getPath(), isSnapshotOnly()), resp -> {
                    recordFinishedRequest(resp);
                    cb.accept(resp);
                }, enqueuedTicks);
        } else if (request instanceof TransactionPreCommitRequest) {
            ensureFlushedBuider(optTicks);
            final TransactionRequest<?> tmp = new TransactionPreCommitRequest(getIdentifier(), nextSequence(),
                localActor());
            enqueueRequest(tmp, resp -> {
                recordSuccessfulRequest(tmp);
                cb.accept(resp);
            }, enqueuedTicks);
        } else if (request instanceof TransactionDoCommitRequest) {
            ensureFlushedBuider(optTicks);
            enqueueRequest(new TransactionDoCommitRequest(getIdentifier(), nextSequence(), localActor()), callback,
                enqueuedTicks);
        } else if (request instanceof TransactionAbortRequest) {
            ensureFlushedBuider(optTicks);
            enqueueDoAbort(callback, enqueuedTicks);
        } else if (request instanceof TransactionPurgeRequest) {
            enqueuePurge(callback, enqueuedTicks);
        } else if (request instanceof IncrementTransactionSequenceRequest) {
            final IncrementTransactionSequenceRequest req = (IncrementTransactionSequenceRequest) request;
            ensureFlushedBuider(optTicks);
            enqueueRequest(new IncrementTransactionSequenceRequest(getIdentifier(), nextSequence(), localActor(),
                snapshotOnly, req.getIncrement()), callback, enqueuedTicks);
            incrementSequence(req.getIncrement());
        } else {
            throw new IllegalArgumentException("Unhandled request {}" + request);
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
            switch (maybeProto.get()) {
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
                    throw new IllegalArgumentException("Unhandled protocol " + maybeProto.get());
            }
        }
    }
}
