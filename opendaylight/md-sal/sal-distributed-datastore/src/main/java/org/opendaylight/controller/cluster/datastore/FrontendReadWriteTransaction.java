/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.FutureCallback;
import java.util.Collection;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
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
 * Frontend read-write transaction state as observed by the shard leader.
 *
 * @author Robert Varga
 */
@NotThreadSafe
final class FrontendReadWriteTransaction extends FrontendTransaction {
    private static final Logger LOG = LoggerFactory.getLogger(FrontendReadWriteTransaction.class);

    private ReadWriteShardDataTreeTransaction openTransaction;
    private DataTreeModification sealedModification;
    private ShardDataTreeCohort readyCohort;

    private FrontendReadWriteTransaction(final AbstractFrontendHistory history, final TransactionIdentifier id,
            final ReadWriteShardDataTreeTransaction transaction) {
        super(history, id);
        this.openTransaction = Preconditions.checkNotNull(transaction);
    }

    private FrontendReadWriteTransaction(final AbstractFrontendHistory history, final TransactionIdentifier id,
            final DataTreeModification mod) {
        super(history, id);
        this.sealedModification = Preconditions.checkNotNull(mod);
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
    @Nullable TransactionSuccess<?> doHandleRequest(final TransactionRequest<?> request, final RequestEnvelope envelope,
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
            handleTransactionAbort(request.getSequence(), envelope, now);
            return null;
        } else if (request instanceof AbortLocalTransactionRequest) {
            handleLocalTransactionAbort(request.getSequence(), envelope, now);
            return null;
        } else {
            LOG.warn("Rejecting unsupported request {}", request);
            throw new UnsupportedRequestException(request);
        }
    }

    private void handleTransactionPreCommit(final TransactionPreCommitRequest request,
            final RequestEnvelope envelope, final long now) throws RequestException {
        readyCohort.preCommit(new FutureCallback<DataTreeCandidate>() {
            @Override
            public void onSuccess(final DataTreeCandidate result) {
                recordAndSendSuccess(envelope, now, new TransactionPreCommitSuccess(readyCohort.getIdentifier(),
                    request.getSequence()));
            }

            @Override
            public void onFailure(final Throwable failure) {
                recordAndSendFailure(envelope, now, new RuntimeRequestException("Precommit failed", failure));
                readyCohort = null;
            }
        });
    }

    private void handleTransactionDoCommit(final TransactionDoCommitRequest request, final RequestEnvelope envelope,
            final long now) throws RequestException {
        readyCohort.commit(new FutureCallback<UnsignedLong>() {
            @Override
            public void onSuccess(final UnsignedLong result) {
                successfulCommit(envelope, now);
            }

            @Override
            public void onFailure(final Throwable failure) {
                recordAndSendFailure(envelope, now, new RuntimeRequestException("Commit failed", failure));
                readyCohort = null;
            }
        });
    }

    private void handleLocalTransactionAbort(final long sequence, final RequestEnvelope envelope, final long now) {
        Preconditions.checkState(readyCohort == null, "Transaction {} encountered local abort with commit underway",
                getIdentifier());
        openTransaction.abort(() -> recordAndSendSuccess(envelope, now, new TransactionAbortSuccess(getIdentifier(),
            sequence)));
    }

