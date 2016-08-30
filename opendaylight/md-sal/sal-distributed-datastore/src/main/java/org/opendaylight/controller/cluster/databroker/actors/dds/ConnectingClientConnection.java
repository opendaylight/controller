/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import org.opendaylight.controller.cluster.access.client.ClientActorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ConnectingClientConnection extends AbstractClientConnection {
    private static final Logger LOG = LoggerFactory.getLogger(ConnectedClientConnection.class);

    ConnectingClientConnection(final ClientActorContext context) {
        super(context);
    }

    ConnectedClientConnection toConnected(final ShardBackendInfo backend) {
        LOG.warn("Attempted to connect already-connected connection {}", this);
        // FIXME:
        return null;
    }
}
