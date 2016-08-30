/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import akka.actor.ActorRef;
import com.google.common.annotations.Beta;
import java.util.ArrayDeque;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.cluster.access.concepts.RequestEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Beta
@NotThreadSafe
public final class ConnectedClientConnection<T extends BackendInfo> extends AbstractReceivingClientConnection<T> {
    private static final Logger LOG = LoggerFactory.getLogger(ConnectedClientConnection.class);

    private long nextTxSequence;

    public ConnectedClientConnection(final ClientActorContext context, final T backend) {
        super(context, backend, new ArrayDeque<>());
    }

    private TransmittedConnectionEntry transmit(final ConnectionEntry entry) {
        final long txSequence = nextTxSequence++;

        final RequestEnvelope toSend = new RequestEnvelope(entry.getRequest().toVersion(remoteVersion()), sessionId(),
            txSequence);

        final ActorRef actor = remoteActor();
        LOG.trace("Transmitting request {} as {} to {}", entry.getRequest(), toSend, actor);
        actor.tell(toSend, ActorRef.noSender());

        return new TransmittedConnectionEntry(entry, sessionId(), txSequence, readTime());
    }

    @Override
    void enqueueEntry(final ConnectionEntry entry) {
        if (inflightSize() < remoteMaxMessages()) {
            appendToInflight(transmit(entry));
            LOG.debug("Enqueued request {} to queue {}", entry.getRequest(), this);
        } else {
            LOG.debug("Queue is at capacity, delayed sending of request {}", entry.getRequest());
            super.enqueueEntry(entry);
        }
    }

    @Override
    void sendMessages(final int count) {
        int toSend = count;

        while (toSend > 0) {
            final ConnectionEntry e = dequeEntry();
            if (e == null) {
                break;
            }

            LOG.debug("Transmitting entry {}", e);
            appendToInflight(transmit(e));
            toSend--;
        }
    }

    ReconnectingClientConnection<T> toReconnecting() {
        // FIXME: once we are multi-threaded, we need to synchronize with enqueueEntry()
        return new ReconnectingClientConnection<>(this);
    }

}
