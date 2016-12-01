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

@Beta
@NotThreadSafe
public final class ConnectedClientConnection<T extends BackendInfo> extends AbstractReceivingClientConnection<T> {
    ConnectedClientConnection(final ClientActorContext context, final Long cookie, final T backend) {
        super(context, cookie, backend);
    }

    @Override
    ClientActorBehavior<T> reconnectConnection(final ClientActorBehavior<T> current) {
        final ReconnectingClientConnection<T> next = new ReconnectingClientConnection<>(this);
        setForwarder(new SimpleReconnectForwarder(next));
        current.reconnectConnection(this, next);
        return current;
    }
}