    private void handleTransactionAbort(final long sequence, final RequestEnvelope envelope, final long now) {
        if (readyCohort == null) {
            openTransaction.abort(() -> recordAndSendSuccess(envelope, now, new TransactionAbortSuccess(getIdentifier(),
                sequence)));
            return;
        }

        readyCohort.abort(new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                readyCohort = null;
                recordAndSendSuccess(envelope, now, new TransactionAbortSuccess(getIdentifier(), sequence));
                LOG.debug("Transaction {} aborted", getIdentifier());
            }

            @Override
            public void onFailure(final Throwable failure) {
                readyCohort = null;
                LOG.warn("Transaction {} abort failed", getIdentifier(), failure);
                recordAndSendFailure(envelope, now, new RuntimeRequestException("Abort failed", failure));
            }
        });
    }

    private void coordinatedCommit(final RequestEnvelope envelope, final long now) {
        readyCohort.canCommit(new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                recordAndSendSuccess(envelope, now, new TransactionCanCommitSuccess(readyCohort.getIdentifier(),
                    envelope.getMessage().getSequence()));
            }

            @Override
            public void onFailure(final Throwable failure) {
                recordAndSendFailure(envelope, now, new RuntimeRequestException("CanCommit failed", failure));
                readyCohort = null;
            }
        });
    }

    private void directCommit(final RequestEnvelope envelope, final long now) {
        readyCohort.canCommit(new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                successfulDirectCanCommit(envelope, now);
            }

            @Override
            public void onFailure(final Throwable failure) {
                recordAndSendFailure(envelope, now, new RuntimeRequestException("CanCommit failed", failure));
                readyCohort = null;
            }
        });
    }

    void successfulDirectCanCommit(final RequestEnvelope envelope, final long startTime) {
        readyCohort.preCommit(new FutureCallback<DataTreeCandidate>() {
            @Override
            public void onSuccess(final DataTreeCandidate result) {
                successfulDirectPreCommit(envelope, startTime);
            }

            @Override
            public void onFailure(final Throwable failure) {
                recordAndSendFailure(envelope, startTime, new RuntimeRequestException("PreCommit failed", failure));
                readyCohort = null;
            }
        });
    }

    void successfulDirectPreCommit(final RequestEnvelope envelope, final long startTime) {
        readyCohort.commit(new FutureCallback<UnsignedLong>() {
            @Override
            public void onSuccess(final UnsignedLong result) {
                successfulCommit(envelope, startTime);
            }

            @Override
            public void onFailure(final Throwable failure) {
                recordAndSendFailure(envelope, startTime, new RuntimeRequestException("DoCommit failed", failure));
                readyCohort = null;
            }
        });
    }

    void successfulCommit(final RequestEnvelope envelope, final long startTime) {
        recordAndSendSuccess(envelope, startTime, new TransactionCommitSuccess(readyCohort.getIdentifier(),
            envelope.getMessage().getSequence()));
        readyCohort = null;
    }

    private void handleCommitLocalTransaction(final CommitLocalTransactionRequest request,
            final RequestEnvelope envelope, final long now) throws RequestException {
        if (!sealedModification.equals(request.getModification())) {
            LOG.warn("Expecting modification {}, commit request has {}", sealedModification, request.getModification());
            throw new UnsupportedRequestException(request);
        }

        final java.util.Optional<Exception> optFailure = request.getDelayedFailure();
        if (optFailure.isPresent()) {
            readyCohort = history().createFailedCohort(getIdentifier(), sealedModification, optFailure.get());
        } else {
            readyCohort = history().createReadyCohort(getIdentifier(), sealedModification);
        }

        if (request.isCoordinated()) {
            coordinatedCommit(envelope, now);
        } else {
            directCommit(envelope, now);
        }
    }

    private ExistsTransactionSuccess handleExistsTransaction(final ExistsTransactionRequest request)
            throws RequestException {
        final Optional<NormalizedNode<?, ?>> data = openTransaction.getSnapshot().readNode(request.getPath());
        return recordSuccess(request.getSequence(), new ExistsTransactionSuccess(getIdentifier(), request.getSequence(),
            data.isPresent()));
    }

    private ReadTransactionSuccess handleReadTransaction(final ReadTransactionRequest request)
            throws RequestException {
        final Optional<NormalizedNode<?, ?>> data = openTransaction.getSnapshot().readNode(request.getPath());
        return recordSuccess(request.getSequence(), new ReadTransactionSuccess(getIdentifier(), request.getSequence(),
            data));
    }

    private ModifyTransactionSuccess replyModifySuccess(final long sequence) {
        return recordSuccess(sequence, new ModifyTransactionSuccess(getIdentifier(), sequence));
    }

    private @Nullable TransactionSuccess<?> handleModifyTransaction(final ModifyTransactionRequest request,
            final RequestEnvelope envelope, final long now) throws RequestException {

        final Collection<TransactionModification> mods = request.getModifications();
        if (!mods.isEmpty()) {
            final DataTreeModification modification = openTransaction.getSnapshot();
            for (TransactionModification m : mods) {
                if (m instanceof TransactionDelete) {
                    modification.delete(m.getPath());
                } else if (m instanceof TransactionWrite) {
                    modification.write(m.getPath(), ((TransactionWrite) m).getData());
                } else if (m instanceof TransactionMerge) {
                    modification.merge(m.getPath(), ((TransactionMerge) m).getData());
                } else {
                    LOG.warn("{}: ignoring unhandled modification {}", history().persistenceId(), m);
                }
            }
        }

        final java.util.Optional<PersistenceProtocol> maybeProto = request.getPersistenceProtocol();
        if (!maybeProto.isPresent()) {
            return replyModifySuccess(request.getSequence());
        }

        switch (maybeProto.get()) {
            case ABORT:
                openTransaction.abort(() -> replyModifySuccess(request.getSequence()));
                openTransaction = null;
                return null;
            case READY:
                ensureReady();
                return replyModifySuccess(request.getSequence());
            case SIMPLE:
                ensureReady();
                directCommit(envelope, now);
                return null;
            case THREE_PHASE:
                ensureReady();
                coordinatedCommit(envelope, now);
                return null;
            default:
                LOG.warn("{}: rejecting unsupported protocol {}", history().persistenceId(), maybeProto.get());
                throw new UnsupportedRequestException(request);
        }
    }

    private void ensureReady() {
        // We may have a combination of READY + SIMPLE/THREE_PHASE , in which case we want to ready the transaction
        // only once.
        if (readyCohort == null) {
            readyCohort = openTransaction.ready();
            LOG.debug("{}: transitioned {} to ready", history().persistenceId(), openTransaction.getIdentifier());
            openTransaction = null;
        }
    }
}
