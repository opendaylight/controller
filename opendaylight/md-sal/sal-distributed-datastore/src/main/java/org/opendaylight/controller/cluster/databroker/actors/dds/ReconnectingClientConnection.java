/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import com.google.common.base.Preconditions;
import java.util.Queue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ReconnectingClientConnection extends AbstractClientConnection {
    private static final Logger LOG = LoggerFactory.getLogger(ReconnectingClientConnection.class);

    private final Queue<TransmittedConnectionEntry> inflight;

    ReconnectingClientConnection(final AbstractClientConnection oldConnection,
        final Queue<TransmittedConnectionEntry> inflight) {
        super(oldConnection);
        this.inflight = Preconditions.checkNotNull(inflight);
    }

    ConnectedClientConnection toConnected(final ShardBackendInfo backend) {
        // FIXME
        return null;
    }
}
