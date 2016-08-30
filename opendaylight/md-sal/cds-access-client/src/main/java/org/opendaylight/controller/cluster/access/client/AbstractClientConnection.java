/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import akka.actor.ActorRef;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import java.util.ArrayDeque;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Consumer;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.controller.cluster.access.concepts.ResponseEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for a connection to the backend. Responsible to queueing and dispatch of requests toward the backend.
 *
 * @author Robert Varga
 */
@NotThreadSafe
public abstract class AbstractClientConnection<T extends BackendInfo> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractClientConnection.class);
    private final Queue<ConnectionEntry> pending;
    private final ClientActorContext context;

    private volatile ReconnectForwarder successor;

    AbstractClientConnection(final ClientActorContext context) {
        this.context = Preconditions.checkNotNull(context);
        this.pending = new ArrayDeque<>(1);
    }

    AbstractClientConnection(final AbstractClientConnection<T> oldConnection) {
        this.context = oldConnection.context;
        this.pending = oldConnection.pending;
    }

    public final ClientActorContext context() {
        return context;
    }

    public final ActorRef localActor() {
        return context.self();
    }

    final long readTime() {
        return context.ticker().read();
    }

    final Queue<ConnectionEntry> pending() {
        return pending;
    }

    public final void sendRequest(final Request<?, ?> request, final Consumer<Response<?, ?>> callback) {
        final ReconnectForwarder beforeQueue = successor;
        final ConnectionEntry entry = new ConnectionEntry(request, callback, readTime());
        if (beforeQueue != null) {
            LOG.trace("Forwarding entry {} from {} to {}", entry, this, beforeQueue);
            beforeQueue.forwardEntry(entry);
            return;
        }

        enqueueEntry(entry);

        final ReconnectForwarder afterQueue = successor;
        if (afterQueue != null) {
            synchronized (this) {
                spliceToSuccessor(afterQueue);
            }
        }
    }

    public final synchronized void setForwarder(final ReconnectForwarder forwarder) {
        Verify.verify(successor == null, "Successor {} already set on connection {}", successor, this);
        successor = Preconditions.checkNotNull(forwarder);
        spliceToSuccessor(forwarder);
    }

    @GuardedBy("this")
    private void spliceToSuccessor(final ReconnectForwarder successor) {
        LOG.debug("Connection {} superseded by {}, splicing queue", this, successor);

        ConnectionEntry entry = pending.poll();
        while (entry != null) {
            successor.forwardEntry(entry);
            entry = pending.poll();
        }
    }

    final ConnectionEntry dequeEntry() {
        return pending.poll();
    }

    void enqueueEntry(final ConnectionEntry entry) {
        pending.add(entry);
    }

    abstract void receiveResponse(final ResponseEnvelope<?> envelope);

    public abstract Optional<T> getBackendInfo();
}
