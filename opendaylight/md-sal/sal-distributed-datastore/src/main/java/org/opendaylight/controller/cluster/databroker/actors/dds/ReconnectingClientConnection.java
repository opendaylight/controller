/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import com.google.common.base.Preconditions;
import java.util.Optional;
import java.util.Queue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ReconnectingClientConnection extends AbstractClientConnection {
    private static final Logger LOG = LoggerFactory.getLogger(ReconnectingClientConnection.class);

    private final Queue<TransmittedConnectionEntry> inflight;
    private final ShardBackendInfo backend;

    ReconnectingClientConnection(final AbstractClientConnection oldConnection,
        final Queue<TransmittedConnectionEntry> inflight) {
        super(oldConnection);
        this.backend = oldConnection.getBackendInfo().orElse(null);
        this.inflight = Preconditions.checkNotNull(inflight);
    }

    @Override
    Optional<ShardBackendInfo> getBackendInfo() {
        return Optional.ofNullable(backend);
    }

    ConnectedClientConnection toConnected(final ShardBackendInfo backend) {
        // FIXME
        return null;
    }
}
