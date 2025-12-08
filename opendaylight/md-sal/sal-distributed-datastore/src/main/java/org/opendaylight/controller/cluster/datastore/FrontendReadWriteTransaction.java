/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.FutureCallback;
import java.util.Collection;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.access.commands.AbortLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ClosedTransactionException;
import org.opendaylight.controller.cluster.access.commands.CommitLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ExistsTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ExistsTransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.ReadTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ReadTransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionAbortRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionAbortSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionCanCommitSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionCommitSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionDelete;
import org.opendaylight.controller.cluster.access.commands.TransactionDoCommitRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionMerge;
import org.opendaylight.controller.cluster.access.commands.TransactionModification;
import org.opendaylight.controller.cluster.access.commands.TransactionPreCommitRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionPreCommitSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionWrite;
import org.opendaylight.controller.cluster.access.concepts.RequestEnvelope;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.RuntimeRequestException;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.access.concepts.UnsupportedRequestException;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeModification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Frontend read-write transaction state as observed by the shard leader. This class is NOT thread-safe.
 */
final class FrontendReadWriteTransaction extends FrontendTransaction {
    private enum CommitStage {
        READY,
        CAN_COMMIT_PENDING,
        CAN_COMMIT_COMPLETE,
        PRE_COMMIT_PENDING,
        PRE_COMMIT_COMPLETE,
        COMMIT_PENDING,
    }

    private abstract static sealed class State {
        @Override
        public abstract String toString();
    }

    private static final class Aborted extends State {
        private static final Aborted INSTANCE = new Aborted();

        @Override
        public String toString() {
            return "ABORTED";
        }
    }

    private static final class Aborting extends State {
        private static final Aborting INSTANCE = new Aborting();

        @Override
        public String toString() {
            return "ABORTING";
        }
    }

    private static final class Committed extends State {
        private static final Committed INSTANCE = new Committed();

        @Override
        public String toString() {
            return "COMMITTED";
        }
    }

    private static final class Failed extends State {
        final @NonNull RequestException cause;

        Failed(final RequestException cause) {
            this.cause = requireNonNull(cause);
        }

        @Override
        public String toString() {
            return "FAILED (" + cause.getMessage() + ")";
        }
    }

    private static final class Open extends State {
        final @NonNull ReadWriteShardDataTreeTransaction openTransaction;

        Open(final ReadWriteShardDataTreeTransaction openTransaction) {
            this.openTransaction = requireNonNull(openTransaction);
        }

        @Override
        public String toString() {
            return "OPEN";
        }
    }

    private static final class Ready extends State {
        final @NonNull CommitCohort readyCohort;

        @NonNull CommitStage stage = CommitStage.READY;

        Ready(final CommitCohort readyCohort) {
            this.readyCohort = requireNonNull(readyCohort);
        }

        @Override
        public String toString() {
            return "READY (" + stage + ")";
        }
    }

    private static final class Sealed extends State {
        final @NonNull DataTreeModification sealedModification;

        Sealed(final DataTreeModification sealedModification) {
            this.sealedModification = requireNonNull(sealedModification);
        }

        @Override
        public String toString() {
            return "SEALED";
        }
    }

    /**
     * Retired state, needed to catch and suppress callbacks after we have removed associated state.
     */
    private static final class Retired extends State {
        private final String prevStateString;

        Retired(final State prevState) {
            prevStateString = prevState.toString();
        }

        @Override
        public String toString() {
            return "RETIRED (in " + prevStateString + ")";
        }
    }

    /**
     * Abstract base class for callbacks towards {@link CommitCohort}.
     */
    private abstract static sealed class AbstractCallback<T> implements FutureCallback<T>
            permits AbstractCanCommitCallback, AbstractPreCommitCallback, DoCommitCallback {
        private final @NonNull FrontendReadWriteTransaction transaction;
        private final @NonNull RequestEnvelope envelope;
        private final long startTime;

        @NonNullByDefault
        AbstractCallback(final FrontendReadWriteTransaction transaction, final RequestEnvelope envelope,
                final long startTime) {
            this.transaction = requireNonNull(transaction);
            this.envelope = requireNonNull(envelope);
            this.startTime = startTime;
        }

        @Override
        public final void onSuccess(final T result) {
            onSuccess(transaction, envelope, startTime);
        }

        @NonNullByDefault
        abstract void onSuccess(FrontendReadWriteTransaction transaction, RequestEnvelope envelope, long startTime);

        @Override
        public final void onFailure(final Throwable failure) {
            transaction.failTransaction(envelope, startTime, new RuntimeRequestException(failureMessage(), failure));
        }

        abstract @NonNull String failureMessage();

        @Override
        public final String toString() {
            return MoreObjects.toStringHelper(this).add("transaction", transaction).toString();
        }
    }

