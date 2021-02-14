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
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.access.commands.AbortLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.AbstractLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.CommitLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequestBuilder;
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
final class LocalReadWriteProxyTransaction extends LocalProxyTransaction {
    private static final Logger LOG = LoggerFactory.getLogger(LocalReadWriteProxyTransaction.class);

    /**
     * This field needs to be accessed via {@link #getModification()}, which performs state checking to ensure
     * the modification can actually be accessed.
     */
    private final CursorAwareDataTreeModification modification;

    private Supplier<? extends RuntimeException> closedException;

    private CursorAwareDataTreeModification sealedModification;

    /**
     * Recorded failure from previous operations. Normally we would want to propagate the error directly to the
     * offending call site, but that exposes inconsistency in behavior during initial connection, when we go through
     * {@link RemoteProxyTransaction}, which detects this sort of issues at canCommit/directCommit time on the backend.
     *
     * <p>
     * We therefore do not report incurred exceptions directly, but report them once the user attempts to commit
     * this transaction.
     */
    private Exception recordedFailure;

    LocalReadWriteProxyTransaction(final ProxyHistory parent, final TransactionIdentifier identifier,
        final DataTreeSnapshot snapshot) {
        super(parent, identifier, false);
        this.modification = (CursorAwareDataTreeModification) snapshot.newModification();
    }

    LocalReadWriteProxyTransaction(final ProxyHistory parent, final TransactionIdentifier identifier) {
        super(parent, identifier, true);
        // This is DONE transaction, this should never be touched
        this.modification = null;
    }

    @Override
    boolean isSnapshotOnly() {
        return false;
    }

