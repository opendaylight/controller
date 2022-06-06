/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import static java.util.Objects.requireNonNull;

/**
 * Forwarder class responsible for routing requests from the previous connection incarnation back to the originator,
 * which can then convert them as appropriate.
*/
public abstract class ReconnectForwarder {
    private final AbstractReceivingClientConnection<?> successor;

    ReconnectForwarder(final AbstractReceivingClientConnection<?> successor) {
        this.successor = requireNonNull(successor);
    }

    protected ReconnectForwarder(final ConnectedClientConnection<?> successor) {
        this((AbstractReceivingClientConnection<?>) successor);
    }

    protected ReconnectForwarder(final ReconnectingClientConnection<?> successor) {
        this((AbstractReceivingClientConnection<?>) successor);
    }

    protected final void sendToSuccessor(final ConnectionEntry entry) {
        successor.sendRequest(entry.getRequest(), entry.getCallback());
    }

    protected final void replayToSuccessor(final ConnectionEntry entry) {
        successor.enqueueRequest(entry.getRequest(), entry.getCallback(), entry.getEnqueuedTicks());
    }

    protected abstract void forwardEntry(ConnectionEntry entry, long now);

    protected abstract void replayEntry(ConnectionEntry entry, long now);

    final AbstractReceivingClientConnection<?> successor() {
        return successor;
    }
}
