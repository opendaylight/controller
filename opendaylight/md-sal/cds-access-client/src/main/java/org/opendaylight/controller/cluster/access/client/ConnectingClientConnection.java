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
import org.opendaylight.controller.cluster.access.concepts.ResponseEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Beta
public final class ConnectingClientConnection<T extends BackendInfo> extends AbstractClientConnection<T> {
    private static final Logger LOG = LoggerFactory.getLogger(ConnectingClientConnection.class);

    public ConnectingClientConnection(final ClientActorContext context) {
        super(context);
    }

    @Override
    public Optional<T> getBackendInfo() {
        return Optional.empty();
    }

    /**
     * Switch to connected-remote mode based on provided backend information.
     *
     * @param backend Backend information
     * @return A locked ConnectedClientConnection
     */
    public ConnectedClientConnection<T> toRemoteConnected(final T backend) {
        LOG.debug("Completing connecting connection {} with {}", this, backend);
        final ConnectedClientConnection<T> ret = new ConnectedClientConnection<>(context(), backend);
        setForwarder(new SimpleReconnectForwarder(ret));
        return ret;
    }

    @Override
    void receiveResponse(final ResponseEnvelope<?> envelope) {
        LOG.warn("Initial connection {} ignoring response {}", this, envelope);
    }
}
