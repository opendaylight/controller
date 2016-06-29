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
import com.google.common.base.Verify;
import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.FutureCallback;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.cluster.access.commands.CommitLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ExistsTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ExistsTransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.OutOfOrderRequestException;
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
 * Frontend transaction state as observed by the shard leader.
 *
 * @author Robert Varga
 */
@NotThreadSafe
final class FrontendTransaction {
    private static final Logger LOG = LoggerFactory.getLogger(FrontendTransaction.class);

    private final AbstractFrontendHistory history;
    private final TransactionIdentifier id;

    /**
     * It is possible that after we process a request and send a response that response gets lost and the client
     * initiates a retry. Since subsequent requests can mutate transaction state we need to retain the response until
     * it is acknowledged by the client.
     */
    private final Queue<Object> replayQueue = new ArrayDeque<>();
    private long firstReplaySequence;
    private Long lastPurgedSequence;
    private long expectedSequence;

    private ReadWriteShardDataTreeTransaction openTransaction;
    private ModifyTransactionSuccess cachedModifySuccess;
    private DataTreeModification sealedModification;
    private ShardDataTreeCohort readyCohort;

    private FrontendTransaction(final AbstractFrontendHistory history, final TransactionIdentifier id,
            final ReadWriteShardDataTreeTransaction transaction) {
        this.history = Preconditions.checkNotNull(history);
        this.id = Preconditions.checkNotNull(id);
        this.openTransaction = Preconditions.checkNotNull(transaction);
    }

    private FrontendTransaction(final AbstractFrontendHistory history, final TransactionIdentifier id,
            final DataTreeModification mod) {
        this.history = Preconditions.checkNotNull(history);
        this.id = Preconditions.checkNotNull(id);
        this.sealedModification = Preconditions.checkNotNull(mod);
    }

    static FrontendTransaction createOpen(final AbstractFrontendHistory history,
            final ReadWriteShardDataTreeTransaction transaction) {
        return new FrontendTransaction(history, transaction.getIdentifier(), transaction);
    }

    static FrontendTransaction createReady(final AbstractFrontendHistory history, final TransactionIdentifier id,
            final DataTreeModification mod) {
        return new FrontendTransaction(history, id, mod);
    }

    java.util.Optional<TransactionSuccess<?>> replaySequence(final long sequence) throws RequestException {
        // Fast path check: if the requested sequence is the next request, bail early
        if (expectedSequence == sequence) {
            return java.util.Optional.empty();
        }

        // Check sequencing: we do not need to bother with future requests
        if (Long.compareUnsigned(expectedSequence, sequence) < 0) {
            throw new OutOfOrderRequestException(expectedSequence);
        }

        // Sanity check: if we have purged sequences, this has to be newer
        if (lastPurgedSequence != null && Long.compareUnsigned(lastPurgedSequence, sequence) >= 0) {
            // FIXME: report dead sequence, this is a hard client error
            throw new IllegalArgumentException();
        }

        // At this point we have established that the requested sequence lies in the open interval
        // (lastPurgedSequence, expectedSequence). That does not actually mean we have a response, as the commit
        // machinery is asynchronous, hence a reply may be in the works and not available.

        long replaySequence = firstReplaySequence;
        final Iterator<?> it = replayQueue.iterator();
        while (it.hasNext()) {
            final Object replay = it.next();
            if (replaySequence == sequence) {
                if (replay instanceof RequestException) {
                    throw (RequestException) replay;
                }

                Verify.verify(replay instanceof TransactionSuccess);
                return java.util.Optional.of((TransactionSuccess<?>) replay);
            }

            replaySequence++;
        }

        // Not found
        return java.util.Optional.empty();
    }

    void purgeSequencesUpTo(final long sequence) {
        // FIXME: implement this

        lastPurgedSequence = sequence;
    }

