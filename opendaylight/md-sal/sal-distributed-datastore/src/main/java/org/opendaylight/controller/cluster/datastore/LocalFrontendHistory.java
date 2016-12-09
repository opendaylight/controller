/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.base.Preconditions;
import com.google.common.base.Ticker;
import org.opendaylight.controller.cluster.access.commands.DeadTransactionException;
import org.opendaylight.controller.cluster.access.commands.LocalHistorySuccess;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Chained transaction specialization of {@link AbstractFrontendHistory}. It prevents concurrent open transactions.
 *
 * @author Robert Varga
 */
final class LocalFrontendHistory extends AbstractFrontendHistory {
    private enum State {
        OPEN,
        CLOSED,
    }

    private static final Logger LOG = LoggerFactory.getLogger(LocalFrontendHistory.class);

    private final ShardDataTreeTransactionChain chain;

    private Long lastSeenTransaction;
    private State state = State.OPEN;

    LocalFrontendHistory(final String persistenceId, final Ticker ticker, final ShardDataTreeTransactionChain chain) {
        super(persistenceId, ticker);
        this.chain = Preconditions.checkNotNull(chain);
    }

    @Override
    public LocalHistoryIdentifier getIdentifier() {
        return chain.getIdentifier();
    }

    @Override
    FrontendTransaction createOpenSnapshot(final TransactionIdentifier id) throws RequestException {
        checkDeadTransaction(id);
        lastSeenTransaction = id.getTransactionId();
        return FrontendReadOnlyTransaction.create(this, chain.newReadOnlyTransaction(id));
    }

    @Override
    FrontendTransaction createOpenTransaction(final TransactionIdentifier id) throws RequestException {
        checkDeadTransaction(id);
        lastSeenTransaction = id.getTransactionId();
        return FrontendReadWriteTransaction.createOpen(this, chain.newReadWriteTransaction(id));
    }

    @Override
    FrontendTransaction createReadyTransaction(final TransactionIdentifier id, final DataTreeModification mod)
            throws RequestException {
        checkDeadTransaction(id);
        lastSeenTransaction = id.getTransactionId();
        return FrontendReadWriteTransaction.createReady(this, id, mod);
    }

    @Override
    ShardDataTreeCohort createReadyCohort(final TransactionIdentifier id, final DataTreeModification mod) {
        return chain.createReadyCohort(id, mod);
    }

    LocalHistorySuccess destroy(final long sequence, final long now) throws RequestException {
        if (state != State.CLOSED) {
            LOG.debug("{}: closing history {}", persistenceId(), getIdentifier());

            // FIXME: add any finalization as needed
            state = State.CLOSED;
        }

        // FIXME: record a DESTROY tombstone in the journal
        return new LocalHistorySuccess(getIdentifier(), sequence);
    }

    boolean isDestroyed() {
        return state == State.CLOSED;
    }

    private void checkDeadTransaction(final TransactionIdentifier id) throws RequestException {
        // FIXME: check if this history is still open
        // FIXME: check if the last transaction has been submitted

        // Transaction identifiers within a local history have to have increasing IDs
        if (lastSeenTransaction != null && Long.compareUnsigned(lastSeenTransaction, id.getTransactionId()) >= 0) {
            throw new DeadTransactionException(lastSeenTransaction);
        }
    }
}
