/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.opendaylight.controller.cluster.access.ABIVersion;

public class ConnectedClientConnectionTest
        extends AbstractClientConnectionTest<ConnectedClientConnection<BackendInfo>, BackendInfo> {

    @Override
    protected ConnectedClientConnection createConnection() {
        final BackendInfo backend = new BackendInfo(backendProbe.ref(), 0L, ABIVersion.BORON, 10);
        return new ConnectedClientConnection<>(context, 0L, backend);
    }

    @Override
    @Test
    public void testReconnectConnection() throws Exception {
        final ClientActorBehavior<BackendInfo> behavior = mock(ClientActorBehavior.class);
        connection.reconnectConnection(behavior);
        verify(behavior).reconnectConnection(same(connection), any(ReconnectingClientConnection.class));
    }

}