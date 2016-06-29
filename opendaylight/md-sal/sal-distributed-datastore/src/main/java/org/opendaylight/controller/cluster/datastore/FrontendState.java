/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.cluster.access.commands.CommitLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ExistsTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.OutOfOrderRequestException;
import org.opendaylight.controller.cluster.access.commands.ReadTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionAbortRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionDoCommitRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionPreCommitRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionSuccess;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.access.concepts.UnsupportedRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Frontend state as observed by the shard leader. this class is responsible for tracking generations and sequencing
 * in the frontend/backend conversation.
 *
 * @author Robert Varga
 */
@NotThreadSafe
final class FrontendState {
    private static final OutOfOrderRequestException UNSEQUENCED_START = new OutOfOrderRequestException(0);
    private static final Logger LOG = LoggerFactory.getLogger(FrontendState.class);

    private final Map<TransactionIdentifier, FrontendTransaction> transactions = new HashMap<>();
    private final ClientIdentifier clientId;
    private final String persistenceId;

    FrontendState(final String persistenceId, final ClientIdentifier clientId) {
        this.persistenceId = Preconditions.checkNotNull(persistenceId);
        this.clientId = Preconditions.checkNotNull(clientId);
    }

    long getGeneration() {
        return clientId.getGeneration();
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

        final LocalHistoryIdentifier historyId = request.getTarget().getHistoryId();
        if (historyId.getHistoryId() != 0) {
            // FIXME: lookup local history


        } else {
            // FIXME: initialize to singleton local history
        }

        final FrontendTransaction ret = new FrontendTransaction();
        transactions.put(id, ret);
        LOG.debug("{}: allocated new transaction {}", persistenceId, id);
        return ret;
    }

    private TransactionSuccess<?> handleCommitLocalTransaction(final CommitLocalTransactionRequest request,
            final long sequence) {
        // TODO Auto-generated method stub
        return null;
    }

    private TransactionSuccess<?> handleExistsTransaction(final ExistsTransactionRequest request, final long sequence) {
        // TODO Auto-generated method stub
        return null;
    }

    private TransactionSuccess<?> handleReadTransaction(final ReadTransactionRequest request, final long sequence) {
        // TODO Auto-generated method stub
        return null;
    }

    private TransactionSuccess<?> handleModifyTransaction(final ModifyTransactionRequest request, final long sequence) {
        // TODO Auto-generated method stub
        return null;
    }

    private TransactionSuccess<?> handleTransactionPreCommit(final TransactionPreCommitRequest request, final long sequence) {
        // TODO Auto-generated method stub
        return null;
    }

    private TransactionSuccess<?> handleTransactionDoCommit(final TransactionDoCommitRequest request, final long sequence) {
        // TODO Auto-generated method stub
        return null;
    }

    private TransactionSuccess<?> handleTransactionAbort(final TransactionAbortRequest request, final long sequence) {
        // TODO Auto-generated method stub
        return null;
    }

    void retire() {
        // FIXME: flush all state
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(FrontendState.class).add("clientId", clientId).toString();
    }
}
