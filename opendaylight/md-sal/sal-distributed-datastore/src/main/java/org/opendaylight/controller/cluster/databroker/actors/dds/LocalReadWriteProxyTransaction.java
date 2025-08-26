/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Verify.verifyNotNull;

import com.google.common.base.VerifyException;
import com.google.common.util.concurrent.FluentFuture;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import org.opendaylight.controller.cluster.access.commands.ExistsTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ReadTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionAbortRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionDelete;
import org.opendaylight.controller.cluster.access.commands.TransactionDoCommitRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionMerge;
import org.opendaylight.controller.cluster.access.commands.TransactionPreCommitRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionWrite;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.controller.cluster.access.concepts.RuntimeRequestException;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.util.AbstractDataTreeModificationCursor;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.tree.api.CursorAwareDataTreeModification;
import org.opendaylight.yangtools.yang.data.tree.api.CursorAwareDataTreeSnapshot;
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
     * <p>We therefore do not report incurred exceptions directly, but report them once the user attempts to commit
     * this transaction.
     */
    private Exception recordedFailure;

    @SuppressWarnings("checkstyle:IllegalCatch")
    LocalReadWriteProxyTransaction(final ProxyHistory parent, final TransactionIdentifier identifier,
            final DataTreeSnapshot snapshot) {
        super(parent, identifier, false);

        if (snapshot instanceof FailedDataTreeModification failed) {
            recordedFailure = failed.cause();
            modification = failed;
        } else {
            CursorAwareDataTreeModification mod;
            try {
                mod = (CursorAwareDataTreeModification) snapshot.newModification();
            } catch (Exception e) {
                LOG.debug("Failed to instantiate modification for {}", identifier, e);
                recordedFailure = e;
                mod = new FailedDataTreeModification(snapshot.modelContext(), e);
            }
            modification = mod;
        }
    }

    LocalReadWriteProxyTransaction(final ProxyHistory parent, final TransactionIdentifier identifier) {
        super(parent, identifier, true);
        // This is DONE transaction, this should never be touched
        modification = null;
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
    FluentFuture<Boolean> doExists(final YangInstanceIdentifier path) {
        final var ex = recordedFailure;
        return ex == null ? super.doExists(path)
            : FluentFutures.immediateFailedFluentFuture(ReadFailedException.MAPPER.apply(ex));
    }

    @Override
    FluentFuture<Optional<NormalizedNode>> doRead(final YangInstanceIdentifier path) {
        final var ex = recordedFailure;
        return ex == null ? super.doRead(path)
            : FluentFutures.immediateFailedFluentFuture(ReadFailedException.MAPPER.apply(ex));
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    void doDelete(final YangInstanceIdentifier path) {
        final var mod = getModification();
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
        final var mod = getModification();
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
        final var mod = getModification();
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
        final var mod = getModification();
        final var ret = new CommitLocalTransactionRequest(getIdentifier(), nextSequence(), localActor(), mod,
            recordedFailure, coordinated);
        closedException = this::submittedException;
        return ret;
    }

    private void sealModification() {
        checkState(sealedModification == null, "Transaction %s is already sealed", this);
        final var mod = getModification();
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
        final var builder = ModifyTransactionRequest.builder(getIdentifier(), localActor()).setSequence(0);

        sealedModification.applyToCursor(new AbstractDataTreeModificationCursor() {
            @Override
            public void write(final PathArgument child, final NormalizedNode data) {
                builder.addWrite(current().node(child), data);
            }

            @Override
            public void merge(final PathArgument child, final NormalizedNode data) {
                builder.addMerge(current().node(child), data);
            }

            @Override
            public void delete(final PathArgument child) {
                builder.addDelete(current().node(child));
            }
        });

        return Optional.of(builder.build());
    }

    CursorAwareDataTreeSnapshot getSnapshot() {
        checkState(sealedModification != null, "Proxy %s is not sealed yet", getIdentifier());
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
        for (var mod : request.getModifications()) {
            switch (mod) {
                case TransactionDelete delete -> delete(delete.getPath());
                case TransactionWrite write -> write(write.getPath(), write.getData());
                case TransactionMerge merge -> merge(merge.getPath(), merge.getData());
            }
        }

        request.getPersistenceProtocol().ifPresent(proto -> {
            final var cb = verifyNotNull(callback, "Request %s has null callback", request);
            if (markSealed()) {
                sealOnly();
            }

            switch (proto) {
                case null -> throw new NullPointerException();
                case ABORT -> sendMethod.accept(new AbortLocalTransactionRequest(getIdentifier(), localActor()), cb);
                case READY -> {
                    // No-op, as we have already issued a sealOnly() and we are not transmitting anything
                }
                case SIMPLE -> sendMethod.accept(commitRequest(false), cb);
                case THREE_PHASE -> sendMethod.accept(commitRequest(true), cb);
            }
        });
    }

    @Override
    void handleReplayedLocalRequest(final AbstractLocalTransactionRequest<?> request,
            final Consumer<Response<?, ?>> callback, final long now) {
        if (request instanceof CommitLocalTransactionRequest req) {
            enqueueRequest(rebaseCommit(req), callback, now);
        } else {
            super.handleReplayedLocalRequest(request, callback, now);
        }
    }

    @Override
    void handleReplayedRemoteRequest(final TransactionRequest<?> request, final Consumer<Response<?, ?>> callback,
            final long enqueuedTicks) {
        LOG.debug("Applying replayed request {}", request);
        switch (request) {
            case TransactionPreCommitRequest req ->
                enqueueRequest(new TransactionPreCommitRequest(getIdentifier(), nextSequence(), localActor()), callback,
                    enqueuedTicks);
            case TransactionDoCommitRequest req ->
                enqueueRequest(new TransactionDoCommitRequest(getIdentifier(), nextSequence(), localActor()), callback,
                    enqueuedTicks);
            case TransactionAbortRequest req -> enqueueDoAbort(callback, enqueuedTicks);
            default -> super.handleReplayedRemoteRequest(request, callback, enqueuedTicks);
        }
    }

    @Override
    void handleForwardedRemoteRequest(final TransactionRequest<?> request, final Consumer<Response<?, ?>> callback) {
        LOG.debug("Applying forwarded request {}", request);
        switch (request) {
            case TransactionPreCommitRequest req ->
                sendRequest(new TransactionPreCommitRequest(getIdentifier(), nextSequence(), localActor()), callback);
            case TransactionDoCommitRequest req ->
                sendRequest(new TransactionDoCommitRequest(getIdentifier(), nextSequence(), localActor()), callback);
            case TransactionAbortRequest req -> sendDoAbort(callback);
            default -> super.handleForwardedRemoteRequest(request, callback);
        }
    }

    @Override
    Response<?, ?> handleExistsRequest(final DataTreeSnapshot snapshot, final ExistsTransactionRequest request) {
        final var ex = recordedFailure;
        return ex == null ? super.handleExistsRequest(snapshot, request)
            : request.toRequestFailure(
                new RuntimeRequestException("Previous modification failed", ReadFailedException.MAPPER.apply(ex)));
    }

    @Override
    Response<?, ?> handleReadRequest(final DataTreeSnapshot snapshot, final ReadTransactionRequest request) {
        final var ex = recordedFailure;
        return ex == null ? super.handleReadRequest(snapshot, request)
            : request.toRequestFailure(
                new RuntimeRequestException("Previous modification failed", ReadFailedException.MAPPER.apply(ex)));
    }

    @Override
    void forwardToLocal(final LocalProxyTransaction successor, final TransactionRequest<?> request,
            final Consumer<Response<?, ?>> callback) {
        switch (request) {
            case CommitLocalTransactionRequest req -> verifyLocalReadWrite(successor).sendRebased(req, callback);
            case ModifyTransactionRequest req ->
                verifyLocalReadWrite(successor).handleForwardedRemoteRequest(req, callback);
            default -> {
                super.forwardToLocal(successor, request, callback);
                return;
            }
        }
        LOG.debug("Forwarded request {} to successor {}", request, successor);
    }

    private static LocalReadWriteProxyTransaction verifyLocalReadWrite(final LocalProxyTransaction successor) {
        if (successor instanceof LocalReadWriteProxyTransaction local) {
            return local;
        }
        throw new VerifyException("Unexpected successor " + successor);
    }

    @Override
    void sendAbort(final AbortLocalTransactionRequest request, final Consumer<Response<?, ?>> callback) {
        super.sendAbort(request, callback);
        closedException = this::abortedException;
    }

    @Override
    void enqueueAbort(final AbortLocalTransactionRequest request, final Consumer<Response<?, ?>> callback,
            final long enqueuedTicks) {
        super.enqueueAbort(request, callback, enqueuedTicks);
        closedException = this::abortedException;
    }

    @SuppressFBWarnings(value = "THROWS_METHOD_THROWS_RUNTIMEEXCEPTION", justification = "Replay of recorded failure")
    private @NonNull CursorAwareDataTreeModification getModification() {
        if (closedException != null) {
            throw closedException.get();
        }
        return verifyNotNull(modification, "Transaction %s is DONE", getIdentifier());
    }

    private void sendRebased(final CommitLocalTransactionRequest request, final Consumer<Response<?, ?>> callback) {
        sendRequest(rebaseCommit(request), callback);
    }

    private CommitLocalTransactionRequest rebaseCommit(final CommitLocalTransactionRequest request) {
        // Rebase old modification on new data tree.
        final var mod = getModification();

        if (!(mod instanceof FailedDataTreeModification)) {
            final var failure = request.delayedFailure();
            if (failure != null) {
                if (recordedFailure == null) {
                    recordedFailure = failure;
                } else {
                    recordedFailure.addSuppressed(failure);
                }
            } else {
                try (var cursor = mod.openCursor()) {
                    request.getModification().applyToCursor(cursor);
                }
            }
        }

        if (markSealed()) {
            sealOnly();
        }

        return commitRequest(request.isCoordinated());
    }
}
