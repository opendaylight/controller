/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.cluster.access.commands.CommitLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.PersistenceProtocol;
import org.opendaylight.controller.cluster.access.commands.TransactionAbortRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionDelete;
import org.opendaylight.controller.cluster.access.commands.TransactionDoCommitRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionMerge;
import org.opendaylight.controller.cluster.access.commands.TransactionModification;
import org.opendaylight.controller.cluster.access.commands.TransactionPreCommitRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionWrite;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.util.AbstractDataTreeModificationCursor;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.CursorAwareDataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.CursorAwareDataTreeSnapshot;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModificationCursor;
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
final class LocalReadWriteProxyTransaction extends LocalProxyTransaction {
    private static final Logger LOG = LoggerFactory.getLogger(LocalReadWriteProxyTransaction.class);

    private CursorAwareDataTreeModification modification;
    private CursorAwareDataTreeModification sealedModification;

    LocalReadWriteProxyTransaction(final ProxyHistory parent, final TransactionIdentifier identifier,
        final DataTreeSnapshot snapshot) {
        super(parent, identifier);
        this.modification = (CursorAwareDataTreeModification) snapshot.newModification();
    }

    @Override
    boolean isSnapshotOnly() {
        return false;
    }

    @Override
    CursorAwareDataTreeSnapshot readOnlyView() {
        return modification;
    }

    @Override
    void doDelete(final YangInstanceIdentifier path) {
        modification.delete(path);
    }

    @Override
    void doMerge(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        modification.merge(path, data);
    }

    @Override
    void doWrite(final YangInstanceIdentifier path, final NormalizedNode<?, ?> data) {
        modification.write(path, data);
    }

    private RuntimeException abortedException() {
        return new IllegalStateException("Tracker " + getIdentifier() + " has been aborted");
    }

    private RuntimeException submittedException() {
        return new IllegalStateException("Tracker " + getIdentifier() + " has been submitted");
    }

    @Override
    CommitLocalTransactionRequest commitRequest(final boolean coordinated) {
        final CommitLocalTransactionRequest ret = new CommitLocalTransactionRequest(getIdentifier(), nextSequence(),
            localActor(), modification, coordinated);
        modification = new FailedDataTreeModification(this::submittedException);
        return ret;
    }

    @Override
    void doSeal() {
        modification.ready();
        sealedModification = modification;
    }

    @Override
    void flushState(final AbstractProxyTransaction successor) {
        sealedModification.applyToCursor(new AbstractDataTreeModificationCursor() {
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
    }

    DataTreeSnapshot getSnapshot() {
        Preconditions.checkState(sealedModification != null, "Proxy %s is not sealed yet", getIdentifier());
        return sealedModification;
    }

    @Override
    void applyModifyTransactionRequest(final ModifyTransactionRequest request,
            final @Nullable Consumer<Response<?, ?>> callback) {
        for (final TransactionModification mod : request.getModifications()) {
            if (mod instanceof TransactionWrite) {
                modification.write(mod.getPath(), ((TransactionWrite)mod).getData());
            } else if (mod instanceof TransactionMerge) {
                modification.merge(mod.getPath(), ((TransactionMerge)mod).getData());
            } else if (mod instanceof TransactionDelete) {
                modification.delete(mod.getPath());
            } else {
                throw new IllegalArgumentException("Unsupported modification " + mod);
            }
        }

        final java.util.Optional<PersistenceProtocol> maybeProtocol = request.getPersistenceProtocol();
        if (maybeProtocol.isPresent()) {
            Verify.verify(callback != null, "Request {} has null callback", request);
            ensureSealed();

            switch (maybeProtocol.get()) {
                case ABORT:
                    sendAbort(callback);
                    break;
                case READY:
                    // No-op, as we have already issued a seal()
                    break;
                case SIMPLE:
                    sendRequest(commitRequest(false), callback);
                    break;
                case THREE_PHASE:
                    sendRequest(commitRequest(true), callback);
                    break;
                default:
                    throw new IllegalArgumentException("Unhandled protocol " + maybeProtocol.get());
            }
        }
    }

    @Override
    void handleForwardedRemoteRequest(final TransactionRequest<?> request,
            final @Nullable Consumer<Response<?, ?>> callback) {
        LOG.debug("Applying forwarded request {}", request);

        if (request instanceof TransactionPreCommitRequest) {
            sendRequest(new TransactionPreCommitRequest(getIdentifier(), nextSequence(), localActor()), callback);
        } else if (request instanceof TransactionDoCommitRequest) {
            sendRequest(new TransactionDoCommitRequest(getIdentifier(), nextSequence(), localActor()), callback);
        } else if (request instanceof TransactionAbortRequest) {
            sendAbort(callback);
        } else {
            super.handleForwardedRemoteRequest(request, callback);
        }
    }

    @Override
    void forwardToLocal(final LocalProxyTransaction successor, final TransactionRequest<?> request,
            final Consumer<Response<?, ?>> callback) {
        if (request instanceof CommitLocalTransactionRequest) {
            Verify.verify(successor instanceof LocalReadWriteProxyTransaction);
            ((LocalReadWriteProxyTransaction) successor).sendCommit((CommitLocalTransactionRequest)request, callback);
            LOG.debug("Forwarded request {} to successor {}", request, successor);
        } else {
            super.forwardToLocal(successor, request, callback);
        }
    }

    @Override
    void sendAbort(final TransactionRequest<?> request, final Consumer<Response<?, ?>> callback) {
        super.sendAbort(request, callback);
        modification = new FailedDataTreeModification(this::abortedException);
    }

    private void sendCommit(final CommitLocalTransactionRequest request, final Consumer<Response<?, ?>> callback) {
        // Rebase old modification on new data tree.
        try (DataTreeModificationCursor cursor = modification.createCursor(YangInstanceIdentifier.EMPTY)) {
            request.getModification().applyToCursor(cursor);
        }

        ensureSealed();
        sendRequest(commitRequest(request.isCoordinated()), callback);
    }
}
