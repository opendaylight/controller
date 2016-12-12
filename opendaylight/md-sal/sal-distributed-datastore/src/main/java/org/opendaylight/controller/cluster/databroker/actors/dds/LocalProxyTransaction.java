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
import java.util.function.Consumer;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.cluster.access.commands.AbortLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.CommitLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ExistsTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ExistsTransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ReadTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ReadTransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.util.AbstractDataTreeModificationCursor;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link AbstractProxyTransaction} for dispatching a transaction towards a shard leader which is co-located with
 * the client instance.
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
@NotThreadSafe
abstract class LocalProxyTransaction extends AbstractProxyTransaction {
    private static final Logger LOG = LoggerFactory.getLogger(LocalProxyTransaction.class);

    private final TransactionIdentifier identifier;

    LocalProxyTransaction(final ProxyHistory parent, final TransactionIdentifier identifier) {
        super(parent);
        this.identifier = Preconditions.checkNotNull(identifier);
    }

    @Override
    public final TransactionIdentifier getIdentifier() {
        return identifier;
    }

    abstract DataTreeSnapshot readOnlyView();

    abstract void applyModifyTransactionRequest(ModifyTransactionRequest request,
            @Nullable Consumer<Response<?, ?>> callback);

    @Override
    final CheckedFuture<Boolean, ReadFailedException> doExists(final YangInstanceIdentifier path) {
        return Futures.immediateCheckedFuture(readOnlyView().readNode(path).isPresent());
    }

    @Override
    final CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> doRead(final YangInstanceIdentifier path) {
        return Futures.immediateCheckedFuture(readOnlyView().readNode(path));
    }

    @Override
    final void doAbort() {
        sendAbort(new AbortLocalTransactionRequest(identifier, localActor()), response -> {
            LOG.debug("Transaction {} abort completed with {}", identifier, response);
        });
    }

    @Override
    void handleForwardedRemoteRequest(final TransactionRequest<?> request,
            final @Nullable Consumer<Response<?, ?>> callback) {
        if (request instanceof ModifyTransactionRequest) {
            applyModifyTransactionRequest((ModifyTransactionRequest) request, callback);
        } else if (request instanceof ReadTransactionRequest) {
            final YangInstanceIdentifier path = ((ReadTransactionRequest) request).getPath();
            final Optional<NormalizedNode<?, ?>> result = readOnlyView().readNode(path);
            callback.accept(new ReadTransactionSuccess(request.getTarget(), request.getSequence(), result));
        } else if (request instanceof ExistsTransactionRequest) {
            final YangInstanceIdentifier path = ((ExistsTransactionRequest) request).getPath();
            final boolean result = readOnlyView().readNode(path).isPresent();
            callback.accept(new ExistsTransactionSuccess(request.getTarget(), request.getSequence(), result));
        } else {
            throw new IllegalArgumentException("Unhandled request " + request);
        }
    }

    @Override
    void forwardToRemote(final RemoteProxyTransaction successor, final TransactionRequest<?> request,
            final Consumer<Response<?, ?>> callback) {
        if (request instanceof CommitLocalTransactionRequest) {
            final CommitLocalTransactionRequest req = (CommitLocalTransactionRequest) request;
            final DataTreeModification mod = req.getModification();

            LOG.debug("Applying modification {} to successor {}", mod, successor);
            mod.applyToCursor(new AbstractDataTreeModificationCursor() {
                @Override
                public void write(final PathArgument child, final NormalizedNode<?, ?> data) {
                    successor.write(current().node(child), data);
                }

                @Override
                public void merge(final PathArgument child, final NormalizedNode<?, ?> data) {
                    successor.merge(current().node(child), data);
                }

                @Override
                public void delete(final PathArgument child) {
                    successor.delete(current().node(child));
                }
            });

            successor.ensureSealed();

            final ModifyTransactionRequest successorReq = successor.commitRequest(req.isCoordinated());
            successor.sendRequest(successorReq, callback);
        } else if (request instanceof AbortLocalTransactionRequest) {
            LOG.debug("Forwarding abort {} to successor {}", request, successor);
            successor.abort();
        } else {
            throw new IllegalArgumentException("Unhandled request" + request);
        }
    }

    @Override
    void forwardToLocal(final LocalProxyTransaction successor, final TransactionRequest<?> request,
            final Consumer<Response<?, ?>> callback) {
        if (request instanceof AbortLocalTransactionRequest) {
            successor.sendAbort(request, callback);
        } else {
            throw new IllegalArgumentException("Unhandled request" + request);
        }

        LOG.debug("Forwarded request {} to successor {}", request, successor);
    }

    void sendAbort(final TransactionRequest<?> request, final Consumer<Response<?, ?>> callback) {
        sendRequest(request, callback);
    }
}
