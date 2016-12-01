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
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;
import javax.annotation.concurrent.NotThreadSafe;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.RequestEnvelope;

@Beta
@NotThreadSafe
public final class ConnectedClientConnection<T extends BackendInfo> extends AbstractReceivingClientConnection<T> {
    public ConnectedClientConnection(final ClientActorContext context, final Long cookie, final T backend) {
        super(context, cookie, backend);
    }

    @Override
    ClientActorBehavior<T> reconnectConnection(final ClientActorBehavior<T> current) {
        final ReconnectingClientConnection<T> next = new ReconnectingClientConnection<>(this);
        setForwarder(new SimpleReconnectForwarder(next));
        current.reconnectConnection(this, next);
        return current;
    }

    @Override
    int remoteMaxMessages() {
        return backend().getMaxMessages();
    }

    @Override
    Entry<ActorRef, RequestEnvelope> prepareForTransmit(final Request<?, ?> req) {
        return new SimpleImmutableEntry<>(backend().getActor(), new RequestEnvelope(
            req.toVersion(backend().getVersion()), backend().getSessionId(), nextTxSequence()));
    }
}
