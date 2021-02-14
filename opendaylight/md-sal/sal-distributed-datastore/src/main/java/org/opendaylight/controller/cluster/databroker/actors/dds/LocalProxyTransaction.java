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
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.util.AbstractDataTreeModificationCursor;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link AbstractProxyTransaction} for dispatching a transaction towards a shard leader which is co-located with
 * the client instance. This class is NOT thread-safe.
 *
 * <p>
 * It requires a {@link DataTreeSnapshot}, which is used to instantiated a new {@link DataTreeModification}. Operations
 * are then performed on this modification and once the transaction is submitted, the modification is sent to the shard
 * leader.
 *
 * <p>
 * This class is not thread-safe as usual with transactions. Since it does not interact with the backend until the
 * transaction is submitted, at which point this class gets out of the picture, this is not a cause for concern.
 *
 * @author Robert Varga
 */
abstract class LocalProxyTransaction extends AbstractProxyTransaction {
    private static final Logger LOG = LoggerFactory.getLogger(LocalProxyTransaction.class);

    private final TransactionIdentifier identifier;

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
    final FluentFuture<Boolean> doExists(final YangInstanceIdentifier path) {
        return FluentFutures.immediateFluentFuture(readOnlyView().readNode(path).isPresent());
    }

    @Override
    final FluentFuture<Optional<NormalizedNode>> doRead(final YangInstanceIdentifier path) {
        return FluentFutures.immediateFluentFuture(readOnlyView().readNode(path));
    }

    @Override
    final AbortLocalTransactionRequest abortRequest() {
        return new AbortLocalTransactionRequest(identifier, localActor());
    }

    @Override
    void handleReplayedLocalRequest(final AbstractLocalTransactionRequest<?> request,
            final Consumer<Response<?, ?>> callback, final long enqueuedTicks) {
        if (request instanceof AbortLocalTransactionRequest) {
            enqueueAbort(request, callback, enqueuedTicks);
        } else {
            throw new IllegalArgumentException("Unhandled request" + request);
        }
    }

    private boolean handleReadRequest(final TransactionRequest<?> request, final Consumer<Response<?, ?>> callback) {
        // Note we delay completion of read requests to limit the scope at which the client can run, as they have
        // listeners, which we do not want to execute while we are reconnecting.
        if (request instanceof ReadTransactionRequest) {
            final YangInstanceIdentifier path = ((ReadTransactionRequest) request).getPath();
            final Optional<NormalizedNode> result = readOnlyView().readNode(path);
            if (callback != null) {
                // XXX: FB does not see that callback is final, on stack and has be check for non-null.
                final Consumer<Response<?, ?>> fbIsStupid = requireNonNull(callback);
                executeInActor(() -> fbIsStupid.accept(new ReadTransactionSuccess(request.getTarget(),
                    request.getSequence(), result)));
            }
            return true;
        } else if (request instanceof ExistsTransactionRequest) {
            final YangInstanceIdentifier path = ((ExistsTransactionRequest) request).getPath();
            final boolean result = readOnlyView().readNode(path).isPresent();
            if (callback != null) {
                // XXX: FB does not see that callback is final, on stack and has be check for non-null.
                final Consumer<Response<?, ?>> fbIsStupid = requireNonNull(callback);
                executeInActor(() -> fbIsStupid.accept(new ExistsTransactionSuccess(request.getTarget(),
                    request.getSequence(), result)));
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    void handleReplayedRemoteRequest(final TransactionRequest<?> request, final Consumer<Response<?, ?>> callback,
            final long enqueuedTicks) {
        if (request instanceof ModifyTransactionRequest) {
            replayModifyTransactionRequest((ModifyTransactionRequest) request, callback, enqueuedTicks);
        } else if (handleReadRequest(request, callback)) {
            // No-op
        } else if (request instanceof TransactionPurgeRequest) {
            enqueuePurge(callback, enqueuedTicks);
        } else if (request instanceof IncrementTransactionSequenceRequest) {
            // Local transactions do not have non-replayable requests which would be visible to the backend,
            // hence we can skip sequence increments.
            LOG.debug("Not replaying {}", request);
        } else {
            throw new IllegalArgumentException("Unhandled request " + request);
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
        if (request instanceof ModifyTransactionRequest) {
            applyForwardedModifyTransactionRequest((ModifyTransactionRequest) request, callback);
        } else if (handleReadRequest(request, callback)) {
            // No-op
        } else if (request instanceof TransactionPurgeRequest) {
            enqueuePurge(callback);
        } else {
            throw new IllegalArgumentException("Unhandled request " + request);
        }
    }

    @Override
    final void forwardToRemote(final RemoteProxyTransaction successor, final TransactionRequest<?> request,
                         final Consumer<Response<?, ?>> callback) {
        if (request instanceof CommitLocalTransactionRequest) {
            final CommitLocalTransactionRequest req = (CommitLocalTransactionRequest) request;
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
        } else if (request instanceof AbortLocalTransactionRequest) {
            LOG.debug("Forwarding abort {} to successor {}", request, successor);
            successor.abort();
        } else if (request instanceof TransactionPurgeRequest) {
            LOG.debug("Forwarding purge {} to successor {}", request, successor);
            successor.enqueuePurge(callback);
        } else if (request instanceof ModifyTransactionRequest) {
            successor.handleForwardedRequest(request, callback);
        } else {
            throwUnhandledRequest(request);
        }
    }

    @Override
    void forwardToLocal(final LocalProxyTransaction successor, final TransactionRequest<?> request,
            final Consumer<Response<?, ?>> callback) {
        if (request instanceof AbortLocalTransactionRequest) {
            successor.sendAbort(request, callback);
        } else if (request instanceof TransactionPurgeRequest) {
            successor.enqueuePurge(callback);
        } else {
            throwUnhandledRequest(request);
        }

        LOG.debug("Forwarded request {} to successor {}", request, successor);
    }

    private static void throwUnhandledRequest(final TransactionRequest<?> request) {
        throw new IllegalArgumentException("Unhandled request" + request);
    }

    void sendAbort(final TransactionRequest<?> request, final Consumer<Response<?, ?>> callback) {
        sendRequest(request, callback);
    }

    void enqueueAbort(final TransactionRequest<?> request, final Consumer<Response<?, ?>> callback,
            final long enqueuedTicks) {
        enqueueRequest(request, callback, enqueuedTicks);
    }
}
