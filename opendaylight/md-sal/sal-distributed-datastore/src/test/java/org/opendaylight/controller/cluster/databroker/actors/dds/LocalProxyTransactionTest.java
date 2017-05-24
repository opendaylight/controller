/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.assertFutureEquals;

import akka.testkit.TestProbe;
import com.google.common.base.Ticker;
import java.util.function.Consumer;
import org.junit.Assert;
import org.junit.Test;
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
import org.opendaylight.yangtools.yang.data.api.schema.tree.CursorAwareDataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModificationCursor;

public abstract class LocalProxyTransactionTest<T extends LocalProxyTransaction>
        extends AbstractProxyTransactionTest<T> {

    @Override
    @Test
    public void testExists() throws Exception {
        assertFutureEquals(true, transaction.exists(PATH_1));
        assertFutureEquals(false, transaction.exists(PATH_3));
    }

    @Override
    @Test
    public void testRead() throws Exception {
        assertFutureEquals(com.google.common.base.Optional.of(DATA_1), transaction.read(PATH_1));
        assertFutureEquals(com.google.common.base.Optional.absent(), transaction.read(PATH_3));
    }

    @Test
    public void testAbort() throws Exception {
        transaction.abort();
        getTester().expectTransactionRequest(AbortLocalTransactionRequest.class);
    }

    @SuppressWarnings("unchecked")
    private void setupExecuteInActor() {
        doAnswer(inv -> {
            inv.getArgumentAt(0, InternalCommand.class).execute(mock(ClientActorBehavior.class));
            return null;
        }).when(context).executeInActor(any(InternalCommand.class));
    }

    @Test
    public void testHandleForwardedRemoteReadRequest() throws Exception {
        final TestProbe probe = createProbe();
        final ReadTransactionRequest request =
                new ReadTransactionRequest(TRANSACTION_ID, 0L, probe.ref(), PATH_1, true);
        final Consumer<Response<?, ?>> callback = createCallbackMock();
        setupExecuteInActor();

        transaction.handleReplayedRemoteRequest(request, callback, Ticker.systemTicker().read());
        final ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(callback).accept(captor.capture());
        final Response value = captor.getValue();
        Assert.assertTrue(value instanceof ReadTransactionSuccess);
        final ReadTransactionSuccess success = (ReadTransactionSuccess) value;
        Assert.assertTrue(success.getData().isPresent());
        Assert.assertEquals(DATA_1, success.getData().get());
    }

    @Test
    public void testHandleForwardedRemoteExistsRequest() throws Exception {
        final TestProbe probe = createProbe();
        final ExistsTransactionRequest request =
                new ExistsTransactionRequest(TRANSACTION_ID, 0L, probe.ref(), PATH_1, true);
        final Consumer<Response<?, ?>> callback = createCallbackMock();
        setupExecuteInActor();

        transaction.handleReplayedRemoteRequest(request, callback, Ticker.systemTicker().read());
        final ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
        verify(callback).accept(captor.capture());
        final Response value = captor.getValue();
        Assert.assertTrue(value instanceof ExistsTransactionSuccess);
        final ExistsTransactionSuccess success = (ExistsTransactionSuccess) value;
        Assert.assertTrue(success.getExists());
    }

    @Test
    public void testHandleForwardedRemotePurgeRequest() throws Exception {
        final TestProbe probe = createProbe();
        final TransactionPurgeRequest request =
                new TransactionPurgeRequest(TRANSACTION_ID, 0L, probe.ref());
        testHandleForwardedRemoteRequest(request);
    }

    @Override
    @Test
    public void testForwardToRemoteAbort() throws Exception {
        final TestProbe probe = createProbe();
        final AbortLocalTransactionRequest request = new AbortLocalTransactionRequest(TRANSACTION_ID, probe.ref());
        final ModifyTransactionRequest modifyRequest = testForwardToRemote(request, ModifyTransactionRequest.class);
        Assert.assertTrue(modifyRequest.getPersistenceProtocol().isPresent());
        Assert.assertEquals(PersistenceProtocol.ABORT, modifyRequest.getPersistenceProtocol().get());
    }

    @Override
    @Test
    public void testForwardToRemoteCommit() throws Exception {
        final TestProbe probe = createProbe();
        final CursorAwareDataTreeModification modification = mock(CursorAwareDataTreeModification.class);
        final CommitLocalTransactionRequest request =
                new CommitLocalTransactionRequest(TRANSACTION_ID, 0L, probe.ref(), modification, null, true);
        doAnswer(this::applyToCursorAnswer).when(modification).applyToCursor(any());
        final ModifyTransactionRequest modifyRequest = testForwardToRemote(request, ModifyTransactionRequest.class);
        verify(modification).applyToCursor(any());
        Assert.assertTrue(modifyRequest.getPersistenceProtocol().isPresent());
        Assert.assertEquals(PersistenceProtocol.THREE_PHASE, modifyRequest.getPersistenceProtocol().get());
        checkModifications(modifyRequest);
    }

    @Test
    public void testForwardToLocalAbort() throws Exception {
        final TestProbe probe = createProbe();
        final AbortLocalTransactionRequest request = new AbortLocalTransactionRequest(TRANSACTION_ID, probe.ref());
        testForwardToLocal(request, AbortLocalTransactionRequest.class);
    }

    @Test
    public void testForwardToLocalPurge() throws Exception {
        final TestProbe probe = createProbe();
        final TransactionPurgeRequest request = new TransactionPurgeRequest(TRANSACTION_ID, 0L, probe.ref());
        testForwardToLocal(request, TransactionPurgeRequest.class);
    }

    protected <T extends TransactionRequest> T testForwardToLocal(final TransactionRequest toForward,
                                                                  final Class<T> expectedMessageClass) {
        final Consumer<Response<?, ?>> callback = createCallbackMock();
        final TransactionTester<LocalReadWriteProxyTransaction> transactionTester = createLocalProxy();
        final LocalReadWriteProxyTransaction successor = transactionTester.getTransaction();
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
    protected Answer applyToCursorAnswer(final InvocationOnMock invocation) {
        final DataTreeModificationCursor cursor =
                invocation.getArgumentAt(0, DataTreeModificationCursor.class);
        cursor.write(PATH_1.getLastPathArgument(), DATA_1);
        cursor.merge(PATH_2.getLastPathArgument(), DATA_2);
        cursor.delete(PATH_3.getLastPathArgument());
        return null;
    }

}