/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import com.google.common.annotations.Beta;
import java.util.Optional;

@Beta
public final class ConnectingClientConnection<T extends BackendInfo> extends AbstractClientConnection<T> {
    // Initial state, never instantiated externally
    ConnectingClientConnection(final ClientActorContext context, final Long cookie) {
        super(context, cookie, new TransmitQueue.Halted());
    }

    @Override
    public Optional<T> getBackendInfo() {
        return Optional.empty();
    }

    @Override
    ClientActorBehavior<T> reconnectConnection(final ClientActorBehavior<T> current) {
        throw new UnsupportedOperationException("Attempted to reconnect a connecting connection");
    }
}
