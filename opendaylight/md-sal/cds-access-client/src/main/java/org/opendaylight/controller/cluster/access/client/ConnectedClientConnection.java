/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import com.google.common.annotations.Beta;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.cluster.access.concepts.RequestException;

@Beta
@NotThreadSafe
public final class ConnectedClientConnection<T extends BackendInfo> extends AbstractReceivingClientConnection<T> {

    ConnectedClientConnection(final AbstractClientConnection<T> oldConnection, final T newBackend) {
        super(oldConnection, newBackend);
    }

    @Override
    ClientActorBehavior<T> lockedReconnect(final ClientActorBehavior<T> current, final RequestException cause) {
        final ReconnectingClientConnection<T> next = new ReconnectingClientConnection<>(this, cause);
        setForwarder(new SimpleReconnectForwarder(next));
        current.reconnectConnection(this, next);
        return current;
    }
}
