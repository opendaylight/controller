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
import java.util.Queue;
import org.opendaylight.controller.cluster.access.client.ClientActorContext;
import org.opendaylight.controller.cluster.access.client.RequestCallback;
import org.opendaylight.controller.cluster.access.concepts.Request;

abstract class AbstractClientConnection {
    private final Queue<ConnectionEntry> pending;
    private final ClientActorContext context;

    AbstractClientConnection(final ClientActorContext context) {
        this.context = Preconditions.checkNotNull(context);
        this.pending = new ArrayDeque<>(1);
    }

    AbstractClientConnection(AbstractClientConnection oldConnection) {
        this.context = oldConnection.context;
        this.pending = oldConnection.pending;
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
        enqueueEntry(new ConnectionEntry(request, callback, readTime()));
    }

    void enqueueEntry(final ConnectionEntry entry) {
        pending.add(entry);
    }
}
