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
import org.opendaylight.controller.cluster.access.commands.OutOfOrderRequestException;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionSuccess;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
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

        // FIXME: handle purging of transactions

        final TransactionIdentifier id = request.getTarget();
        FrontendTransaction tx = transactions.get(id);
        if (tx == null) {
            // The transaction does not exist and we are about to create it, check sequence number
            if (sequence != 0) {
                LOG.debug("{}: no transaction state present, unexpected request {}", persistenceId(), request);
                throw UNSEQUENCED_START;
            }

            if (request instanceof CommitLocalTransactionRequest) {
                tx = createReadyTransaction(id, ((CommitLocalTransactionRequest) request).getModification());
                LOG.debug("{}: allocated new ready transaction {}", persistenceId(), id);
            } else {
                tx = createOpenTransaction(id);
                LOG.debug("{}: allocated new open transaction {}", persistenceId(), id);
            }

            transactions.put(id, tx);
        } else {
            final Optional<TransactionSuccess<?>> replay = tx.replaySequence(sequence);
            if (replay.isPresent()) {
                return replay.get();
            }
        }

        return tx.handleRequest(request, sequence);
    }

    abstract FrontendTransaction createOpenTransaction(TransactionIdentifier id) throws RequestException;
    abstract FrontendTransaction createReadyTransaction(TransactionIdentifier id, DataTreeModification mod)
        throws RequestException;
}
