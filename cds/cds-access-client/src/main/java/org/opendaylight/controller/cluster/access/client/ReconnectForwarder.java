/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import static java.util.Objects.requireNonNull;

import java.util.function.Consumer;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.access.concepts.Request;

/**
 * Forwarder class responsible for routing requests from the previous connection incarnation back to the originator,
 * which can then convert them as appropriate.
 */
public abstract class ReconnectForwarder {
    private final @NonNull AbstractReceivingClientConnection<?> successor;

    ReconnectForwarder(final AbstractReceivingClientConnection<?> successor) {
        this.successor = requireNonNull(successor);
    }

    /**
     * Constructor for forwarding towards a connected connection.
     *
     * @param successor the successor {@link ConnectedClientConnection}
     */
    protected ReconnectForwarder(final ConnectedClientConnection<?> successor) {
        this((AbstractReceivingClientConnection<?>) successor);
    }

    /**
     * Constructor for forwarding towards a reconnecting connection.
     *
     * @param successor the successor {@link ReconnectingClientConnection}
     */
    protected ReconnectForwarder(final ReconnectingClientConnection<?> successor) {
        this((AbstractReceivingClientConnection<?>) successor);
    }

    /**
     * Forward a {@link ConnectionEntry} to the successor via
     * {@link AbstractClientConnection#sendRequest(Request, Consumer)}.
     *
     * @param entry the {@link ConnectionEntry}
     */
    protected final void sendToSuccessor(final ConnectionEntry entry) {
        successor.sendRequest(entry.getRequest(), entry.getCallback());
    }

    /**
     * Forward a {@link ConnectionEntry} to the successor via
     * {@link AbstractClientConnection#enqueueRequest(Request, Consumer, long)}.
     *
     * @param entry the {@link ConnectionEntry}
     */
    protected final void replayToSuccessor(final ConnectionEntry entry) {
        successor.enqueueRequest(entry.getRequest(), entry.getCallback(), entry.getEnqueuedTicks());
    }

    protected abstract void forwardEntry(ConnectionEntry entry, long now);

    protected abstract void replayEntry(ConnectionEntry entry, long now);

    final @NonNull AbstractReceivingClientConnection<?> successor() {
        return successor;
    }
}
