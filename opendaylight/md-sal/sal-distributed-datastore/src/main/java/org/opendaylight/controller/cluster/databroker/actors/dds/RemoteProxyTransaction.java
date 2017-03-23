/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.access.commands.AbstractReadTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ExistsTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ExistsTransactionSuccess;
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
            final boolean snapshotOnly, final boolean sendReadyOnSeal) {
        super(parent);
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
        sendRequest(request, completer);
        return MappingCheckedFuture.create(future, ReadFailedException.MAPPER);
    }

    @Override
    CheckedFuture<Boolean, ReadFailedException> doExists(final YangInstanceIdentifier path) {
        final SettableFuture<Boolean> future = SettableFuture.create();
        return sendReadRequest(new ExistsTransactionRequest(getIdentifier(), nextSequence(), localActor(), path,
            isSnapshotOnly()), t -> completeExists(future, t), future);
    }

    @Override
    CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> doRead(final YangInstanceIdentifier path) {
        final SettableFuture<Optional<NormalizedNode<?, ?>>> future = SettableFuture.create();
        return sendReadRequest(new ReadTransactionRequest(getIdentifier(), nextSequence(), localActor(), path,
            isSnapshotOnly()), t -> completeRead(future, t), future);
    }

    @Override
    void doAbort() {
        ensureInitializedBuilder();
        builder.setAbort();
        flushBuilder();
    }

    private void ensureInitializedBuilder() {
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
        final ModifyTransactionRequest request = builder.build();
        builderBusy = false;

        sendModification(request);
    }

    private void sendModification(final TransactionRequest<?> request) {
        sendRequest(request, response -> completeModify(request, response));
    }

    @Override
    void handleForwardedRemoteRequest(final TransactionRequest<?> request,
            final @Nullable Consumer<Response<?, ?>> callback) {
        nextSequence();

        if (callback == null) {
            sendModification(request);
            return;
        }

        /*
         * FindBugs is utterly stupid, as it does not recognize the fact that we have checked for null
         * and reports NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE in the lambda below.
         */
        final Consumer<Response<?, ?>> findBugsIsStupid = callback;

        // FIXME: do not use sendRequest() once we have throttling in place, as we have already waited the
        //        period required to get into the queue.
        sendRequest(request, response -> {
            findBugsIsStupid.accept(Preconditions.checkNotNull(response));
            completeModify(request, response);
        });
    }

    private void appendModification(final TransactionModification modification) {
        if (operationFailure == null) {
            ensureInitializedBuilder();

            builder.addModification(modification);
            if (builder.size() >= REQUEST_MAX_MODIFICATIONS) {
                flushBuilder();
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

        recordFinishedRequest();
    }

    private void completeRead(final SettableFuture<Optional<NormalizedNode<?, ?>>> future,
            final Response<?, ?> response) {
        LOG.debug("Read request completed with {}", response);

        if (response instanceof ReadTransactionSuccess) {
            future.set(((ReadTransactionSuccess) response).getData());
        } else {
            failFuture(future, response);
        }

        recordFinishedRequest();
    }

    @Override
    ModifyTransactionRequest commitRequest(final boolean coordinated) {
        ensureInitializedBuilder();
        builder.setCommit(coordinated);

        final ModifyTransactionRequest ret = builder.build();
        builderBusy = false;
        return ret;
    }

    @Override
    void doSeal() {
        if (sendReadyOnSeal) {
            ensureInitializedBuilder();
            builder.setReady();
            flushBuilder();
        }
    }

    @Override
    void flushState(final AbstractProxyTransaction successor) {
        if (builderBusy) {
            final ModifyTransactionRequest request = builder.build();
            builderBusy = false;
            successor.handleForwardedRemoteRequest(request, null);
        }
    }

    @Override
    void forwardToRemote(final RemoteProxyTransaction successor, final TransactionRequest<?> request,
            final Consumer<Response<?, ?>> callback) {
        successor.handleForwardedRequest(request, callback);
    }

    private void handleForwardedRequest(final TransactionRequest<?> request, final Consumer<Response<?, ?>> callback) {
        if (request instanceof ModifyTransactionRequest) {
            final ModifyTransactionRequest req = (ModifyTransactionRequest) request;

            req.getModifications().forEach(this::appendModification);

            final java.util.Optional<PersistenceProtocol> maybeProto = req.getPersistenceProtocol();
            if (maybeProto.isPresent()) {
                ensureSealed();

                switch (maybeProto.get()) {
                    case ABORT:
                        sendAbort(callback);
                        break;
                    case SIMPLE:
                        sendRequest(commitRequest(false), callback);
                        break;
                    case THREE_PHASE:
                        sendRequest(commitRequest(true), callback);
                        break;
                    case READY:
                        //no op
                        break;
                    default:
                        throw new IllegalArgumentException("Unhandled protocol " + maybeProto.get());
                }
            }
        } else if (request instanceof ReadTransactionRequest) {
            ensureFlushedBuider();
            sendRequest(new ReadTransactionRequest(getIdentifier(), nextSequence(), localActor(),
                ((ReadTransactionRequest) request).getPath(), isSnapshotOnly()), callback);
        } else if (request instanceof ExistsTransactionRequest) {
            ensureFlushedBuider();
            sendRequest(new ExistsTransactionRequest(getIdentifier(), nextSequence(), localActor(),
                ((ExistsTransactionRequest) request).getPath(), isSnapshotOnly()), callback);
        } else if (request instanceof TransactionPreCommitRequest) {
            ensureFlushedBuider();
            sendRequest(new TransactionPreCommitRequest(getIdentifier(), nextSequence(), localActor()), callback);
        } else if (request instanceof TransactionDoCommitRequest) {
            ensureFlushedBuider();
            sendRequest(new TransactionDoCommitRequest(getIdentifier(), nextSequence(), localActor()), callback);
        } else if (request instanceof TransactionAbortRequest) {
            ensureFlushedBuider();
            sendAbort(callback);
        } else if (request instanceof TransactionPurgeRequest) {
            purge();
        } else {
            throw new IllegalArgumentException("Unhandled request {}" + request);
        }
    }

    @Override
    void forwardToLocal(final LocalProxyTransaction successor, final TransactionRequest<?> request,
            final Consumer<Response<?, ?>> callback) {
        successor.handleForwardedRemoteRequest(request, callback);
    }
}
