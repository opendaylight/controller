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

    public ConnectingClientConnection(final ClientActorContext context, final Long cookie) {
        super(context, cookie);
    }

    @Override
    public Optional<T> getBackendInfo() {
        return Optional.empty();
    }

    @Override
    public void receiveResponse(final ResponseEnvelope<?> envelope) {
        LOG.warn("Initial connection {} ignoring response {}", this, envelope);
    }
}