    @Override
    CursorAwareDataTreeSnapshot readOnlyView() {
        return getModification();
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    void doDelete(final YangInstanceIdentifier path) {
        final CursorAwareDataTreeModification mod = getModification();
        if (recordedFailure != null) {
            LOG.debug("Transaction {} recorded failure, ignoring delete of {}", getIdentifier(), path);
            return;
        }

        try {
            mod.delete(path);
        } catch (Exception e) {
            LOG.debug("Transaction {} delete on {} incurred failure, delaying it until commit", getIdentifier(), path,
                e);
            recordedFailure = e;
        }
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    void doMerge(final YangInstanceIdentifier path, final NormalizedNode data) {
        final CursorAwareDataTreeModification mod = getModification();
        if (recordedFailure != null) {
            LOG.debug("Transaction {} recorded failure, ignoring merge to {}", getIdentifier(), path);
            return;
        }

        try {
            mod.merge(path, data);
        } catch (Exception e) {
            LOG.debug("Transaction {} merge to {} incurred failure, delaying it until commit", getIdentifier(), path,
                e);
            recordedFailure = e;
        }
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    void doWrite(final YangInstanceIdentifier path, final NormalizedNode data) {
        final CursorAwareDataTreeModification mod = getModification();
        if (recordedFailure != null) {
            LOG.debug("Transaction {} recorded failure, ignoring write to {}", getIdentifier(), path);
            return;
        }

        try {
            mod.write(path, data);
        } catch (Exception e) {
            LOG.debug("Transaction {} write to {} incurred failure, delaying it until commit", getIdentifier(), path,
                e);
            recordedFailure = e;
        }
    }

    private RuntimeException abortedException() {
        return new IllegalStateException("Tracker " + getIdentifier() + " has been aborted");
    }

    private RuntimeException submittedException() {
        return new IllegalStateException("Tracker " + getIdentifier() + " has been submitted");
    }

    @Override
    CommitLocalTransactionRequest commitRequest(final boolean coordinated) {
        final CursorAwareDataTreeModification mod = getModification();
        final CommitLocalTransactionRequest ret = new CommitLocalTransactionRequest(getIdentifier(), nextSequence(),
            localActor(), mod, recordedFailure, coordinated);
        closedException = this::submittedException;
        return ret;
    }

    private void sealModification() {
        Preconditions.checkState(sealedModification == null, "Transaction %s is already sealed", this);
        final CursorAwareDataTreeModification mod = getModification();
        mod.ready();
        sealedModification = mod;
    }

    @Override
    boolean sealOnly() {
        sealModification();
        return super.sealOnly();
    }

    @Override
    boolean sealAndSend(final OptionalLong enqueuedTicks) {
        sealModification();
        return super.sealAndSend(enqueuedTicks);
    }

    @Override
    Optional<ModifyTransactionRequest> flushState() {
        final ModifyTransactionRequestBuilder b = new ModifyTransactionRequestBuilder(getIdentifier(), localActor());
        b.setSequence(0);

        sealedModification.applyToCursor(new AbstractDataTreeModificationCursor() {
            @Override
            public void write(final PathArgument child, final NormalizedNode data) {
                b.addModification(new TransactionWrite(current().node(child), data));
            }

            @Override
            public void merge(final PathArgument child, final NormalizedNode data) {
                b.addModification(new TransactionMerge(current().node(child), data));
            }

            @Override
            public void delete(final PathArgument child) {
                b.addModification(new TransactionDelete(current().node(child)));
            }
        });

        return Optional.of(b.build());
    }

    DataTreeSnapshot getSnapshot() {
        Preconditions.checkState(sealedModification != null, "Proxy %s is not sealed yet", getIdentifier());
        return sealedModification;
    }

    @Override
    void applyForwardedModifyTransactionRequest(final ModifyTransactionRequest request,
            final Consumer<Response<?, ?>> callback) {
        commonModifyTransactionRequest(request, callback, this::sendRequest);
    }

    @Override
    void replayModifyTransactionRequest(final ModifyTransactionRequest request,
            final Consumer<Response<?, ?>> callback, final long enqueuedTicks) {
        commonModifyTransactionRequest(request, callback, (req, cb) -> enqueueRequest(req, cb, enqueuedTicks));
    }

    private void commonModifyTransactionRequest(final ModifyTransactionRequest request,
            final @Nullable Consumer<Response<?, ?>> callback,
            final BiConsumer<TransactionRequest<?>, Consumer<Response<?, ?>>> sendMethod) {
        for (final TransactionModification mod : request.getModifications()) {
            if (mod instanceof TransactionWrite) {
                write(mod.getPath(), ((TransactionWrite)mod).getData());
            } else if (mod instanceof TransactionMerge) {
                merge(mod.getPath(), ((TransactionMerge)mod).getData());
            } else if (mod instanceof TransactionDelete) {
                delete(mod.getPath());
            } else {
                throw new IllegalArgumentException("Unsupported modification " + mod);
            }
        }

        final Optional<PersistenceProtocol> maybeProtocol = request.getPersistenceProtocol();
        if (maybeProtocol.isPresent()) {
            Verify.verify(callback != null, "Request %s has null callback", request);
            if (markSealed()) {
                sealOnly();
            }

            switch (maybeProtocol.get()) {
                case ABORT:
                    sendMethod.accept(new AbortLocalTransactionRequest(getIdentifier(), localActor()), callback);
                    break;
                case READY:
                    // No-op, as we have already issued a sealOnly() and we are not transmitting anything
                    break;
                case SIMPLE:
                    sendMethod.accept(commitRequest(false), callback);
                    break;
                case THREE_PHASE:
                    sendMethod.accept(commitRequest(true), callback);
                    break;
                default:
                    throw new IllegalArgumentException("Unhandled protocol " + maybeProtocol.get());
            }
        }
    }

    @Override
    void handleReplayedLocalRequest(final AbstractLocalTransactionRequest<?> request,
            final Consumer<Response<?, ?>> callback, final long now) {
        if (request instanceof CommitLocalTransactionRequest) {
            enqueueRequest(rebaseCommit((CommitLocalTransactionRequest)request), callback, now);
        } else {
            super.handleReplayedLocalRequest(request, callback, now);
        }
    }

    @Override
    void handleReplayedRemoteRequest(final TransactionRequest<?> request,
            final Consumer<Response<?, ?>> callback, final long enqueuedTicks) {
        LOG.debug("Applying replayed request {}", request);

        if (request instanceof TransactionPreCommitRequest) {
            enqueueRequest(new TransactionPreCommitRequest(getIdentifier(), nextSequence(), localActor()), callback,
                enqueuedTicks);
        } else if (request instanceof TransactionDoCommitRequest) {
            enqueueRequest(new TransactionDoCommitRequest(getIdentifier(), nextSequence(), localActor()), callback,
                enqueuedTicks);
        } else if (request instanceof TransactionAbortRequest) {
            enqueueDoAbort(callback, enqueuedTicks);
        } else {
            super.handleReplayedRemoteRequest(request, callback, enqueuedTicks);
        }
    }

    @Override
    void handleForwardedRemoteRequest(final TransactionRequest<?> request, final Consumer<Response<?, ?>> callback) {
        LOG.debug("Applying forwarded request {}", request);

        if (request instanceof TransactionPreCommitRequest) {
            sendRequest(new TransactionPreCommitRequest(getIdentifier(), nextSequence(), localActor()), callback);
        } else if (request instanceof TransactionDoCommitRequest) {
            sendRequest(new TransactionDoCommitRequest(getIdentifier(), nextSequence(), localActor()), callback);
        } else if (request instanceof TransactionAbortRequest) {
            sendDoAbort(callback);
        } else {
            super.handleForwardedRemoteRequest(request, callback);
        }
    }

    @Override
    void forwardToLocal(final LocalProxyTransaction successor, final TransactionRequest<?> request,
            final Consumer<Response<?, ?>> callback) {
        if (request instanceof CommitLocalTransactionRequest) {
            Verify.verify(successor instanceof LocalReadWriteProxyTransaction);
            ((LocalReadWriteProxyTransaction) successor).sendRebased((CommitLocalTransactionRequest)request, callback);
            LOG.debug("Forwarded request {} to successor {}", request, successor);
        } else {
            super.forwardToLocal(successor, request, callback);
        }
    }

    @Override
    void sendAbort(final TransactionRequest<?> request, final Consumer<Response<?, ?>> callback) {
        super.sendAbort(request, callback);
        closedException = this::abortedException;
    }

    @Override
    void enqueueAbort(final TransactionRequest<?> request, final Consumer<Response<?, ?>> callback,
            final long enqueuedTicks) {
        super.enqueueAbort(request, callback, enqueuedTicks);
        closedException = this::abortedException;
    }

    private @NonNull CursorAwareDataTreeModification getModification() {
        if (closedException != null) {
            throw closedException.get();
        }

        return Preconditions.checkNotNull(modification, "Transaction %s is DONE", getIdentifier());
    }

    private void sendRebased(final CommitLocalTransactionRequest request, final Consumer<Response<?, ?>> callback) {
        sendRequest(rebaseCommit(request), callback);
    }

    private CommitLocalTransactionRequest rebaseCommit(final CommitLocalTransactionRequest request) {
        // Rebase old modification on new data tree.
        final CursorAwareDataTreeModification mod = getModification();

        try (DataTreeModificationCursor cursor = mod.openCursor()) {
            request.getModification().applyToCursor(cursor);
        }

        if (markSealed()) {
            sealOnly();
        }

        return commitRequest(request.isCoordinated());
    }
}
