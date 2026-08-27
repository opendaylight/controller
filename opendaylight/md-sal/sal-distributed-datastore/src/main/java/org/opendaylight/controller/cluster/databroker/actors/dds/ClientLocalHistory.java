/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import org.opendaylight.controller.cluster.access.client.AbstractClientConnection;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

/**
 * Client-side view of a local history. This class tracks all state related to a particular history and routes
 * frontend requests towards the backend.
 *
 * <p>This interface is used by the world outside of the actor system and in the actor system it is manifested via its
 * client actor. That requires some state transfer with {@link AbstractDataStoreClientBehavior}. In order to reduce
 * request latency, all messages are carbon-copied (and enqueued first) to the client actor.
 */
public final class ClientLocalHistory extends AbstractClientHistory implements AutoCloseable {
    ClientLocalHistory(final AbstractDataStoreClientBehavior client, final LocalHistoryIdentifier historyId) {
        super(client, historyId);
    }

    @Override
    public void close() {
        doClose();
    }

    private State ensureIdleState() {
        final var local = state();
        if (local != State.IDLE) {
            throw new IllegalStateException("Local history %s state is %s".formatted(this, local));
        }
        return local;
    }

    @Override
    ClientSnapshot doCreateSnapshot() {
        ensureIdleState();
        return new ClientSnapshot(this, new TransactionIdentifier(getIdentifier(), nextTx()));
    }

    @Override
    ClientTransaction doCreateTransaction() {
        updateState(ensureIdleState(), State.TX_OPEN);
        return new ClientTransaction(this, new TransactionIdentifier(getIdentifier(), nextTx()));
    }

    @Override
    void onTransactionAbort(final AbstractClientHandle<?> snap) {
        if (snap instanceof ClientTransaction) {
            final var local = state();
            if (local == State.TX_OPEN) {
                updateState(local, State.IDLE);
            }
        }

        super.onTransactionAbort(snap);
    }

    @Override
    AbstractTransactionCommitCohort onTransactionReady(final ClientTransaction tx,
            final AbstractTransactionCommitCohort cohort) {
        final var local = state();
        return switch (local) {
            case IDLE -> throw new IllegalStateException(
                "Local history %s is idle when readying transaction %s".formatted(this, tx.getIdentifier()));
            case TX_OPEN -> {
                updateState(local, State.IDLE);
                yield super.onTransactionReady(tx, cohort);
            }
            case CLOSED -> super.onTransactionReady(tx, cohort);
        };
    }

    @Override
    ProxyHistory createHistoryProxy(final LocalHistoryIdentifier historyId,
            final AbstractClientConnection<ShardBackendInfo> connection) {
        return ProxyHistory.createClient(this, connection, historyId);
    }
}
