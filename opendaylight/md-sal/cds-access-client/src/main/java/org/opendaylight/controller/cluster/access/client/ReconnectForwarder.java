/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import com.google.common.base.Preconditions;
import java.util.function.Consumer;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Forwarder class responsible for routing requests from the previous connection incarnation back to the originator,
 * which can then convert them as appropriate.
 *
 * @author Robert Varga
 */
public abstract class ReconnectForwarder {
    static final Logger LOG = LoggerFactory.getLogger(ReconnectForwarder.class);
    // Visible for subclass method handle
    private final AbstractReceivingClientConnection<?> successor;

    protected ReconnectForwarder(final AbstractReceivingClientConnection<?> successor) {
        this.successor = Preconditions.checkNotNull(successor);
    }

    protected final void sendToSuccessor(final Request<?, ?> request, final Consumer<Response<?, ?>> callback) {
        successor.sendRequest(request, callback);
    }

    protected abstract void forwardEntry(ConnectionEntry entry, long now);

    protected abstract void replayEntry(ConnectionEntry entry, long now);

    final AbstractReceivingClientConnection<?> successor() {
        return successor;
    }
}