    /**
     * Abstract base class for a callback for {@link CommitCohort#canCommit(FutureCallback)}.
     */
    private abstract static sealed class AbstractCanCommitCallback extends AbstractCallback<Empty>
            permits CoordinatedCanCommitCallback, DirectCanCommitCallback {
        @NonNullByDefault
        AbstractCanCommitCallback(final FrontendReadWriteTransaction transaction, final RequestEnvelope envelope,
                final long startTime) {
            super(transaction, envelope, startTime);
        }

        @Override
        final String failureMessage() {
            return "CanCommit failed";
        }
    }

    /**
     * Callback for {@link CommitCohort#canCommit(FutureCallback)} invoked during coordinated commit.
     */
    private static final class CoordinatedCanCommitCallback extends AbstractCanCommitCallback {
        @NonNullByDefault
        CoordinatedCanCommitCallback(final FrontendReadWriteTransaction transaction, final RequestEnvelope envelope,
                final long startTime) {
            super(transaction, envelope, startTime);
        }

        @Override
        void onSuccess(final FrontendReadWriteTransaction transaction, final RequestEnvelope envelope,
                final long startTime) {
            transaction.successfulCanCommit(envelope, startTime);
        }
    }

    /**
     * Callback for {@link CommitCohort#canCommit(FutureCallback)} invoked during direct commit.
     */
    private static final class DirectCanCommitCallback extends AbstractCanCommitCallback {
        @NonNullByDefault
        DirectCanCommitCallback(final FrontendReadWriteTransaction transaction, final RequestEnvelope envelope,
                final long startTime) {
            super(transaction, envelope, startTime);
        }

        @Override
        void onSuccess(final FrontendReadWriteTransaction transaction, final RequestEnvelope envelope,
                final long startTime) {
            transaction.successfulDirectCanCommit(envelope, startTime);
        }
    }

    /**
     * Abstract base class for a callback for {@link CommitCohort#preCommit(FutureCallback)}.
     */
    private abstract static sealed class AbstractPreCommitCallback extends AbstractCallback<DataTreeCandidate>
            permits CoordinatedPreCommitCallback, DirectPreCommitCallback {
        @NonNullByDefault
        AbstractPreCommitCallback(final FrontendReadWriteTransaction transaction, final RequestEnvelope envelope,
                final long startTime) {
            super(transaction, envelope, startTime);
        }

        @Override
        final String failureMessage() {
            return "PreCommit failed";
        }
    }

    /**
     * Callback for {@link CommitCohort#preCommit(FutureCallback)} invoked during coordinated commit.
     */
    private static final class CoordinatedPreCommitCallback extends AbstractPreCommitCallback {
        @NonNullByDefault
        CoordinatedPreCommitCallback(final FrontendReadWriteTransaction transaction, final RequestEnvelope envelope,
                final long startTime) {
            super(transaction, envelope, startTime);
        }

        @Override
        void onSuccess(final FrontendReadWriteTransaction transaction, final RequestEnvelope envelope,
                final long startTime) {
            transaction.successfulPreCommit(envelope, startTime);
        }
    }

    /**
     * Callback for {@link CommitCohort#preCommit(FutureCallback)} invoked during direct commit.
     */
    private static final class DirectPreCommitCallback extends AbstractPreCommitCallback {
        @NonNullByDefault
        DirectPreCommitCallback(final FrontendReadWriteTransaction transaction, final RequestEnvelope envelope,
                final long startTime) {
            super(transaction, envelope, startTime);
        }

        @Override
        void onSuccess(final FrontendReadWriteTransaction transaction, final RequestEnvelope envelope,
                final long startTime) {
            transaction.successfulDirectPreCommit(envelope, startTime);
        }
    }

    /**
     * A callback for {@link CommitCohort#commit(FutureCallback)}, initiated either in coordinated or direct fashion.
     */
    private static final class DoCommitCallback extends AbstractCallback<UnsignedLong> {
        @NonNullByDefault
        DoCommitCallback(final FrontendReadWriteTransaction transaction, final RequestEnvelope envelope,
                final long startTime) {
            super(transaction, envelope, startTime);
        }

