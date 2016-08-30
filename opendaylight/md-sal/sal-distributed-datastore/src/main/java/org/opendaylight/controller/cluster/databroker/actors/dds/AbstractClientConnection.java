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
import java.util.ArrayDeque;
import java.util.Optional;
import java.util.Queue;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.cluster.access.client.ClientActorContext;
import org.opendaylight.controller.cluster.access.client.RequestCallback;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NotThreadSafe
abstract class AbstractClientConnection {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractClientConnection.class);
    private final Queue<ConnectionEntry> pending;
    private final ClientActorContext context;

    private volatile AbstractClientConnection successor;

    AbstractClientConnection(final ClientActorContext context) {
        this.context = Preconditions.checkNotNull(context);
        this.pending = new ArrayDeque<>(1);
    }

    AbstractClientConnection(final AbstractClientConnection oldConnection) {
        this.context = oldConnection.context;
        this.pending = oldConnection.pending;
    }

    final ClientActorContext context() {
        return context;
    }

    final ActorRef localActor() {
        return context.self();
    }

    final long readTime() {
        return context.ticker().read();
    }

    final void addPending(final ConnectionEntry entry) {
    }

    final void sendRequest(final Request<?, ?> request, final RequestCallback callback) {
        final AbstractClientConnection beforeQueue = successor;
        final ConnectionEntry entry = new ConnectionEntry(request, callback, readTime());
        if (beforeQueue != null) {
            LOG.trace("Forwarding entry {} from {} to {}", entry, this, beforeQueue);
            beforeQueue.enqueueEntry(entry);
            return;
        }

        enqueueEntry(entry);

        final AbstractClientConnection afterQueue = successor;
        if (afterQueue != null) {
            synchronized (this) {
                spliceToSuccessor(afterQueue);
            }
        }
    }

    void enqueueEntry(final ConnectionEntry entry) {
        pending.add(entry);
    }

    @GuardedBy("this")
    void spliceToSuccessor( final AbstractClientConnection successor) {
        LOG.debug("Connection {} superseded by {}, splicing queue", this, successor);

        ConnectionEntry entry = pending.poll();
        while (entry != null) {
            successor.enqueueEntry(entry);
            entry = pending.poll();
        }
    }

    synchronized <T extends AbstractClientConnection> T spliceTo(final T newConn) {
        successor = newConn;
        spliceToSuccessor(newConn);
        return newConn;
    }

    abstract Optional<ShardBackendInfo> getBackendInfo();
}
