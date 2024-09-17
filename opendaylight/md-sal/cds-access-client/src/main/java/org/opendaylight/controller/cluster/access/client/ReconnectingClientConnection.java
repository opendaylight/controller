/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import static java.util.Objects.requireNonNull;

import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link AbstractClientConnection} which is being reconnected after having timed out.
 *
 * @param <T> {@link BackendInfo} type
 */
public final class ReconnectingClientConnection<T extends BackendInfo> extends AbstractReceivingClientConnection<T> {
    private static final Logger LOG = LoggerFactory.getLogger(ReconnectingClientConnection.class);

    private RequestException cause;

    ReconnectingClientConnection(final ConnectedClientConnection<T> oldConnection, final RequestException cause) {
        super(oldConnection);
        this.cause = requireNonNull(cause);
    }

    @Override
    long backendSilentTicks(final long now) {
        // We do not want to reconnect this connection, as we need the timer to to keep running
        return 0;
    }

    @Override
    @SuppressWarnings("checkstyle:hiddenField")
    ClientActorBehavior<T> lockedReconnect(final ClientActorBehavior<T> current, final RequestException cause) {
        this.cause = requireNonNull(cause);
        LOG.warn("Skipping reconnect of already-reconnecting connection {}", this);
        return current;
    }

    @Override
    RequestException enrichPoison(final RequestException ex) {
        if (ex.getCause() != null) {
            ex.addSuppressed(cause);
        } else {
            ex.initCause(cause);
        }

        return ex;
    }

}