        @Override
        void onSuccess(final FrontendReadWriteTransaction transaction, final RequestEnvelope envelope,
                final long startTime) {
            transaction.successfulCommit(envelope, startTime);
        }

        @Override
        String failureMessage() {
            return "DoCommit failed";
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(FrontendReadWriteTransaction.class);

    private State state;

    private FrontendReadWriteTransaction(final AbstractFrontendHistory history, final TransactionIdentifier id,
            final ReadWriteShardDataTreeTransaction transaction) {
        super(history, id);
        state = new Open(transaction);
    }

    private FrontendReadWriteTransaction(final AbstractFrontendHistory history, final TransactionIdentifier id,
            final DataTreeModification mod) {
        super(history, id);
        state = new Sealed(mod);
    }

    static @NonNull FrontendReadWriteTransaction createOpen(final AbstractFrontendHistory history,
            final ReadWriteShardDataTreeTransaction transaction) {
        return new FrontendReadWriteTransaction(history, transaction.getIdentifier(), transaction);
    }

    static @NonNull FrontendReadWriteTransaction createReady(final AbstractFrontendHistory history,
            final TransactionIdentifier id, final DataTreeModification mod) {
        return new FrontendReadWriteTransaction(history, id, mod);
    }

    // Sequence has already been checked
    @Override
    TransactionSuccess<?> doHandleRequest(final TransactionRequest<?> request, final RequestEnvelope envelope,
            final long now) throws RequestException {
        return switch (request) {
            case ModifyTransactionRequest req -> handleModifyTransaction(req, envelope, now);
            case CommitLocalTransactionRequest req -> handleCommitLocalTransaction(req, envelope, now);
            case ExistsTransactionRequest req -> handleExistsTransaction(req);
            case ReadTransactionRequest req -> handleReadTransaction(req);
            case TransactionPreCommitRequest req -> handleTransactionPreCommit(req, envelope, now);
            case TransactionDoCommitRequest req -> handleTransactionDoCommit(req, envelope, now);
            case TransactionAbortRequest req -> handleTransactionAbort(req.getSequence(), envelope, now);
            case AbortLocalTransactionRequest req -> handleLocalTransactionAbort(req.getSequence(), envelope, now);
            default -> {
                LOG.warn("Rejecting unsupported request {}", request);
                throw new UnsupportedRequestException(request);
            }
        };
    }

    @Override
    void retire() {
        state = new Retired(state);
    }

    private TransactionSuccess<?> handleTransactionPreCommit(final TransactionPreCommitRequest request,
            final RequestEnvelope envelope, final long now) throws RequestException {
        switch (state) {
            case Failed failed -> throw reportFailed(failed);
            case Ready ready -> {
                switch (ready.stage) {
                    case PRE_COMMIT_PENDING ->
                        LOG.debug("{}: Transaction {} is already preCommitting", persistenceId(), getIdentifier());
                    case CAN_COMMIT_COMPLETE -> {
                        ready.stage = CommitStage.PRE_COMMIT_PENDING;
                        LOG.debug("{}: Transaction {} initiating preCommit", persistenceId(), getIdentifier());
                        ready.readyCohort.preCommit(new CoordinatedPreCommitCallback(this, envelope, now));
                    }
                    default -> throw new IllegalStateException("Attempted to preCommit in stage " + ready.stage);
                }
            }
            default -> throw new IllegalStateException(getIdentifier() + " cannot preCommit in state " + state);
        }
        return null;
    }

    private void successfulPreCommit(final RequestEnvelope envelope, final long startTime) {
        switch (state) {
            case Retired retired ->
                LOG.debug("{}: Suppressing successful preCommit of retired transaction {}", persistenceId(),
                    getIdentifier());
            case Ready ready -> {
                LOG.debug("{}: Transaction {} completed preCommit", persistenceId(), getIdentifier());
                recordAndSendSuccess(envelope, startTime, new TransactionPreCommitSuccess(getIdentifier(),
                    envelope.getMessage().getSequence()));
                ready.stage = CommitStage.PRE_COMMIT_COMPLETE;
            }
            default -> throw new IllegalStateException(
                getIdentifier() + " cannot complete preCommit in state " + state);
        }
    }

    private void failTransaction(final RequestEnvelope envelope, final long now, final RuntimeRequestException cause) {
        if (state instanceof Retired) {
            LOG.debug("{}: Suppressing failure of retired transaction {}", persistenceId(), getIdentifier(), cause);
            return;
        }

        recordAndSendFailure(envelope, now, cause);
        state = new Failed(cause);
        LOG.debug("{}: Transaction {} failed", persistenceId(), getIdentifier(), cause);
    }

    private TransactionSuccess<?> handleTransactionDoCommit(final TransactionDoCommitRequest request,
            final RequestEnvelope envelope, final long now) throws RequestException {
        switch (state) {
            case Aborted aborted -> {
                LOG.debug("{}: Transaction {} has already aborted", persistenceId(), getIdentifier());
                throw new ClosedTransactionException(false);
            }
            case Aborting aborting -> {
                LOG.debug("{}: Transaction {} is already aborting", persistenceId(), getIdentifier());
                throw new ClosedTransactionException(false);
            }
            case Committed committed -> {
                LOG.debug("{}: Transaction {} has already committed", persistenceId(), getIdentifier());
                throw new ClosedTransactionException(true);
            }
            case Failed failed -> throw reportFailed(failed);
            case Ready ready -> {
                switch (ready.stage) {
                    case COMMIT_PENDING ->
                        LOG.debug("{}: Transaction {} is already committing", persistenceId(), getIdentifier());
                    case PRE_COMMIT_COMPLETE -> {
                        ready.stage = CommitStage.COMMIT_PENDING;
                        LOG.debug("{}: Transaction {} initiating commit", persistenceId(), getIdentifier());
                        ready.readyCohort.commit(new DoCommitCallback(this, envelope, now));
                    }
                    default -> throw new IllegalStateException("Attempted to doCommit in stage " + ready.stage);
                }
            }
            default -> throw new IllegalStateException(getIdentifier() + " cannot perform doCommit in state " + state);
        }
        return null;
    }

    private TransactionSuccess<?> handleLocalTransactionAbort(final long sequence, final RequestEnvelope envelope,
            final long now) {
        checkOpen().abort(
            () -> recordAndSendSuccess(envelope, now, new TransactionAbortSuccess(getIdentifier(), sequence)));
        return null;
    }

    private void startAbort() {
        state = Aborting.INSTANCE;
        LOG.debug("{}: Transaction {} aborting", persistenceId(), getIdentifier());
    }

    private void finishAbort() {
        state = Aborted.INSTANCE;
        LOG.debug("{}: Transaction {} aborted", persistenceId(), getIdentifier());
    }

    private TransactionAbortSuccess handleTransactionAbort(final long sequence, final RequestEnvelope envelope,
            final long now) {
        return switch (state) {
            case Aborted aborted -> {
                // We should have recorded the reply
                LOG.warn("{}: Transaction {} already aborted", persistenceId(), getIdentifier());
                yield new TransactionAbortSuccess(getIdentifier(), sequence);
            }
            case Aborting aborting -> {
                LOG.debug("{}: Transaction {} already aborting", persistenceId(), getIdentifier());
                yield null;
            }
            case Open open -> {
                final var openTransaction = open.openTransaction;
                startAbort();
                openTransaction.abort(() -> {
                    recordAndSendSuccess(envelope, now, new TransactionAbortSuccess(getIdentifier(), sequence));
                    finishAbort();
                });
                yield null;
            }
            case Ready ready -> {
                startAbort();
                ready.readyCohort.abort(new FutureCallback<>() {
                    @Override
                    public void onSuccess(final Empty result) {
                        recordAndSendSuccess(envelope, now, new TransactionAbortSuccess(getIdentifier(), sequence));
                        finishAbort();
                    }

                    @Override
                    public void onFailure(final Throwable failure) {
                        recordAndSendFailure(envelope, now, new RuntimeRequestException("Abort failed", failure));
                        LOG.warn("{}: Transaction {} abort failed", persistenceId(), getIdentifier(), failure);
                        finishAbort();
                    }
                });
                yield null;
            }
            default -> throw new IllegalStateException(getIdentifier() + " cannot be aborted in state " + state);
        };
    }

    private void coordinatedCommit(final RequestEnvelope envelope, final long now, final Ready ready)
            throws RequestException {
        switch (ready.stage) {
            case CAN_COMMIT_PENDING ->
                LOG.debug("{}: Transaction {} is already canCommitting", persistenceId(), getIdentifier());
            case READY -> {
                ready.stage = CommitStage.CAN_COMMIT_PENDING;
                LOG.debug("{}: Transaction {} initiating canCommit", persistenceId(), getIdentifier());
                ready.readyCohort.canCommit(new CoordinatedCanCommitCallback(this, envelope, now));
            }
            default -> throw new IllegalStateException("Attempted to canCommit in stage " + ready.stage);
        }
    }

    private void successfulCanCommit(final RequestEnvelope envelope, final long startTime) {
        switch (state) {
            case Ready ready -> {
                recordAndSendSuccess(envelope, startTime, new TransactionCanCommitSuccess(getIdentifier(),
                    envelope.getMessage().getSequence()));
                ready.stage = CommitStage.CAN_COMMIT_COMPLETE;
                LOG.debug("{}: Transaction {} completed canCommit", persistenceId(), getIdentifier());
            }
            case Retired retired -> {
                LOG.debug("{}: Suppressing successful canCommit of retired transaction {}", persistenceId(),
                    getIdentifier());
            }
            default -> throw new IllegalStateException(
                getIdentifier() + " cannot complete canCommit in state " + state);
        }
    }

    private void directCommit(final RequestEnvelope envelope, final long now, final Ready ready)
            throws RequestException {
        switch (ready.stage) {
            case READY -> {
                ready.stage = CommitStage.CAN_COMMIT_PENDING;
                LOG.debug("{}: Transaction {} initiating direct canCommit", persistenceId(), getIdentifier());
                ready.readyCohort.canCommit(new DirectCanCommitCallback(this, envelope, now));
            }
            default ->
                LOG.debug("{}: Transaction {} in state {}, not initiating direct commit for {}", persistenceId(),
                    getIdentifier(), state, envelope);
        }
    }

    private void successfulDirectCanCommit(final RequestEnvelope envelope, final long startTime) {
        switch (state) {
            case Retired retired ->
                LOG.debug("{}: Suppressing direct canCommit of retired transaction {}", persistenceId(),
                    getIdentifier());
            case Ready ready -> {
                ready.stage = CommitStage.PRE_COMMIT_PENDING;
                LOG.debug("{}: Transaction {} initiating direct preCommit", persistenceId(), getIdentifier());
                ready.readyCohort.preCommit(new DirectPreCommitCallback(this, envelope, startTime));
            }
            default -> throw new IllegalStateException(
                getIdentifier() + " cannot complete canCommit in state " + state);
        }
    }

    @NonNullByDefault
    private void successfulDirectPreCommit(final RequestEnvelope envelope, final long startTime) {
        switch (state) {
            case Retired retired ->
                LOG.debug("{}: Suppressing direct commit of retired transaction {}", persistenceId(), getIdentifier());
            case Ready ready -> {
                ready.stage = CommitStage.COMMIT_PENDING;
                LOG.debug("{}: Transaction {} initiating direct commit", persistenceId(), getIdentifier());
                ready.readyCohort.commit(new DoCommitCallback(this, envelope, startTime));
            }
            default -> throw new IllegalStateException(
                getIdentifier() + " cannot complete preCommit in state " + state);
        }
    }

    @NonNullByDefault
    private void successfulCommit(final RequestEnvelope envelope, final long startTime) {
        switch (state) {
            case Retired retired -> {
                LOG.debug("{}: Suppressing commit response on retired transaction {}", persistenceId(),
                    getIdentifier());
            }
            default -> {
                recordAndSendSuccess(envelope, startTime, new TransactionCommitSuccess(getIdentifier(),
                    envelope.getMessage().getSequence()));
                state = Committed.INSTANCE;
            }
        }
    }

    private @Nullable TransactionSuccess<?> handleCommitLocalTransaction(
            final @NonNull CommitLocalTransactionRequest request, final @NonNull RequestEnvelope envelope,
            final long now) throws RequestException {
        if (!(state instanceof Sealed sealed)) {
            throw new IllegalStateException(getIdentifier() + " expect to be sealed, is in state " + state);
        }

        final var sealedModification = sealed.sealedModification;
        if (!sealedModification.equals(request.getModification())) {
            LOG.warn("Expecting modification {}, commit request has {}", sealedModification, request.getModification());
            throw new UnsupportedRequestException(request);
        }

        final var optFailure = request.delayedFailure();
        final var ready = new Ready(optFailure == null
            ? history().createReadyCohort(getIdentifier(), sealedModification)
            : history().createFailedCohort(getIdentifier(), sealedModification, optFailure));
        state = ready;
        if (request.isCoordinated()) {
            coordinatedCommit(envelope, now, ready);
        } else {
            directCommit(envelope, now, ready);
        }
        return null;
    }

    @NonNullByDefault
    private ExistsTransactionSuccess handleExistsTransaction(final ExistsTransactionRequest request) {
        final var data = checkOpen().getSnapshot().readNode(request.getPath());
        return recordSuccess(request.getSequence(), new ExistsTransactionSuccess(getIdentifier(), request.getSequence(),
            data.isPresent()));
    }

    @NonNullByDefault
    private ReadTransactionSuccess handleReadTransaction(final ReadTransactionRequest request) {
        final var data = checkOpen().getSnapshot().readNode(request.getPath());
        return recordSuccess(request.getSequence(), new ReadTransactionSuccess(getIdentifier(), request.getSequence(),
            data));
    }

    @NonNullByDefault
    private ModifyTransactionSuccess replyModifySuccess(final long sequence) {
        return recordSuccess(sequence, new ModifyTransactionSuccess(getIdentifier(), sequence));
    }

    private void applyModifications(final Collection<TransactionModification> modifications) {
        if (!modifications.isEmpty()) {
            applyModifications(checkOpen().getSnapshot(), modifications);
        }
    }

    private static void applyModifications(final DataTreeModification modification,
            final Collection<TransactionModification> mods) {
        for (var mod : mods) {
            switch (mod) {
                case TransactionDelete delete -> modification.delete(delete.getPath());
                case TransactionMerge merge -> modification.merge(merge.getPath(), merge.getData());
                case TransactionWrite write -> modification.write(write.getPath(), write.getData());
            }
        }
    }

    private @Nullable TransactionSuccess<?> handleModifyTransaction(final ModifyTransactionRequest request,
            final RequestEnvelope envelope, final long now) throws RequestException {
        // We need to examine the persistence protocol first to see if this is an idempotent request. If there is no
        // protocol, there is nothing for us to do.
        final var maybeProto = request.getPersistenceProtocol();
        if (maybeProto.isEmpty()) {
            applyModifications(request.getModifications());
            return replyModifySuccess(request.getSequence());
        }

        return switch (maybeProto.orElseThrow()) {
            case ABORT -> {
                if (state instanceof Aborting) {
                    LOG.debug("{}: Transaction {} already aborting", persistenceId(), getIdentifier());
                    yield null;
                }
                final var openTransaction = checkOpen();
                startAbort();
                openTransaction.abort(() -> {
                    recordAndSendSuccess(envelope, now, new ModifyTransactionSuccess(getIdentifier(),
                        request.getSequence()));
                    finishAbort();
                });
                yield null;
            }
            case READY -> {
                ensureReady(request.getModifications());
                yield replyModifySuccess(request.getSequence());
            }
            case SIMPLE -> {
                directCommit(envelope, now, ensureReady(request.getModifications()));
                yield null;
            }
            case THREE_PHASE -> {
                coordinatedCommit(envelope, now, ensureReady(request.getModifications()));
                yield null;
            }
        };
    }

    private @NonNull Ready ensureReady(final Collection<TransactionModification> modifications) {
        // We may have a combination of READY + SIMPLE/THREE_PHASE , in which case we want to ready the transaction
        // only once.
        return switch (state) {
            case Open open -> {
                final var transaction = open.openTransaction;
                applyModifications(transaction.getSnapshot(), modifications);
                final var ready = new Ready(transaction.ready());
                state = ready;
                LOG.debug("{}: transitioned {} to ready", persistenceId(), getIdentifier());
                yield ready;
            }
            case Ready ready -> {
                LOG.debug("{}: {} is already in state {}", persistenceId(), getIdentifier(), ready);
                yield ready;
            }
            default -> throw new IllegalStateException(getIdentifier() + " cannot become ready in state " + state);
        };
    }

    private @NonNull RequestException reportFailed(final Failed failed) {
        LOG.debug("{}: {} has failed, rejecting request", persistenceId(), getIdentifier());
        return failed.cause;
    }

    private ReadWriteShardDataTreeTransaction checkOpen() {
        if (state instanceof Open open) {
            return open.openTransaction;
        }
        throw new IllegalStateException(getIdentifier() + " expect to be open, is in state " + state);
    }
}