    // Sequence has already been checked
    TransactionSuccess<?> handleRequest(final TransactionRequest<?> request, final RequestEnvelope envelope)
            throws RequestException {
        if (request instanceof ModifyTransactionRequest) {
            return handleModifyTransaction((ModifyTransactionRequest) request, envelope);
        } else if (request instanceof CommitLocalTransactionRequest) {
            handleCommitLocalTransaction((CommitLocalTransactionRequest) request, envelope);
            return null;
        } else if (request instanceof ExistsTransactionRequest) {
            return handleExistsTransaction((ExistsTransactionRequest) request);
        } else if (request instanceof ReadTransactionRequest) {
            return handleReadTransaction((ReadTransactionRequest) request);
        } else if (request instanceof TransactionPreCommitRequest) {
            handleTransactionPreCommit((TransactionPreCommitRequest) request, envelope);
            return null;
        } else if (request instanceof TransactionDoCommitRequest) {
            handleTransactionDoCommit((TransactionDoCommitRequest) request, envelope);
            return null;
        } else if (request instanceof TransactionAbortRequest) {
            return handleTransactionAbort((TransactionAbortRequest) request);
        } else {
            throw new UnsupportedRequestException(request);
        }
    }

    private void recordResponse(final long sequence, final Object response) {
        if (replayQueue.isEmpty()) {
            firstReplaySequence = sequence;
        }
        replayQueue.add(response);
        expectedSequence++;
    }

    private <T extends TransactionSuccess<?>> T recordSuccess(final long sequence, final T success) {
        recordResponse(sequence, success);
        return success;
    }

    private void recordAndSendSuccess(final RequestEnvelope envelope, final TransactionSuccess<?> success) {
        recordResponse(success.getSequence(), success);
        envelope.sendSuccess(success);
    }

    private void recordAndSendFailure(final RequestEnvelope envelope, final RuntimeRequestException failure) {
        recordResponse(envelope.getMessage().getSequence(), failure);
        envelope.sendFailure(failure);
    }

    private void handleTransactionPreCommit(final TransactionPreCommitRequest request,
            final RequestEnvelope envelope) throws RequestException {
        readyCohort.preCommit(new FutureCallback<DataTreeCandidate>() {
            @Override
            public void onSuccess(final DataTreeCandidate result) {
                recordAndSendSuccess(envelope, new TransactionPreCommitSuccess(readyCohort.getIdentifier(),
                    request.getSequence()));
            }

            @Override
            public void onFailure(final Throwable t) {
                failedPreCommit(envelope, t);
            }
        });
    }

    private void failedPreCommit(final RequestEnvelope envelope, final Throwable t) {
        // TODO Auto-generated method stub
        readyCohort = null;
    }

    private void handleTransactionDoCommit(final TransactionDoCommitRequest request, final RequestEnvelope envelope)
            throws RequestException {
        readyCohort.commit(new FutureCallback<UnsignedLong>() {
            @Override
            public void onSuccess(final UnsignedLong result) {
                successfulCommit(envelope);
            }

            @Override
            public void onFailure(final Throwable t) {
                failedCommit(envelope, t);
            }
        });
    }

    private void failedCommit(final RequestEnvelope envelope, final Throwable t) {
        recordAndSendFailure(envelope, new RuntimeRequestException("Commit failed", t));
        readyCohort = null;
    }

    private TransactionSuccess<?> handleTransactionAbort(final TransactionAbortRequest request)
            throws RequestException {
        try {
            readyCohort.abort().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to preCommit " + id, e);
        }

        readyCohort = null;
        return new TransactionAbortSuccess(id, request.getSequence());
    }

