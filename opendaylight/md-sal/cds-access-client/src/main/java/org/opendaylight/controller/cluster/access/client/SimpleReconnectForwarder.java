/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

// Simple forwarder which just pushes the entry to the successor
final class SimpleReconnectForwarder extends ReconnectForwarder {
    SimpleReconnectForwarder(final AbstractReceivingClientConnection<?> successor) {
        super(successor);
    }

    @Override
    protected void forwardEntry(final ConnectionEntry entry, final long now) {
        successor().sendEntry(entry, now);
    }

    @Override
    protected void replayEntry(final ConnectionEntry entry, final long now) {
        // We are executing in the context of the client thread, do not block
        successor().enqueueEntry(entry, now);
    }
}
