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
import java.util.Map.Entry;
import java.util.Optional;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.RequestEnvelope;

@Beta
public final class ConnectingClientConnection<T extends BackendInfo> extends AbstractClientConnection<T> {
    // Initial state, never instantiated externally
    ConnectingClientConnection(final ClientActorContext context, final Long cookie) {
        super(context, cookie);
    }

    @Override
    public Optional<T> getBackendInfo() {
        return Optional.empty();
    }

    @Override
    ClientActorBehavior<T> reconnectConnection(final ClientActorBehavior<T> current) {
        throw new UnsupportedOperationException("Attempted to reconnect a connecting connection");
    }

    @Override
    Entry<ActorRef, RequestEnvelope> prepareForTransmit(final Request<?, ?> req) {
        // This is guarded by remoteMaxMessages() == 0
        throw new UnsupportedOperationException("Attempted to transmit on a connecting connection");
    }

    @Override
    int remoteMaxMessages() {
        return 0;
    }
}
