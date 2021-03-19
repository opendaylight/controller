/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.access.client;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import java.util.function.Consumer;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.controller.cluster.access.concepts.ResponseEnvelope;

/**
 * Util class to access package private members in cds-access-client for test purposes.
 */
public final class AccessClientUtil {
    private AccessClientUtil() {
        throw new UnsupportedOperationException();
    }

    public static ClientActorContext createClientActorContext(final ActorSystem system, final ActorRef actor,
                                                              final ClientIdentifier id, final String persistenceId) {

        return spy(new ClientActorContext(actor, persistenceId, system, id, newMockClientActorConfig()));
    }

    public static ClientActorConfig newMockClientActorConfig() {
        ClientActorConfig mockConfig = mock(ClientActorConfig.class);
        lenient().doReturn(2_000_000).when(mockConfig).getMaximumMessageSliceSize();
        lenient().doReturn(1_000_000_000).when(mockConfig).getFileBackedStreamingThreshold();
        doReturn(AbstractClientConnection.DEFAULT_REQUEST_TIMEOUT_NANOS).when(mockConfig).getRequestTimeout();
        lenient().doReturn(AbstractClientConnection.DEFAULT_BACKEND_ALIVE_TIMEOUT_NANOS)
            .when(mockConfig).getBackendAlivenessTimerInterval();
        lenient().doReturn(AbstractClientConnection.DEFAULT_NO_PROGRESS_TIMEOUT_NANOS)
            .when(mockConfig).getNoProgressTimeout();
        return mockConfig;
    }

    public static <T extends BackendInfo> ConnectedClientConnection<T> createConnectedConnection(
            final ClientActorContext context, final Long cookie, final T backend) {
        return new ConnectedClientConnection<>(new ConnectingClientConnection<>(context, cookie, backend.getName()),
                backend);
    }

    public static void completeRequest(final AbstractClientConnection<? extends BackendInfo> connection,
                                       final ResponseEnvelope<?> envelope) {
        connection.receiveResponse(envelope);
    }

    public static ConnectionEntry createConnectionEntry(final Request<?, ?> request,
                                                        final Consumer<Response<?, ?>> callback,
                                                        final long now) {
        return new ConnectionEntry(request, callback, now);
    }

}
