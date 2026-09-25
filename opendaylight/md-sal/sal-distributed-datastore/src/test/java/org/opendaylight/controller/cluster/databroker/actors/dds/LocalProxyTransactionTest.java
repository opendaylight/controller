/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.assertFutureEquals;

import com.google.common.base.Ticker;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.cluster.access.client.ClientActorBehavior;
import org.opendaylight.controller.cluster.access.client.InternalCommand;
import org.opendaylight.controller.cluster.access.commands.AbortLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.CommitLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ExistsTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ExistsTransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.PersistenceProtocol;
import org.opendaylight.controller.cluster.access.commands.ReadTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ReadTransactionSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionPurgeRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.yangtools.yang.data.tree.api.CursorAwareDataTreeModification;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeModificationCursor;

abstract class LocalProxyTransactionTest<T extends LocalProxyTransaction> extends AbstractProxyTransactionTest<T> {
    @Test
    @Override
    void testExists() throws Exception {
        assertFutureEquals(Boolean.TRUE, transaction.exists(PATH_1));
        assertFutureEquals(Boolean.FALSE, transaction.exists(PATH_3));
    }

    @Override
    @Test
    void testRead() throws Exception {
        assertFutureEquals(Optional.of(DATA_1), transaction.read(PATH_1));
        assertFutureEquals(Optional.empty(), transaction.read(PATH_3));
    }

    @Test
    void testAbort() {
        transaction.abort();
        getTester().expectTransactionRequest(AbortLocalTransactionRequest.class);
    }

    @SuppressWarnings("unchecked")
    private void setupExecuteInActor() {
        doAnswer(inv -> {
            inv.getArgument(0, InternalCommand.class).execute(mock(ClientActorBehavior.class));
            return null;
        }).when(context).executeInActor(any(InternalCommand.class));
    }

    @Test
    void testHandleForwardedRemoteReadRequest() {
        final var probe = createProbe();
        final var request = new ReadTransactionRequest(TRANSACTION_ID, 0L, probe.ref(), PATH_1, true);
        final Consumer<Response<?, ?>> callback = mock();
        setupExecuteInActor();

        transaction.handleReplayedRemoteRequest(request, callback, Ticker.systemTicker().read());
        final ArgumentCaptor<Response<?, ?>> captor = ArgumentCaptor.captor();
        verify(callback).accept(captor.capture());
        final var success = assertInstanceOf(ReadTransactionSuccess.class, captor.getValue());
        assertEquals(Optional.of(DATA_1), success.getData());
    }

    @Test
    void testHandleForwardedRemoteExistsRequest() {
        final var probe = createProbe();
        final var request = new ExistsTransactionRequest(TRANSACTION_ID, 0L, probe.ref(), PATH_1, true);
        final Consumer<Response<?, ?>> callback = mock();
        setupExecuteInActor();

        transaction.handleReplayedRemoteRequest(request, callback, Ticker.systemTicker().read());
        final ArgumentCaptor<Response<?, ?>> captor = ArgumentCaptor.captor();
        verify(callback).accept(captor.capture());
        final var success = assertInstanceOf(ExistsTransactionSuccess.class, captor.getValue());
        assertTrue(success.getExists());
    }

    @Test
    void testHandleForwardedRemotePurgeRequest() {
        final var probe = createProbe();
        final var request = new TransactionPurgeRequest(TRANSACTION_ID, 0L, probe.ref());
        testHandleForwardedRemoteRequest(request);
    }

    @Test
    @Override
    void testForwardToRemoteAbort() {
        final var probe = createProbe();
        final var request = new AbortLocalTransactionRequest(TRANSACTION_ID, probe.ref());
        final var modifyRequest = testForwardToRemote(request, ModifyTransactionRequest.class);
        assertEquals(Optional.of(PersistenceProtocol.ABORT), modifyRequest.getPersistenceProtocol());
    }

    @Test
    @Override
    void testForwardToRemoteCommit() {
        final var probe = createProbe();
        final var modification = mock(CursorAwareDataTreeModification.class);
        final var request =
            new CommitLocalTransactionRequest(TRANSACTION_ID, 0L, probe.ref(), modification, null, true);
        doAnswer(LocalProxyTransactionTest::applyToCursorAnswer).when(modification).applyToCursor(any());
        final var modifyRequest = testForwardToRemote(request, ModifyTransactionRequest.class);
        verify(modification).applyToCursor(any());
        assertEquals(Optional.of(PersistenceProtocol.THREE_PHASE), modifyRequest.getPersistenceProtocol());
        checkModifications(modifyRequest);
    }

    @Test
    void testForwardToLocalAbort() {
        final var probe = createProbe();
        final var request = new AbortLocalTransactionRequest(TRANSACTION_ID, probe.ref());
        testForwardToLocal(request, AbortLocalTransactionRequest.class);
    }

    @Test
    void testForwardToLocalPurge() {
        final var probe = createProbe();
        final var request = new TransactionPurgeRequest(TRANSACTION_ID, 0L, probe.ref());
        testForwardToLocal(request, TransactionPurgeRequest.class);
    }

    final <R extends TransactionRequest<R>> R testForwardToLocal(final TransactionRequest<?> toForward,
            final Class<R> expectedMessageClass) {
        final Consumer<Response<?, ?>> callback = mock();
        final var transactionTester = createLocalProxy();
        final var successor = transactionTester.getTransaction();
        transaction.forwardToLocal(successor, toForward, callback);
        return transactionTester.expectTransactionRequest(expectedMessageClass);
    }

    /**
     * To emulate side effect of void method.
     * {@link CursorAwareDataTreeModification#applyToCursor(DataTreeModificationCursor)}
     *
     * @param invocation invocation
     * @return void - always null
     */
    static final <T> Answer<T> applyToCursorAnswer(final InvocationOnMock invocation) {
        final var cursor = invocation.getArgument(0, DataTreeModificationCursor.class);
        cursor.write(PATH_1.getLastPathArgument(), DATA_1);
        cursor.merge(PATH_2.getLastPathArgument(), DATA_2);
        cursor.delete(PATH_3.getLastPathArgument());
        return null;
    }
}
