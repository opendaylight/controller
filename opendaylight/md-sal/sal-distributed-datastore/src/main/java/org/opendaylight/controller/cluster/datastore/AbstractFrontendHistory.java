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
import org.opendaylight.controller.cluster.access.commands.AbortLocalTransactionRequest;
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
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class for providing logical tracking of frontend local histories. This class is specialized for
 * standalong transactions and chained transactions.
 *
 * @author Robert Varga
 */
abstract class AbstractFrontendHistory implements Identifiable<LocalHistoryIdentifier> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractFrontendHistory.class);
    private static final OutOfOrderRequestException UNSEQUENCED_START = new OutOfOrderRequestException(0);

    private final Map<TransactionIdentifier, FrontendTransaction> transactions = new HashMap<>();
    private final LocalHistoryIdentifier identifier;
    private final String persistenceId;

    AbstractFrontendHistory(final String persistenceId, final LocalHistoryIdentifier identifier) {
        this.persistenceId = Preconditions.checkNotNull(persistenceId);
        this.identifier = Preconditions.checkNotNull(identifier);
    }

    @Override
    public final LocalHistoryIdentifier getIdentifier() {
        return identifier;
    }

    final String persistenceId() {
        return persistenceId;
    }

    final TransactionSuccess<?> handleTransactionRequest(final TransactionRequest<?> request, final long sequence)
            throws RequestException {
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
        } else if (request instanceof AbortLocalTransactionRequest) {
            return handleAbortLocalTransaction((AbortLocalTransactionRequest) request, sequence);
        } else {
            throw new UnsupportedRequestException(request);
        }
    }

    abstract FrontendTransaction createOpenTransaction(TransactionIdentifier id) throws RequestException;
    abstract FrontendTransaction createReadyTransaction(TransactionIdentifier id, DataTreeModification mod)
        throws RequestException;

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

    private TransactionSuccess<?> handleTransactionAbort(final TransactionAbortRequest request, final long sequence)
            throws RequestException {
        // TODO Auto-generated method stub
        return null;
    }

    private TransactionSuccess<?> handleAbortLocalTransaction(final AbortLocalTransactionRequest request,
            final long sequence) throws RequestException {


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
        return ensureOpenTransaction(request, sequence).exists(request.getPath());
    }

    private ReadTransactionSuccess handleReadTransaction(final ReadTransactionRequest request, final long sequence)
            throws RequestException {
        return ensureOpenTransaction(request, sequence).read(request.getPath());
    }

    private TransactionSuccess<?> handleModifyTransaction(final ModifyTransactionRequest request, final long sequence)
            throws RequestException {
        final FrontendTransaction tx = ensureOpenTransaction(request, sequence);

        for (TransactionModification m : request.getModifications()) {
            if (m instanceof TransactionDelete) {
                tx.delete(m.getPath());
            } else if (m instanceof TransactionWrite) {
                tx.write(m.getPath(), ((TransactionWrite) m).getData());
            } else if (m instanceof TransactionMerge) {
                tx.merge(m.getPath(), ((TransactionMerge) m).getData());
            } else {
                LOG.warn("{}: ignoring unhandled modification {}", persistenceId(), m);
            }
        }

        final Optional<PersistenceProtocol> maybeProto = request.getPersistenceProtocol();
        if (!maybeProto.isPresent()) {
            return new ModifyTransactionSuccess(request.getTarget());
        }

        switch (maybeProto.get()) {
            case ABORT:
                return tx.directAbort();
            case SIMPLE:
                return tx.directCommit();
            case THREE_PHASE:
                return tx.coordinatedCommit();
            default:
                throw new UnsupportedRequestException(request);
        }
    }

    private FrontendTransaction ensureOpenTransaction(final TransactionRequest<?> request, final long sequence)
            throws RequestException {
        final TransactionIdentifier id = request.getTarget();
        final FrontendTransaction existing = transactions.get(id);
        if (existing != null) {
            existing.checkRequestSequence(sequence);
            return existing;
        }

        if (sequence != 0) {
            LOG.debug("{}: no transaction state present, unexpected request {}", persistenceId(), request);
            throw UNSEQUENCED_START;
        }

        final FrontendTransaction ret = createOpenTransaction(id);
        transactions.put(id, ret);
        LOG.debug("{}: allocated new transaction {}", persistenceId(), id);
        return ret;
    }
}
