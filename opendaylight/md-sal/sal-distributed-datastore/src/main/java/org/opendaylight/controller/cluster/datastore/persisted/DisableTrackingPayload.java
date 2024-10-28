/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore.persisted;

import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;

// We only support cds-aclient-api and that requires tracking. This payload is no longer generated, but retained for
// backwards compatibility.
@Deprecated(since = "11.0.0", forRemoval = true)
public final class DisableTrackingPayload extends AbstractIdentifiablePayload<ClientIdentifier> {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    DisableTrackingPayload(final ClientIdentifier clientId, final byte[] serialized) {
        super(clientId, serialized);
    }

    @Override
    protected DT externalizableProxy(final byte[] serialized) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected int externalizableProxySize() {
        throw new UnsupportedOperationException();
    }
}
