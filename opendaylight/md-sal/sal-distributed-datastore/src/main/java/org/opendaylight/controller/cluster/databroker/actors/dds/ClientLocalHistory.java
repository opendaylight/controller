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
import java.util.Optional;
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
    ClientLocalHistory(final DistributedDataStoreClientBehavior client, final LocalHistoryIdentifier historyId) {
        super(client, historyId);
    }

    @Override
    public void close() {
        final State local = state();
        if (local != State.CLOSED) {
            Preconditions.checkState(local == State.IDLE, "Local history %s has an open transaction", this);
            updateState(local, State.CLOSED);
        }
    }

    @Override
    ClientTransaction doCreateTransaction() {
        final State local = state();
        Preconditions.checkState(local == State.IDLE, "Local history %s state is %s", this, local);
        updateState(local, State.TX_OPEN);

        return new ClientTransaction(this, new TransactionIdentifier(getIdentifier(), nextTx()));
    }

    @Override
    AbstractTransactionCommitCohort onTransactionReady(final TransactionIdentifier txId,
            final AbstractTransactionCommitCohort cohort) {
        // FIXME: deal with CLOSED here
        final State local = state();
        Verify.verify(local == State.TX_OPEN, "Local history %s is in unexpected state %s", this, local);
        updateState(local, State.IDLE);

        return super.onTransactionReady(txId, cohort);
    }

    @Override
    AbstractProxyHistory createHistoryProxy(final LocalHistoryIdentifier historyId,
            final Optional<ShardBackendInfo> backendInfo) {
        return AbstractProxyHistory.createClient(getClient(), backendInfo, historyId);
    }
}
