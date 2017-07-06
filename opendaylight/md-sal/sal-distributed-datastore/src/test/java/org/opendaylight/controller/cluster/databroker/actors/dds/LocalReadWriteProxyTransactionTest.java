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
import static org.mockito.Mockito.when;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.assertFutureEquals;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.assertOperationThrowsException;

import akka.testkit.TestProbe;
import com.google.common.base.Ticker;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.function.Consumer;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.controller.cluster.access.commands.AbortLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.CommitLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequestBuilder;
import org.opendaylight.controller.cluster.access.commands.TransactionAbortRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionCanCommitSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionCommitSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionDelete;
import org.opendaylight.controller.cluster.access.commands.TransactionDoCommitRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionMerge;
import org.opendaylight.controller.cluster.access.commands.TransactionModification;
import org.opendaylight.controller.cluster.access.commands.TransactionPreCommitRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionPreCommitSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionWrite;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.CursorAwareDataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeSnapshot;

public class LocalReadWriteProxyTransactionTest extends LocalProxyTransactionTest<LocalReadWriteProxyTransaction> {
    @Mock
    private CursorAwareDataTreeModification modification;

    @Override
    protected LocalReadWriteProxyTransaction createTransaction(final ProxyHistory parent,
                                                               final TransactionIdentifier id,
                                                               final DataTreeSnapshot snapshot) {
        when(snapshot.newModification()).thenReturn(modification);
        when(modification.readNode(PATH_1)).thenReturn(com.google.common.base.Optional.of(DATA_1));
        when(modification.readNode(PATH_3)).thenReturn(com.google.common.base.Optional.absent());
        return new LocalReadWriteProxyTransaction(parent, TestUtils.TRANSACTION_ID, snapshot);
    }

    @Test
    public void testIsSnapshotOnly() throws Exception {
        Assert.assertFalse(transaction.isSnapshotOnly());
    }

    @Test
    public void testReadOnlyView() throws Exception {
        Assert.assertEquals(modification, transaction.readOnlyView());
    }

    @Test
    @Override
    public void testDelete() throws Exception {
        transaction.delete(PATH_1);
        verify(modification).delete(PATH_1);
    }

    @Test
    @Override
    public void testDirectCommit() throws Exception {
        transaction.seal();
        final ListenableFuture<Boolean> result = transaction.directCommit();
        final TransactionTester<LocalReadWriteProxyTransaction> tester = getTester();
        final CommitLocalTransactionRequest req = tester.expectTransactionRequest(CommitLocalTransactionRequest.class);
        tester.replySuccess(new TransactionCommitSuccess(TRANSACTION_ID, req.getSequence()));
        assertFutureEquals(true, result);
    }

    @Test
    @Override
    public void testCanCommit() throws Exception {
        testRequestResponse(transaction::canCommit, CommitLocalTransactionRequest.class,
                TransactionCanCommitSuccess::new);
    }

    @Test
    @Override
    public void testPreCommit() throws Exception {
        testRequestResponse(transaction::preCommit, TransactionPreCommitRequest.class,
                TransactionPreCommitSuccess::new);
    }

    @Test
    @Override
    public void testDoCommit() throws Exception {
        testRequestResponse(transaction::doCommit, TransactionDoCommitRequest.class, TransactionCommitSuccess::new);
    }

    @Test
    @Override
    public void testMerge() throws Exception {
        transaction.merge(PATH_1, DATA_1);
        verify(modification).merge(PATH_1, DATA_1);
    }

    @Test
    @Override
    public void testWrite() throws Exception {
        transaction.write(PATH_1, DATA_1);
        verify(modification).write(PATH_1, DATA_1);
    }

    @Test
    public void testCommitRequest() throws Exception {
        transaction.doWrite(PATH_1, DATA_1);
        final boolean coordinated = true;
        final CommitLocalTransactionRequest request = transaction.commitRequest(coordinated);
        Assert.assertEquals(coordinated, request.isCoordinated());
        Assert.assertEquals(modification, request.getModification());
    }

    @Test
    public void testModifyAfterCommitRequest() throws Exception {
        transaction.doWrite(PATH_1, DATA_1);
        final boolean coordinated = true;
        transaction.commitRequest(coordinated);
        assertOperationThrowsException(() -> transaction.doMerge(PATH_1, DATA_1), IllegalStateException.class);
    }

    @Test
    public void testSealOnly() throws Exception {
        assertOperationThrowsException(() -> transaction.getSnapshot(), IllegalStateException.class);
        transaction.sealOnly();
        Assert.assertEquals(modification, transaction.getSnapshot());
    }