    private void coordinatedCommit(final RequestEnvelope envelope) {
        readyCohort.canCommit(new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                recordAndSendSuccess(envelope, new TransactionCanCommitSuccess(readyCohort.getIdentifier(),
                    envelope.getMessage().getSequence()));
            }

            @Override
            public void onFailure(final Throwable t) {
                failedCanCommit(envelope, t);
            }
        });
    }

    private void failedCanCommit(final RequestEnvelope envelope, final Throwable t) {
        // TODO Auto-generated method stub
    }

    private void directCommit(final RequestEnvelope envelope) {
        readyCohort.canCommit(new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                successfulDirectCanCommit(envelope);
            }

            @Override
            public void onFailure(final Throwable t) {
                failedDirectCanCommit(envelope, t);
            }
        });

    }

    private void failedDirectCanCommit(final RequestEnvelope envelope, final Throwable t) {
        // TODO Auto-generated method stub
        readyCohort = null;
    }

    private void successfulDirectCanCommit(final RequestEnvelope envelope) {
        readyCohort.preCommit(new FutureCallback<DataTreeCandidate>() {
            @Override
            public void onSuccess(final DataTreeCandidate result) {
                successfulDirectPreCommit(envelope);
            }

            @Override
            public void onFailure(final Throwable t) {
                failedDirectPreCommit(envelope, t);
            }
        });
    }

    private void failedDirectPreCommit(final RequestEnvelope envelope, final Throwable t) {
        // TODO Auto-generated method stub
        readyCohort = null;
    }

    private void successfulDirectPreCommit(final RequestEnvelope envelope) {
        readyCohort.commit(new FutureCallback<UnsignedLong>() {

            @Override
            public void onSuccess(final UnsignedLong result) {
                successfulCommit(envelope);
            }

            @Override
            public void onFailure(final Throwable t) {
                failedDirectCommit(envelope, t);
            }
        });
    }

    private void failedDirectCommit(final RequestEnvelope envelope, final Throwable t) {
        // TODO Auto-generated method stub
        readyCohort = null;
    }

    private void successfulCommit(final RequestEnvelope envelope) {
        recordAndSendSuccess(envelope, new TransactionCommitSuccess(readyCohort.getIdentifier(),
            envelope.getMessage().getSequence()));
        readyCohort = null;
    }

    private TransactionSuccess<?> handleCommitLocalTransaction(final CommitLocalTransactionRequest request,
            final RequestEnvelope envelope) throws RequestException {
        if (sealedModification.equals(request.getModification())) {
            readyCohort = history.createReadyCohort(id, sealedModification);

            if (request.isCoordinated()) {
                coordinatedCommit(envelope);
            } else {
                directCommit(envelope);
            }

            return null;
        } else {
            throw new UnsupportedRequestException(request);
        }
    }

    private ExistsTransactionSuccess handleExistsTransaction(final ExistsTransactionRequest request)
            throws RequestException {
        final Optional<NormalizedNode<?, ?>> data = openTransaction.getSnapshot().readNode(request.getPath());
        return recordSuccess(request.getSequence(), new ExistsTransactionSuccess(id, request.getSequence(),
            data.isPresent()));
    }

    private ReadTransactionSuccess handleReadTransaction(final ReadTransactionRequest request) throws RequestException {
        final Optional<NormalizedNode<?, ?>> data = openTransaction.getSnapshot().readNode(request.getPath());
        return recordSuccess(request.getSequence(), new ReadTransactionSuccess(id, request.getSequence(), data));
    }

    private ModifyTransactionSuccess replyModifySuccess(final long sequence) {
        if (cachedModifySuccess == null) {
            cachedModifySuccess = new ModifyTransactionSuccess(id, sequence);
        }

        return recordSuccess(sequence, cachedModifySuccess);
    }

    private TransactionSuccess<?> handleModifyTransaction(final ModifyTransactionRequest request,
            final RequestEnvelope envelope) throws RequestException {

        final DataTreeModification modification = openTransaction.getSnapshot();
        for (TransactionModification m : request.getModifications()) {
            if (m instanceof TransactionDelete) {
                modification.delete(m.getPath());
            } else if (m instanceof TransactionWrite) {
                modification.write(m.getPath(), ((TransactionWrite) m).getData());
            } else if (m instanceof TransactionMerge) {
                modification.merge(m.getPath(), ((TransactionMerge) m).getData());
            } else {
                LOG.warn("{}: ignoring unhandled modification {}", history.persistenceId(), m);
            }
        }

        final java.util.Optional<PersistenceProtocol> maybeProto = request.getPersistenceProtocol();
        if (!maybeProto.isPresent()) {
            return replyModifySuccess(request.getSequence());
        }

        switch (maybeProto.get()) {
            case ABORT:
                openTransaction.abort();
                openTransaction = null;
                return replyModifySuccess(request.getSequence());
            case SIMPLE:
                readyCohort = openTransaction.ready();
                openTransaction = null;
                directCommit(envelope);
                return null;
            case THREE_PHASE:
                readyCohort = openTransaction.ready();
                openTransaction = null;
                coordinatedCommit(envelope);
                return null;
            default:
                throw new UnsupportedRequestException(request);
        }
    }
}
