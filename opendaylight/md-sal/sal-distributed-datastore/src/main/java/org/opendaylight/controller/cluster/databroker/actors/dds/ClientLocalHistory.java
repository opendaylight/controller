/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

/**
 * Client-side view of a local history. This class tracks all state related to a particular history and routes
 * frontend requests towards the backend.
 *
 * This interface is used by the world outside of the actor system and in the actor system it is manifested via
 * its client actor. That requires some state transfer with {@link DistributedDataStoreClientBehavior}. In order to
 * reduce request latency, all messages are carbon-copied (and enqueued first) to the client actor.
 *
 * @author Robert Varga
 */
@Beta
public final class ClientLocalHistory extends AbstractClientHistory implements AutoCloseable {
    private static enum State {
        IDLE,
        TX_OPEN,
        CLOSED,
    }

    private static final AtomicReferenceFieldUpdater<ClientLocalHistory, State> STATE_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(ClientLocalHistory.class, State.class, "state");
    private static final AtomicLongFieldUpdater<ClientLocalHistory> NEXT_TX_UPDATER =
            AtomicLongFieldUpdater.newUpdater(ClientLocalHistory.class, "nextTx");

    private volatile State state = State.IDLE;
    // Used via NEXT_TX_UPDATER
    @SuppressWarnings("unused")
    private volatile long nextTx = 0;

    ClientLocalHistory(final DistributedDataStoreClientBehavior client, final LocalHistoryIdentifier historyId) {
        super(client, historyId);
    }

    private void updateState(final State expected, final State next) {
        final boolean success = STATE_UPDATER.compareAndSet(this, expected, next);
        Preconditions.checkState(success, "Race condition detected, state changed from %s to %s", expected, state);
    }

    public ClientTransaction createTransaction() {
        final State local = state;
        Preconditions.checkState(local == State.IDLE, "Local history %s state is %s", this, local);
        updateState(local, State.TX_OPEN);

        return new ClientTransaction(getClient(), this,
            new TransactionIdentifier(getIdentifier(), NEXT_TX_UPDATER.getAndIncrement(this)));
    }

    @Override
    public void close() {
        final State local = state;
        if (local != State.CLOSED) {
            Preconditions.checkState(local == State.IDLE, "Local history %s has an open transaction", this);
            updateState(local, State.CLOSED);
        }
    }

    @Override
    void transactionComplete(final ClientTransaction transaction) {
        final State local = state;
        Verify.verify(state == State.TX_OPEN, "Local history %s is in unexpected state %s", this, local);
        updateState(local, State.IDLE);
        super.transactionComplete(transaction);
    }
}
