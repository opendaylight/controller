/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.opendaylight.controller.cluster.access.commands.CommitLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ExistsTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ExistsTransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.LocalHistorySuccess;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.OutOfOrderRequestException;
import org.opendaylight.controller.cluster.access.commands.PersistenceProtocol;
import org.opendaylight.controller.cluster.access.commands.ReadTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ReadTransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionAbortRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionDelete;
import org.opendaylight.controller.cluster.access.commands.TransactionDoCommitRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionMerge;
import org.opendaylight.controller.cluster.access.commands.TransactionModification;
import org.opendaylight.controller.cluster.access.commands.TransactionPreCommitRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionWrite;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.access.concepts.UnsupportedRequestException;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class FrontendLocalHistory implements Identifiable<LocalHistoryIdentifier> {
    private static enum State {
        OPEN,
        CLOSED,
    }

    private static final OutOfOrderRequestException UNSEQUENCED_START = new OutOfOrderRequestException(0);
    private static final Logger LOG = LoggerFactory.getLogger(FrontendLocalHistory.class);

    private final Map<TransactionIdentifier, FrontendTransaction> transactions = new HashMap<>();
    private final LocalHistoryIdentifier identifier;
    private final String persistenceId;

    private State state = State.OPEN;

    FrontendLocalHistory(final String persistenceId, final LocalHistoryIdentifier id) {
        this.persistenceId = Preconditions.checkNotNull(persistenceId);
        this.identifier = Preconditions.checkNotNull(id);
    }

    @Override
    public LocalHistoryIdentifier getIdentifier() {
        return identifier;
    }

    LocalHistorySuccess destroy() throws RequestException {
        if (state != State.CLOSED) {
            // FIXME: add any finalization as needed
            state = State.CLOSED;
        }

        return new LocalHistorySuccess(identifier);
    }

    boolean isDestroyed() {
        return state == State.CLOSED;
    }

    // TODO: will need to be specialized for single/ordered histories
    private FrontendTransaction createTransaction(final TransactionIdentifier id) throws RequestException {
        return new FrontendTransaction(id);
    }

    TransactionSuccess<?> handleTransactionRequest(final TransactionRequest<?> request, final long sequence) throws RequestException {
        if (request instanceof CommitLocalTransactionRequest) {
            return handleCommitLocalTransaction((CommitLocalTransactionRequest) request, sequence);
        } else if (request instanceof ModifyTransactionRequest) {
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


    private TransactionSuccess<?> handleTransactionPreCommit(final TransactionPreCommitRequest request,
            final long sequence) throws RequestException {
        // TODO Auto-generated method stub
        return null;
    }

    private TransactionSuccess<?> handleTransactionDoCommit(final TransactionDoCommitRequest request,
            final long sequence) throws RequestException {
        // TODO Auto-generated method stub
        return null;
    }

    private TransactionSuccess<?> handleTransactionAbort(final TransactionAbortRequest request, final long sequence) throws RequestException {
        // TODO Auto-generated method stub
        return null;
    }


    private TransactionSuccess<?> handleCommitLocalTransaction(final CommitLocalTransactionRequest request,
            final long sequence) throws RequestException {


        // TODO Auto-generated method stub
        return null;
    }

    private ExistsTransactionSuccess handleExistsTransaction(final ExistsTransactionRequest request,
            final long sequence) throws RequestException {
        return ensureTransaction(request, sequence).exists(request.getPath());
    }

    private ReadTransactionSuccess handleReadTransaction(final ReadTransactionRequest request, final long sequence)
            throws RequestException {
        return ensureTransaction(request, sequence).read(request.getPath());
    }

    private TransactionSuccess<?> handleModifyTransaction(final ModifyTransactionRequest request, final long sequence)
            throws RequestException {
        final FrontendTransaction tx = ensureTransaction(request, sequence);

        for (TransactionModification m : request.getModifications()) {
            if (m instanceof TransactionDelete) {
                tx.delete(m.getPath());
            } else if (m instanceof TransactionWrite) {
                tx.write(m.getPath(), ((TransactionWrite) m).getData());
            } else if (m instanceof TransactionMerge) {
                tx.merge(m.getPath(), ((TransactionMerge) m).getData());
            } else {
                LOG.warn("{}: ignoring unhandled modification {}", persistenceId, m);
            }
        }

        final Optional<PersistenceProtocol> maybeProto = request.getPersistenceProtocol();
        if (!maybeProto.isPresent()) {
            // FIXME: Simple case: send an acknowledgement
            return null;
        }

        switch (maybeProto.get()) {
            case ABORT:
                return tx.directAbort();
            case SIMPLE:
                return tx.directCommit();
            case THREE_PHASE:
                return tx.coordinatedCommit();
            default:
                // FIXME: throw a failure
                return null;
        }
    }

    private FrontendTransaction ensureTransaction(final TransactionRequest<?> request, final long sequence)
            throws RequestException {
        final TransactionIdentifier id = request.getTarget();
        FrontendTransaction existing = transactions.get(id);
        if (existing != null) {
            existing.checkRequestSequence(sequence);
            return existing;
        }

        if (sequence != 0) {
            LOG.debug("{}: no transaction state present, unexpected request {}", persistenceId, request);
            throw UNSEQUENCED_START;
        }

        final FrontendTransaction ret = createTransaction(id);
        transactions.put(id, ret);
        LOG.debug("{}: allocated new transaction {}", persistenceId, id);
        return ret;
    }
}
