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
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;
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
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.access.concepts.UnsupportedRequestException;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
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
    private final TransactionIdentifier id;

    /**
     * It is possible that after we process a request and send a response that response gets lost and the client
     * initiates a retry. Since subsequent requests can mutate transaction state we need to retain the response until
     * it is acknowledged by the client.
     */
    private final Queue<Object> replayQueue = new ArrayDeque<>();
    private long firstReplaySequence;
    private Long lastPurgedSequence = null;

    private long expectedSequence = 0;

    private ModifyTransactionSuccess cachedModifySuccess;

    private ReadWriteShardDataTreeTransaction openTransaction;
    private DataTreeModification sealedModification;
    private ShardDataTreeCohort readyCohort;

    private FrontendTransaction(final TransactionIdentifier id, final ReadWriteShardDataTreeTransaction transaction) {
        this.id = Preconditions.checkNotNull(id);
        this.openTransaction = Preconditions.checkNotNull(transaction);
    }

    private FrontendTransaction(final TransactionIdentifier id, final DataTreeModification mod) {
        this.id = Preconditions.checkNotNull(id);
        this.sealedModification = Preconditions.checkNotNull(mod);
    }

    static FrontendTransaction createOpen(final TransactionIdentifier id, final ReadWriteShardDataTreeTransaction transaction) {
        return new FrontendTransaction(id, transaction);
    }

    static FrontendTransaction createReady(final TransactionIdentifier id, final DataTreeModification mod) {
        return new FrontendTransaction(id, mod);
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
    TransactionSuccess<?> handleRequest(final TransactionRequest<?> request, final long sequence)
            throws RequestException {
        if (request instanceof ModifyTransactionRequest) {
            return handleModifyTransaction((ModifyTransactionRequest) request, sequence);
        } else if (request instanceof ExistsTransactionRequest) {
            return handleExistsTransaction((ExistsTransactionRequest) request, sequence);
        } else if (request instanceof ReadTransactionRequest) {
            return handleReadTransaction((ReadTransactionRequest) request, sequence);
        } else if (request instanceof TransactionPreCommitRequest) {
            return handleTransactionPreCommit((TransactionPreCommitRequest) request, sequence);
        } else if (request instanceof TransactionDoCommitRequest) {
            return handleTransactionDoCommit((TransactionDoCommitRequest) request, sequence);
        } else if (request instanceof TransactionAbortRequest) {
            return handleTransactionAbort((TransactionAbortRequest) request, sequence);
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

    private RequestException recordFailure(final long sequence, final RequestException failure) throws RequestException {
        recordResponse(sequence, failure);
        throw failure;
    }

    private TransactionSuccess<?> handleTransactionPreCommit(final TransactionPreCommitRequest request,
            final long sequence) throws RequestException {
        readyCohort.preCommit();

        // FIXME: start precommit timer
        return new TransactionPreCommitSuccess(id);
    }

    private TransactionSuccess<?> handleTransactionDoCommit(final TransactionDoCommitRequest request,
            final long sequence) throws RequestException {
        readyCohort.commit();
        readyCohort = null;
        return new TransactionCommitSuccess(id);
    }

    private TransactionSuccess<?> handleTransactionAbort(final TransactionAbortRequest request, final long sequence)
            throws RequestException {
        readyCohort.abort();
        readyCohort = null;
        return new TransactionAbortSuccess(id);
    }

    private TransactionSuccess<?> handleCommitLocalTransaction(final CommitLocalTransactionRequest request,
            final long sequence) throws RequestException {
        if (sealedModification.equals(request.getModification())) {
            return startCommit(request.isCoordinated());
        } else {
            throw new UnsupportedRequestException(request);
        }
    }

    private TransactionSuccess<?> startCommit(final boolean coordinated) {
        // TODO Auto-generated method stub
        return null;
    }

    private ExistsTransactionSuccess handleExistsTransaction(final ExistsTransactionRequest request,
            final long sequence) throws RequestException {
        final Optional<NormalizedNode<?, ?>> data = openTransaction.getSnapshot().readNode(request.getPath());
        return recordSuccess(sequence, new ExistsTransactionSuccess(id, data.isPresent()));
    }

    private ReadTransactionSuccess handleReadTransaction(final ReadTransactionRequest request, final long sequence)
            throws RequestException {
        final Optional<NormalizedNode<?, ?>> data = openTransaction.getSnapshot().readNode(request.getPath());
        return recordSuccess(sequence, new ReadTransactionSuccess(id, data));
    }

    private ModifyTransactionSuccess replyModifySuccess(final long sequence) {
        if (cachedModifySuccess == null) {
            cachedModifySuccess = new ModifyTransactionSuccess(id);
        }

        return recordSuccess(sequence, cachedModifySuccess);
    }

    private TransactionSuccess<?> handleModifyTransaction(final ModifyTransactionRequest request, final long sequence)
            throws RequestException {

        final DataTreeModification modification = openTransaction.getSnapshot();
        for (TransactionModification m : request.getModifications()) {
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

        final java.util.Optional<PersistenceProtocol> maybeProto = request.getPersistenceProtocol();
        if (!maybeProto.isPresent()) {
            return replyModifySuccess(sequence);
        }


        switch (maybeProto.get()) {
            case ABORT:
                openTransaction.abort();
                openTransaction = null;
                return replyModifySuccess(sequence);
            case SIMPLE:
                readyCohort = openTransaction.ready();
                openTransaction = null;

                readyCohort.canCommit();
                readyCohort.preCommit();
                readyCohort.commit();
                readyCohort = null;
                return replyModifySuccess(sequence);
            case THREE_PHASE:
                readyCohort = openTransaction.ready();
                openTransaction = null;

                readyCohort.canCommit();
                // FIXME: setup an expiration timer
                return replyModifySuccess(sequence);
            default:
                throw new UnsupportedRequestException(request);
        }
    }
}
