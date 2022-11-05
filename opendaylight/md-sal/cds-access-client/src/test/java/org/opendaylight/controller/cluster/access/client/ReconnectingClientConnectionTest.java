/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.OptionalLong;
import java.util.function.Consumer;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.commands.TransactionAbortSuccess;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.RequestSuccess;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.controller.cluster.access.concepts.ResponseEnvelope;
import org.opendaylight.controller.cluster.access.concepts.SuccessEnvelope;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

public class ReconnectingClientConnectionTest
        extends AbstractClientConnectionTest<ReconnectingClientConnection<BackendInfo>, BackendInfo> {

    @Test
    public void testCheckTimeoutConnectionTimedout() {
        final Consumer<Response<?, ?>> callback = mock(Consumer.class);
        connection.sendRequest(createRequest(replyToProbe.ref()), callback);
        final long now = context.ticker().read() + ConnectedClientConnection.DEFAULT_BACKEND_ALIVE_TIMEOUT_NANOS;
        final OptionalLong timeout = connection.checkTimeout(now);
        assertNotNull(timeout);
        assertTrue(timeout.isPresent());
    }

    @Override
    protected ReconnectingClientConnection<BackendInfo> createConnection() {
        final BackendInfo backend = new BackendInfo(backendProbe.ref(), "test", 0L, ABIVersion.current(), 10);
        final ConnectingClientConnection<BackendInfo> connectingConn = new ConnectingClientConnection<>(context, 0L,
                backend.getName());
        final ConnectedClientConnection<BackendInfo> connectedConn =
                new ConnectedClientConnection<>(connectingConn, backend);
        return new ReconnectingClientConnection<>(connectedConn, mock(RequestException.class));
    }

    @Override
    @Test
    public void testReconnectConnection() {
        final ClientActorBehavior<BackendInfo> behavior = mock(ClientActorBehavior.class);
        assertSame(behavior, connection.lockedReconnect(behavior, mock(RequestException.class)));
    }

    @Override
    @Test
    public void testSendRequestReceiveResponse() {
        final Consumer<Response<?, ?>> callback = mock(Consumer.class);
        final Request<?, ?> request = createRequest(replyToProbe.ref());
        connection.sendRequest(request, callback);
        backendProbe.expectNoMessage();
        final LocalHistoryIdentifier historyId = new LocalHistoryIdentifier(CLIENT_ID, 0L);
        final RequestSuccess<?, ?> message = new TransactionAbortSuccess(new TransactionIdentifier(historyId, 0L), 0L);
        final ResponseEnvelope<?> envelope = new SuccessEnvelope(message, 0L, 0L, 0L);
        connection.receiveResponse(envelope);
        verify(callback, after(1000).never()).accept(any());
    }
}
