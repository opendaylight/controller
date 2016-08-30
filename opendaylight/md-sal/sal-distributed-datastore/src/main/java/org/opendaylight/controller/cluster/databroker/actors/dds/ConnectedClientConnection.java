/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import akka.actor.ActorRef;
import com.google.common.base.Preconditions;
import java.util.Optional;
import java.util.Queue;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.cluster.access.client.ClientActorContext;
import org.opendaylight.controller.cluster.access.concepts.RequestEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NotThreadSafe
final class ConnectedClientConnection extends AbstractClientConnection {
    private static final Logger LOG = LoggerFactory.getLogger(ConnectedClientConnection.class);

    private final Queue<TransmittedConnectionEntry> currentInflight;
    private final ShardBackendInfo backend;

    private long nextTxSequence;

    ConnectedClientConnection(final ClientActorContext context, final ShardBackendInfo backend) {
        super(context);
        currentInflight = null;
        this.backend = Preconditions.checkNotNull(backend);
    }

    private TransmittedConnectionEntry transmit(final ConnectionEntry entry) {
        final long txSequence = nextTxSequence++;

        final RequestEnvelope toSend = new RequestEnvelope(entry.getRequest().toVersion(backend.getVersion()),
            backend.getSessionId(), txSequence);

        final ActorRef actor = backend.getActor();
        LOG.trace("Transmitting request {} as {} to {}", entry.getRequest(), toSend, actor);
        actor.tell(toSend, ActorRef.noSender());

        return new TransmittedConnectionEntry(entry, backend.getSessionId(), txSequence, readTime());
    }

    @Override
    Optional<ShardBackendInfo> getBackendInfo() {
        return Optional.of(backend);
    }

    @Override
    void enqueueEntry(final ConnectionEntry entry) {
        if (currentInflight.size() < backend.getMaxMessages()) {
            currentInflight.offer(transmit(entry));
            LOG.debug("Enqueued request {} to queue {}", entry.getRequest(), this);
        } else {
            LOG.debug("Queue is at capacity, delayed sending of request {}", entry.getRequest());
            super.enqueueEntry(entry);
        }
    }

    ReconnectingClientConnection toReconnecting() {
        // FIXME: once we are multi-threaded, we need to synchronize with enqueueEntry()
        return new ReconnectingClientConnection(this, currentInflight);
    }
}
