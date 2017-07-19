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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import java.util.function.Consumer;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.ABIVersion;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequestBuilder;
import org.opendaylight.controller.cluster.access.commands.TransactionWrite;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.Request;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.messaging.MessageSlice;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;

public class ConnectedClientConnectionTest
        extends AbstractClientConnectionTest<ConnectedClientConnection<BackendInfo>, BackendInfo> {

    @Test
    public void testCheckTimeoutConnectionTimedout() throws Exception {
        final Consumer<Response<?, ?>> callback = mock(Consumer.class);
        connection.sendRequest(createRequest(replyToProbe.ref()), callback);
        final long now = context.ticker().read() + ConnectedClientConnection.DEFAULT_BACKEND_ALIVE_TIMEOUT_NANOS;
        final Optional<Long> timeout = connection.checkTimeout(now);
        Assert.assertNull(timeout);
    }

    @Override
    protected ConnectedClientConnection<BackendInfo> createConnection() {
        final BackendInfo backend = new BackendInfo(backendProbe.ref(), 0L, ABIVersion.BORON, 10);
        final ConnectingClientConnection<BackendInfo> connectingConn = new ConnectingClientConnection<>(context, 0L);
        return  new ConnectedClientConnection<>(connectingConn, backend);
    }

    @Override
    @Test
    public void testReconnectConnection() throws Exception {
        final ClientActorBehavior<BackendInfo> behavior = mock(ClientActorBehavior.class);
        connection.lockedReconnect(behavior, mock(RequestException.class));
        verify(behavior).reconnectConnection(same(connection), any(ReconnectingClientConnection.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSendSliceableMessageRequest() {
        final ClientActorConfig config = AccessClientUtil.newMockClientActorConfig();
        doReturn(5).when(config).getMaximumMessageSliceSize();
        context = new ClientActorContext(contextProbe.ref(), PERSISTENCE_ID, system, CLIENT_ID, config);
        connection = createConnection();

        final Consumer<Response<?, ?>> callback = mock(Consumer.class);

        final TransactionIdentifier identifier =
                new TransactionIdentifier(new LocalHistoryIdentifier(CLIENT_ID, 0L), 0L);
        ModifyTransactionRequestBuilder reqBuilder =
                new ModifyTransactionRequestBuilder(identifier, replyToProbe.ref());
        reqBuilder.addModification(new TransactionWrite(YangInstanceIdentifier.EMPTY, Builders.containerBuilder()
                .withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier.create(
                        QName.create("namespace", "localName"))).build()));
        reqBuilder.setSequence(0L);
        final Request<?, ?> request = reqBuilder.build();
        connection.sendRequest(request, callback);

        backendProbe.expectMsgClass(MessageSlice.class);
    }
}
