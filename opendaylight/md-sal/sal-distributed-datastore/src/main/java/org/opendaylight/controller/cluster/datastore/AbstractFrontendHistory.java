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
import com.google.common.base.Ticker;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.access.commands.AbstractReadTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.CommitLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.OutOfOrderRequestException;
import org.opendaylight.controller.cluster.access.commands.TransactionPurgeRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionPurgeResponse;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionSuccess;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.RequestEnvelope;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class for providing logical tracking of frontend local histories. This class is specialized for
 * standalone transactions and chained transactions.
 *
 * @author Robert Varga
 */
abstract class AbstractFrontendHistory implements Identifiable<LocalHistoryIdentifier> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractFrontendHistory.class);
    private static final OutOfOrderRequestException UNSEQUENCED_START = new OutOfOrderRequestException(0);

    private final Map<TransactionIdentifier, FrontendTransaction> transactions = new HashMap<>();
    private final String persistenceId;
    private final Ticker ticker;

    AbstractFrontendHistory(final String persistenceId, final Ticker ticker) {
        this.persistenceId = Preconditions.checkNotNull(persistenceId);
        this.ticker = Preconditions.checkNotNull(ticker);
    }

    final String persistenceId() {
        return persistenceId;
    }

    final long readTime() {
        return ticker.read();
    }

    final @Nullable TransactionSuccess<?> handleTransactionRequest(final TransactionRequest<?> request,
            final RequestEnvelope envelope, final long now) throws RequestException {
        final TransactionIdentifier id = request.getTarget();

        FrontendTransaction tx;
        if (request instanceof TransactionPurgeRequest) {
            tx = transactions.remove(id);
            if (tx == null) {
                // We have no record of the transaction, nothing to do
                LOG.debug("{}: no state for transaction {}, purge is complete", persistenceId(), id);
                return new TransactionPurgeResponse(id, request.getSequence());
            }
        } else {
            tx = transactions.get(id);
            if (tx == null) {
                // The transaction does not exist and we are about to create it, check sequence number
                if (request.getSequence() != 0) {
                    LOG.debug("{}: no transaction state present, unexpected request {}", persistenceId(), request);
                    throw UNSEQUENCED_START;
                }

                tx = createTransaction(request, id);
                transactions.put(id, tx);
            } else {
                final Optional<TransactionSuccess<?>> maybeReplay = tx.replaySequence(request.getSequence());
                if (maybeReplay.isPresent()) {
                    final TransactionSuccess<?> replay = maybeReplay.get();
                    LOG.debug("{}: envelope {} replaying response {}", persistenceId(), envelope, replay);
                    return replay;
                }
            }
        }

        return tx.handleRequest(request, envelope, now);
    }

    private FrontendTransaction createTransaction(final TransactionRequest<?> request, final TransactionIdentifier id)
            throws RequestException {
        if (request instanceof CommitLocalTransactionRequest) {
            LOG.debug("{}: allocating new ready transaction {}", persistenceId(), id);
            return createReadyTransaction(id, ((CommitLocalTransactionRequest) request).getModification());
        }
        if (request instanceof AbstractReadTransactionRequest) {
            if (((AbstractReadTransactionRequest<?>) request).isSnapshotOnly()) {
                LOG.debug("{}: allocatint new open snapshot {}", persistenceId(), id);
                return createOpenSnapshot(id);
            }
        }

        LOG.debug("{}: allocating new open transaction {}", persistenceId(), id);
        return createOpenTransaction(id);
    }

    abstract FrontendTransaction createOpenSnapshot(TransactionIdentifier id) throws RequestException;

    abstract FrontendTransaction createOpenTransaction(TransactionIdentifier id) throws RequestException;

    abstract FrontendTransaction createReadyTransaction(TransactionIdentifier id, DataTreeModification mod)
        throws RequestException;

    abstract ShardDataTreeCohort createReadyCohort(TransactionIdentifier id, DataTreeModification mod);

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).omitNullValues().add("identifier", getIdentifier())
                .add("persistenceId", persistenceId).add("transactions", transactions).toString();
    }
}
