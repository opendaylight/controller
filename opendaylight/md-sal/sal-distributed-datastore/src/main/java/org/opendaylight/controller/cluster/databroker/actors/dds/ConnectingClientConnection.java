/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import java.util.Optional;
import org.opendaylight.controller.cluster.access.client.ClientActorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ConnectingClientConnection extends AbstractClientConnection {
    private static final Logger LOG = LoggerFactory.getLogger(ConnectedClientConnection.class);

    ConnectingClientConnection(final ClientActorContext context) {
        super(context);
    }

    @Override
    Optional<ShardBackendInfo> getBackendInfo() {
        return Optional.empty();
    }

    /**
     * Switch to connected-remote mode based on provided backend information.
     *
     * @param backend Backend information
     * @return A locked ConnectedClientConnection
     */
    ConnectedClientConnection toRemoteConnected(final ShardBackendInfo backend) {
        LOG.debug("Completing connecting connection {} with {}", this, backend);
        return spliceTo(new ConnectedClientConnection(context(), backend));
    }
}
