/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import akka.actor.ActorRef;
import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;

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
public final class ClientLocalHistory implements AutoCloseable {
    private static final AtomicIntegerFieldUpdater<ClientLocalHistory> STATE_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(ClientLocalHistory.class, "state");
    private static final int IDLE_STATE = 0;
    private static final int CLOSED_STATE = 1;

    private final ClientIdentifier clientId;
    private final long historyId;
    private final ActorRef backendActor;
    private final ActorRef clientActor;

    private volatile int state = IDLE_STATE;

    ClientLocalHistory(final DistributedDataStoreClientBehavior client, final long historyId,
            final ActorRef backendActor) {
        this.clientActor = client.self();
        this.backendActor = Preconditions.checkNotNull(backendActor);
        this.clientId = Verify.verifyNotNull(client.getIdentifier());
        this.historyId = historyId;
    }

    private void checkNotClosed() {
        if (state == CLOSED_STATE) {
            throw new IllegalStateException("Local history " + new LocalHistoryIdentifier(clientId, historyId) + " is closed");
        }
    }

    @Override
    public void close() {
        if (STATE_UPDATER.compareAndSet(this, IDLE_STATE, CLOSED_STATE)) {
            // FIXME: signal close to both client actor and backend actor
        } else if (state != CLOSED_STATE) {
            throw new IllegalStateException("Cannot close history with an open transaction");
        }
    }

    // FIXME: add client requests related to a particular local history
}
