/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import org.opendaylight.controller.cluster.access.commands.DeadTransactionException;
import org.opendaylight.controller.cluster.access.commands.LocalHistorySuccess;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class LocalFrontendHistory extends AbstractFrontendHistory {
    private static enum State {
        OPEN,
        CLOSED,
    }

    private static final Logger LOG = LoggerFactory.getLogger(LocalFrontendHistory.class);

    private Long lastSeenTransaction = null;
    private State state = State.OPEN;

    LocalFrontendHistory(final String persistenceId, final LocalHistoryIdentifier identifier) {
        super(persistenceId, identifier);
    }

    @Override
    FrontendTransaction createOpenTransaction(final TransactionIdentifier id) throws RequestException {
        checkDeadTransaction(id);
        lastSeenTransaction = id.getTransactionId();
        return FrontendTransaction.createOpen(id);
    }

    @Override
    FrontendTransaction createReadyTransaction(final TransactionIdentifier id, final DataTreeModification mod)
            throws RequestException {
        checkDeadTransaction(id);
        lastSeenTransaction = id.getTransactionId();
        return FrontendTransaction.createReady(id, mod);
    }

    LocalHistorySuccess destroy() throws RequestException {
        if (state != State.CLOSED) {
            LOG.debug("{}: closing history {}", persistenceId(), getIdentifier());

            // FIXME: add any finalization as needed
            state = State.CLOSED;
        }

        // FIXME: record a tombstone in the journal
        return new LocalHistorySuccess(getIdentifier());
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