    @Test
    public void testFlushState() throws Exception {
        final TransactionTester<RemoteProxyTransaction> transactionTester = createRemoteProxyTransactionTester();
        final RemoteProxyTransaction successor = transactionTester.getTransaction();
        doAnswer(this::applyToCursorAnswer).when(modification).applyToCursor(any());
        transaction.sealOnly();
        final TransactionRequest<?> request = transaction.flushState().get();
        transaction.forwardToSuccessor(successor, request, null);
        verify(modification).applyToCursor(any());
        transactionTester.getTransaction().seal();
        transactionTester.getTransaction().directCommit();
        final ModifyTransactionRequest modifyRequest =
                transactionTester.expectTransactionRequest(ModifyTransactionRequest.class);
        checkModifications(modifyRequest);
    }

    @Test
    public void testApplyModifyTransactionRequestCoordinated() throws Exception {
        applyModifyTransactionRequest(true);
    }

    @Test
    public void testApplyModifyTransactionRequestSimple() throws Exception {
        applyModifyTransactionRequest(false);
    }

    @Test
    public void testApplyModifyTransactionRequestAbort() throws Exception {
        final TestProbe probe = createProbe();
        final ModifyTransactionRequestBuilder builder =
                new ModifyTransactionRequestBuilder(TRANSACTION_ID, probe.ref());
        builder.setSequence(0L);
        builder.setAbort();
        final ModifyTransactionRequest request = builder.build();
        final Consumer<Response<?, ?>> callback = createCallbackMock();
        transaction.replayModifyTransactionRequest(request, callback, Ticker.systemTicker().read());
        getTester().expectTransactionRequest(AbortLocalTransactionRequest.class);
    }

    @Test
    public void testHandleForwardedRemotePreCommitRequest() throws Exception {
        final TestProbe probe = createProbe();
        final TransactionPreCommitRequest request =
                new TransactionPreCommitRequest(TRANSACTION_ID, 0L, probe.ref());
        testHandleForwardedRemoteRequest(request);
    }

    @Test
    public void testHandleForwardedRemoteDoCommitRequest() throws Exception {
        final TestProbe probe = createProbe();
        final TransactionDoCommitRequest request =
                new TransactionDoCommitRequest(TRANSACTION_ID, 0L, probe.ref());
        testHandleForwardedRemoteRequest(request);
    }

    @Test
    public void testHandleForwardedRemoteAbortRequest() throws Exception {
        final TestProbe probe = createProbe();
        final TransactionAbortRequest request =
                new TransactionAbortRequest(TRANSACTION_ID, 0L, probe.ref());
        testHandleForwardedRemoteRequest(request);
    }

    @Test
    public void testForwardToLocalCommit() throws Exception {
        final TestProbe probe = createProbe();
        final DataTreeModification mod = mock(DataTreeModification.class);
        final TransactionRequest<?> request =
                new CommitLocalTransactionRequest(TRANSACTION_ID, 0L, probe.ref(), mod, null, false);
        testForwardToLocal(request, CommitLocalTransactionRequest.class);
    }

    @Test
    public void testSendAbort() throws Exception {
        final TestProbe probe = createProbe();
        final TransactionRequest<?> request = new AbortLocalTransactionRequest(TRANSACTION_ID, probe.ref());
        transaction.sendAbort(request, createCallbackMock());
        assertOperationThrowsException(() -> transaction.delete(PATH_1), IllegalStateException.class);
    }

    private void applyModifyTransactionRequest(final boolean coordinated) {
        final TestProbe probe = createProbe();
        final ModifyTransactionRequestBuilder builder =
                new ModifyTransactionRequestBuilder(TRANSACTION_ID, probe.ref());
        final TransactionModification write = new TransactionWrite(PATH_1, DATA_1);
        final TransactionModification merge = new TransactionMerge(PATH_2, DATA_2);
        final TransactionModification delete = new TransactionDelete(PATH_3);
        builder.addModification(write);
        builder.addModification(merge);
        builder.addModification(delete);
        builder.setSequence(0L);
        builder.setCommit(coordinated);
        final ModifyTransactionRequest request = builder.build();
        final Consumer<Response<?, ?>> callback = createCallbackMock();
        transaction.replayModifyTransactionRequest(request, callback, Ticker.systemTicker().read());
        verify(modification).write(PATH_1, DATA_1);
        verify(modification).merge(PATH_2, DATA_2);
        verify(modification).delete(PATH_3);
        final CommitLocalTransactionRequest commitRequest =
                getTester().expectTransactionRequest(CommitLocalTransactionRequest.class);
        Assert.assertEquals(modification, commitRequest.getModification());
        Assert.assertEquals(coordinated, commitRequest.isCoordinated());
    }

}