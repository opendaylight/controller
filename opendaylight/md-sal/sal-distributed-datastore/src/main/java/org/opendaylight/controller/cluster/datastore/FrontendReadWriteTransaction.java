/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.FutureCallback;
import java.util.Collection;
import java.util.Optional;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.access.commands.AbortLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.CommitLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ExistsTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ExistsTransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.PersistenceProtocol;
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
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Frontend read-write transaction state as observed by the shard leader. This class is NOT thread-safe.
 *
 * @author Robert Varga
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

    private abstract static class State {
        @Override
        public abstract String toString();
    }

    private static final class Failed extends State {
        final RequestException cause;

        Failed(final RequestException cause) {
            this.cause = requireNonNull(cause);
        }

        @Override
        public String toString() {
            return "FAILED (" + cause.getMessage() + ")";
        }
    }

    private static final class Open extends State {
        final ReadWriteShardDataTreeTransaction openTransaction;

        Open(final ReadWriteShardDataTreeTransaction openTransaction) {
            this.openTransaction = requireNonNull(openTransaction);
        }

        @Override
        public String toString() {
            return "OPEN";
        }
    }

    private static final class Ready extends State {
        final ShardDataTreeCohort readyCohort;
        CommitStage stage;

        Ready(final ShardDataTreeCohort readyCohort) {
            this.readyCohort = requireNonNull(readyCohort);
            this.stage = CommitStage.READY;
        }

        @Override
        public String toString() {
            return "READY (" + stage + ")";
        }
    }

    private static final class Sealed extends State {
        final DataTreeModification sealedModification;

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

    private static final Logger LOG = LoggerFactory.getLogger(FrontendReadWriteTransaction.class);
    private static final State ABORTED = new State() {
        @Override
        public String toString() {
            return "ABORTED";
        }
    };
    private static final State ABORTING = new State() {
        @Override
        public String toString() {
            return "ABORTING";
        }
    };
    private static final State COMMITTED = new State() {
        @Override
        public String toString() {
            return "COMMITTED";
        }
    };

    private State state;

    private FrontendReadWriteTransaction(final AbstractFrontendHistory history, final TransactionIdentifier id,
            final ReadWriteShardDataTreeTransaction transaction) {
        super(history, id);
        this.state = new Open(transaction);
    }

    private FrontendReadWriteTransaction(final AbstractFrontendHistory history, final TransactionIdentifier id,
            final DataTreeModification mod) {
        super(history, id);
        this.state = new Sealed(mod);
    }

    static FrontendReadWriteTransaction createOpen(final AbstractFrontendHistory history,
            final ReadWriteShardDataTreeTransaction transaction) {
        return new FrontendReadWriteTransaction(history, transaction.getIdentifier(), transaction);
    }

    static FrontendReadWriteTransaction createReady(final AbstractFrontendHistory history,
            final TransactionIdentifier id, final DataTreeModification mod) {
        return new FrontendReadWriteTransaction(history, id, mod);
    }

    // Sequence has already been checked
    @Override
    TransactionSuccess<?> doHandleRequest(final TransactionRequest<?> request, final RequestEnvelope envelope,
            final long now) throws RequestException {
        if (request instanceof ModifyTransactionRequest) {
            return handleModifyTransaction((ModifyTransactionRequest) request, envelope, now);
        } else if (request instanceof CommitLocalTransactionRequest) {
            handleCommitLocalTransaction((CommitLocalTransactionRequest) request, envelope, now);
            return null;
        } else if (request instanceof ExistsTransactionRequest) {
            return handleExistsTransaction((ExistsTransactionRequest) request);
        } else if (request instanceof ReadTransactionRequest) {
            return handleReadTransaction((ReadTransactionRequest) request);
        } else if (request instanceof TransactionPreCommitRequest) {
            handleTransactionPreCommit((TransactionPreCommitRequest) request, envelope, now);
            return null;
        } else if (request instanceof TransactionDoCommitRequest) {
            handleTransactionDoCommit((TransactionDoCommitRequest) request, envelope, now);
            return null;
        } else if (request instanceof TransactionAbortRequest) {
            return handleTransactionAbort(request.getSequence(), envelope, now);
        } else if (request instanceof AbortLocalTransactionRequest) {
            handleLocalTransactionAbort(request.getSequence(), envelope, now);
            return null;
        } else {
            LOG.warn("Rejecting unsupported request {}", request);
            throw new UnsupportedRequestException(request);
        }
    }

    @Override
    void retire() {
        state = new Retired(state);
    }

    private void handleTransactionPreCommit(final TransactionPreCommitRequest request,
            final RequestEnvelope envelope, final long now) throws RequestException {
        throwIfFailed();

        final Ready ready = checkReady();
        switch (ready.stage) {
            case PRE_COMMIT_PENDING:
                LOG.debug("{}: Transaction {} is already preCommitting", persistenceId(), getIdentifier());
                break;
            case CAN_COMMIT_COMPLETE:
                ready.stage = CommitStage.PRE_COMMIT_PENDING;
                LOG.debug("{}: Transaction {} initiating preCommit", persistenceId(), getIdentifier());
                ready.readyCohort.preCommit(new FutureCallback<DataTreeCandidate>() {
                    @Override
                    public void onSuccess(final DataTreeCandidate result) {
                        successfulPreCommit(envelope, now);
                    }

                    @Override
                    public void onFailure(final Throwable failure) {
                        failTransaction(envelope, now, new RuntimeRequestException("Precommit failed", failure));
                    }
                });
                break;
            case CAN_COMMIT_PENDING:
            case COMMIT_PENDING:
            case PRE_COMMIT_COMPLETE:
            case READY:
                throw new IllegalStateException("Attempted to preCommit in stage " + ready.stage);
            default:
                throwUnhandledCommitStage(ready);
        }
    }

    void successfulPreCommit(final RequestEnvelope envelope, final long startTime) {
        if (state instanceof Retired) {
            LOG.debug("{}: Suppressing successful preCommit of retired transaction {}", persistenceId(),
                getIdentifier());
            return;
        }

        final Ready ready = checkReady();
        LOG.debug("{}: Transaction {} completed preCommit", persistenceId(), getIdentifier());
        recordAndSendSuccess(envelope, startTime, new TransactionPreCommitSuccess(getIdentifier(),
            envelope.getMessage().getSequence()));
        ready.stage = CommitStage.PRE_COMMIT_COMPLETE;
    }

    void failTransaction(final RequestEnvelope envelope, final long now, final RuntimeRequestException cause) {
        if (state instanceof Retired) {
            LOG.debug("{}: Suppressing failure of retired transaction {}", persistenceId(), getIdentifier(), cause);
            return;
        }

        recordAndSendFailure(envelope, now, cause);
        state = new Failed(cause);
        LOG.debug("{}: Transaction {} failed", persistenceId(), getIdentifier(), cause);
    }

    private void handleTransactionDoCommit(final TransactionDoCommitRequest request, final RequestEnvelope envelope,
            final long now) throws RequestException {
        throwIfFailed();

        final Ready ready = checkReady();
        switch (ready.stage) {
            case COMMIT_PENDING:
                LOG.debug("{}: Transaction {} is already committing", persistenceId(), getIdentifier());
                break;
            case PRE_COMMIT_COMPLETE:
                ready.stage = CommitStage.COMMIT_PENDING;
                LOG.debug("{}: Transaction {} initiating commit", persistenceId(), getIdentifier());
                ready.readyCohort.commit(new FutureCallback<UnsignedLong>() {
                    @Override
                    public void onSuccess(final UnsignedLong result) {
                        successfulCommit(envelope, now);
                    }

                    @Override
                    public void onFailure(final Throwable failure) {
                        failTransaction(envelope, now, new RuntimeRequestException("Commit failed", failure));
                    }
                });
                break;
            case CAN_COMMIT_COMPLETE:
            case CAN_COMMIT_PENDING:
            case PRE_COMMIT_PENDING:
            case READY:
                throw new IllegalStateException("Attempted to doCommit in stage " + ready.stage);
            default:
                throwUnhandledCommitStage(ready);
        }
    }

    private void handleLocalTransactionAbort(final long sequence, final RequestEnvelope envelope, final long now) {
        checkOpen().abort(() -> recordAndSendSuccess(envelope, now, new TransactionAbortSuccess(getIdentifier(),
            sequence)));
    }

    private void startAbort() {
        state = ABORTING;
        LOG.debug("{}: Transaction {} aborting", persistenceId(), getIdentifier());
    }

    private void finishAbort() {
        state = ABORTED;
        LOG.debug("{}: Transaction {} aborted", persistenceId(), getIdentifier());
    }

    private TransactionAbortSuccess handleTransactionAbort(final long sequence, final RequestEnvelope envelope,
            final long now) {
        if (state instanceof Open) {
            final ReadWriteShardDataTreeTransaction openTransaction = checkOpen();
            startAbort();
            openTransaction.abort(() -> {
                recordAndSendSuccess(envelope, now, new TransactionAbortSuccess(getIdentifier(),
                    sequence));
                finishAbort();
            });
            return null;
        }
        if (ABORTING.equals(state)) {
            LOG.debug("{}: Transaction {} already aborting", persistenceId(), getIdentifier());
            return null;
        }
        if (ABORTED.equals(state)) {
            // We should have recorded the reply
            LOG.warn("{}: Transaction {} already aborted", persistenceId(), getIdentifier());
            return new TransactionAbortSuccess(getIdentifier(), sequence);
        }

        final Ready ready = checkReady();
        startAbort();
        ready.readyCohort.abort(new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
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
        return null;
    }

    private void coordinatedCommit(final RequestEnvelope envelope, final long now) throws RequestException {
        throwIfFailed();

        final Ready ready = checkReady();
        switch (ready.stage) {
            case CAN_COMMIT_PENDING:
                LOG.debug("{}: Transaction {} is already canCommitting", persistenceId(), getIdentifier());
                break;
            case READY:
                ready.stage = CommitStage.CAN_COMMIT_PENDING;
                LOG.debug("{}: Transaction {} initiating canCommit", persistenceId(), getIdentifier());
                checkReady().readyCohort.canCommit(new FutureCallback<Void>() {
                    @Override
                    public void onSuccess(final Void result) {
                        successfulCanCommit(envelope, now);
                    }

                    @Override
                    public void onFailure(final Throwable failure) {
                        failTransaction(envelope, now, new RuntimeRequestException("CanCommit failed", failure));
                    }
                });
                break;
            case CAN_COMMIT_COMPLETE:
            case COMMIT_PENDING:
            case PRE_COMMIT_COMPLETE:
            case PRE_COMMIT_PENDING:
                throw new IllegalStateException("Attempted to canCommit in stage " + ready.stage);
            default:
                throwUnhandledCommitStage(ready);
        }
    }

    void successfulCanCommit(final RequestEnvelope envelope, final long startTime) {
        if (state instanceof Retired) {
            LOG.debug("{}: Suppressing successful canCommit of retired transaction {}", persistenceId(),
                getIdentifier());
            return;
        }

        final Ready ready = checkReady();
        recordAndSendSuccess(envelope, startTime, new TransactionCanCommitSuccess(getIdentifier(),
            envelope.getMessage().getSequence()));
        ready.stage = CommitStage.CAN_COMMIT_COMPLETE;
        LOG.debug("{}: Transaction {} completed canCommit", persistenceId(), getIdentifier());
    }

    private void directCommit(final RequestEnvelope envelope, final long now) throws RequestException {
        throwIfFailed();

        final Ready ready = checkReady();
        switch (ready.stage) {
            case CAN_COMMIT_COMPLETE:
            case CAN_COMMIT_PENDING:
            case COMMIT_PENDING:
            case PRE_COMMIT_COMPLETE:
            case PRE_COMMIT_PENDING:
                LOG.debug("{}: Transaction {} in state {}, not initiating direct commit for {}", persistenceId(),
                    getIdentifier(), state, envelope);
                break;
            case READY:
                ready.stage = CommitStage.CAN_COMMIT_PENDING;
                LOG.debug("{}: Transaction {} initiating direct canCommit", persistenceId(), getIdentifier());
                ready.readyCohort.canCommit(new FutureCallback<Void>() {
                    @Override
                    public void onSuccess(final Void result) {
                        successfulDirectCanCommit(envelope, now);
                    }

                    @Override
                    public void onFailure(final Throwable failure) {
                        failTransaction(envelope, now, new RuntimeRequestException("CanCommit failed", failure));
                    }
                });
                break;
            default:
                throwUnhandledCommitStage(ready);
        }
    }

    void successfulDirectCanCommit(final RequestEnvelope envelope, final long startTime) {
        if (state instanceof Retired) {
            LOG.debug("{}: Suppressing direct canCommit of retired transaction {}", persistenceId(), getIdentifier());
            return;
        }

        final Ready ready = checkReady();
        ready.stage = CommitStage.PRE_COMMIT_PENDING;
        LOG.debug("{}: Transaction {} initiating direct preCommit", persistenceId(), getIdentifier());
        ready.readyCohort.preCommit(new FutureCallback<DataTreeCandidate>() {
            @Override
            public void onSuccess(final DataTreeCandidate result) {
                successfulDirectPreCommit(envelope, startTime);
            }

            @Override
            public void onFailure(final Throwable failure) {
                failTransaction(envelope, startTime, new RuntimeRequestException("PreCommit failed", failure));
            }
        });
    }

    void successfulDirectPreCommit(final RequestEnvelope envelope, final long startTime) {
        if (state instanceof Retired) {
            LOG.debug("{}: Suppressing direct commit of retired transaction {}", persistenceId(), getIdentifier());
            return;
        }

        final Ready ready = checkReady();
        ready.stage = CommitStage.COMMIT_PENDING;
        LOG.debug("{}: Transaction {} initiating direct commit", persistenceId(), getIdentifier());
        ready.readyCohort.commit(new FutureCallback<UnsignedLong>() {
            @Override
            public void onSuccess(final UnsignedLong result) {
                successfulCommit(envelope, startTime);
            }

            @Override
            public void onFailure(final Throwable failure) {
                failTransaction(envelope, startTime, new RuntimeRequestException("DoCommit failed", failure));
            }
        });
    }

    void successfulCommit(final RequestEnvelope envelope, final long startTime) {
        if (state instanceof Retired) {
            LOG.debug("{}: Suppressing commit response on retired transaction {}", persistenceId(), getIdentifier());
            return;
        }

        recordAndSendSuccess(envelope, startTime, new TransactionCommitSuccess(getIdentifier(),
            envelope.getMessage().getSequence()));
        state = COMMITTED;
    }

    private void handleCommitLocalTransaction(final CommitLocalTransactionRequest request,
            final RequestEnvelope envelope, final long now) throws RequestException {
        final DataTreeModification sealedModification = checkSealed();
        if (!sealedModification.equals(request.getModification())) {
            LOG.warn("Expecting modification {}, commit request has {}", sealedModification, request.getModification());
            throw new UnsupportedRequestException(request);
        }

        final Optional<Exception> optFailure = request.getDelayedFailure();
        if (optFailure.isPresent()) {
            state = new Ready(history().createFailedCohort(getIdentifier(), sealedModification, optFailure.get()));
        } else {
            state = new Ready(history().createReadyCohort(getIdentifier(), sealedModification, Optional.empty()));
        }

        if (request.isCoordinated()) {
            coordinatedCommit(envelope, now);
        } else {
            directCommit(envelope, now);
        }
    }

    private ExistsTransactionSuccess handleExistsTransaction(final ExistsTransactionRequest request) {
        final Optional<NormalizedNode> data = checkOpen().getSnapshot().readNode(request.getPath());
        return recordSuccess(request.getSequence(), new ExistsTransactionSuccess(getIdentifier(), request.getSequence(),
            data.isPresent()));
    }

    private ReadTransactionSuccess handleReadTransaction(final ReadTransactionRequest request) {
        final Optional<NormalizedNode> data = checkOpen().getSnapshot().readNode(request.getPath());
        return recordSuccess(request.getSequence(), new ReadTransactionSuccess(getIdentifier(), request.getSequence(),
            data));
    }

    private ModifyTransactionSuccess replyModifySuccess(final long sequence) {
        return recordSuccess(sequence, new ModifyTransactionSuccess(getIdentifier(), sequence));
    }

    private void applyModifications(final Collection<TransactionModification> modifications) {
        if (!modifications.isEmpty()) {
            final DataTreeModification modification = checkOpen().getSnapshot();
            for (TransactionModification m : modifications) {
                if (m instanceof TransactionDelete) {
                    modification.delete(m.getPath());
                } else if (m instanceof TransactionWrite) {
                    modification.write(m.getPath(), ((TransactionWrite) m).getData());
                } else if (m instanceof TransactionMerge) {
                    modification.merge(m.getPath(), ((TransactionMerge) m).getData());
                } else {
                    LOG.warn("{}: ignoring unhandled modification {}", persistenceId(), m);
                }
            }
        }
    }

    private @Nullable TransactionSuccess<?> handleModifyTransaction(final ModifyTransactionRequest request,
            final RequestEnvelope envelope, final long now) throws RequestException {
        // We need to examine the persistence protocol first to see if this is an idempotent request. If there is no
        // protocol, there is nothing for us to do.
        final Optional<PersistenceProtocol> maybeProto = request.getPersistenceProtocol();
        if (!maybeProto.isPresent()) {
            applyModifications(request.getModifications());
            return replyModifySuccess(request.getSequence());
        }

        switch (maybeProto.get()) {
            case ABORT:
                if (ABORTING.equals(state)) {
                    LOG.debug("{}: Transaction {} already aborting", persistenceId(), getIdentifier());
                    return null;
                }
                final ReadWriteShardDataTreeTransaction openTransaction = checkOpen();
                startAbort();
                openTransaction.abort(() -> {
                    recordAndSendSuccess(envelope, now, new ModifyTransactionSuccess(getIdentifier(),
                        request.getSequence()));
                    finishAbort();
                });
                return null;
            case READY:
                ensureReady(request.getModifications());
                return replyModifySuccess(request.getSequence());
            case SIMPLE:
                ensureReady(request.getModifications());
                directCommit(envelope, now);
                return null;
            case THREE_PHASE:
                ensureReady(request.getModifications());
                coordinatedCommit(envelope, now);
                return null;
            default:
                LOG.warn("{}: rejecting unsupported protocol {}", persistenceId(), maybeProto.get());
                throw new UnsupportedRequestException(request);
        }
    }

    private void ensureReady(final Collection<TransactionModification> modifications) {
        // We may have a combination of READY + SIMPLE/THREE_PHASE , in which case we want to ready the transaction
        // only once.
        if (state instanceof Ready) {
            LOG.debug("{}: {} is already in state {}", persistenceId(), getIdentifier(), state);
            return;
        }

        applyModifications(modifications);
        state = new Ready(checkOpen().ready(Optional.empty()));
        LOG.debug("{}: transitioned {} to ready", persistenceId(), getIdentifier());
    }

    private void throwIfFailed() throws RequestException {
        if (state instanceof Failed) {
            LOG.debug("{}: {} has failed, rejecting request", persistenceId(), getIdentifier());
            throw ((Failed) state).cause;
        }
    }

    private ReadWriteShardDataTreeTransaction checkOpen() {
        checkState(state instanceof Open, "%s expect to be open, is in state %s", getIdentifier(), state);
        return ((Open) state).openTransaction;
    }

    private Ready checkReady() {
        checkState(state instanceof Ready, "%s expect to be ready, is in state %s", getIdentifier(), state);
        return (Ready) state;
    }

    private DataTreeModification checkSealed() {
        checkState(state instanceof Sealed, "%s expect to be sealed, is in state %s", getIdentifier(), state);
        return ((Sealed) state).sealedModification;
    }

    private static void throwUnhandledCommitStage(final Ready ready) {
        throw new IllegalStateException("Unhandled commit stage " + ready.stage);
    }
}
