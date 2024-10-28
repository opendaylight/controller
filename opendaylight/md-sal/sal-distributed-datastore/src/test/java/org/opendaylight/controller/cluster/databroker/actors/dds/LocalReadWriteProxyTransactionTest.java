/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.actors.dds;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.assertFutureEquals;
import static org.opendaylight.controller.cluster.databroker.actors.dds.TestUtils.assertOperationThrowsException;

import com.google.common.base.Ticker;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.Test;
import org.mockito.Mock;
import org.opendaylight.controller.cluster.access.commands.AbortLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.CommitLocalTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionAbortRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionCanCommitSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionCommitSuccess;
import org.opendaylight.controller.cluster.access.commands.TransactionDoCommitRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionPreCommitRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionPreCommitSuccess;
import org.opendaylight.controller.cluster.access.concepts.Response;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.data.tree.api.CursorAwareDataTreeModification;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeModification;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeSnapshot;

public class LocalReadWriteProxyTransactionTest extends LocalProxyTransactionTest<LocalReadWriteProxyTransaction> {
    @Mock
    private CursorAwareDataTreeModification modification;

    @Override
    protected LocalReadWriteProxyTransaction createTransaction(final ProxyHistory parent,
                                                               final TransactionIdentifier id,
                                                               final DataTreeSnapshot snapshot) {
        when(snapshot.newModification()).thenReturn(modification);
        when(modification.readNode(PATH_1)).thenReturn(Optional.of(DATA_1));
        when(modification.readNode(PATH_3)).thenReturn(Optional.empty());
        return new LocalReadWriteProxyTransaction(parent, TestUtils.TRANSACTION_ID, snapshot);
    }

    @Test
    public void testIsSnapshotOnly() {
        assertFalse(transaction.isSnapshotOnly());
    }

    @Test
    public void testReadOnlyView() {
        assertEquals(modification, transaction.readOnlyView());
    }

    @Test
    @Override
    public void testDelete() {
        transaction.delete(PATH_1);
        verify(modification).delete(PATH_1);
    }

    @Test
    @Override
    public void testDirectCommit() throws Exception {
        transaction.seal();
        final var result = transaction.directCommit();
        final var tester = getTester();
        final var req = tester.expectTransactionRequest(CommitLocalTransactionRequest.class);
        tester.replySuccess(new TransactionCommitSuccess(TRANSACTION_ID, req.getSequence()));
        assertFutureEquals(Boolean.TRUE, result);
    }

    @Test
    @Override
    public void testCanCommit() {
        testRequestResponse(transaction::canCommit, CommitLocalTransactionRequest.class,
                TransactionCanCommitSuccess::new);
    }

    @Test
    @Override
    public void testPreCommit() {
        testRequestResponse(transaction::preCommit, TransactionPreCommitRequest.class,
                TransactionPreCommitSuccess::new);
    }

    @Test
    @Override
    public void testDoCommit() {
        testRequestResponse(transaction::doCommit, TransactionDoCommitRequest.class, TransactionCommitSuccess::new);
    }

    @Test
    @Override
    public void testMerge() {
        transaction.merge(PATH_1, DATA_1);
        verify(modification).merge(PATH_1, DATA_1);
    }

    @Test
    @Override
    public void testWrite() {
        transaction.write(PATH_1, DATA_1);
        verify(modification).write(PATH_1, DATA_1);
    }

    @Test
    public void testCommitRequest() {
        transaction.doWrite(PATH_1, DATA_1);
        final var request = transaction.commitRequest(true);
        assertTrue(request.isCoordinated());
        assertEquals(modification, request.getModification());
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
        assertEquals(modification, transaction.getSnapshot());
    }

    @Test
    public void testFlushState() {
        final var transactionTester = createRemoteProxyTransactionTester();
        final var successor = transactionTester.getTransaction();
        doAnswer(LocalProxyTransactionTest::applyToCursorAnswer).when(modification).applyToCursor(any());
        transaction.sealOnly();
        final var request = transaction.flushState().orElseThrow();
        transaction.forwardToSuccessor(successor, request, null);
        verify(modification).applyToCursor(any());
        transactionTester.getTransaction().seal();
        transactionTester.getTransaction().directCommit();
        final var modifyRequest = transactionTester.expectTransactionRequest(ModifyTransactionRequest.class);
        checkModifications(modifyRequest);
    }

    @Test
    public void testApplyModifyTransactionRequestCoordinated() {
        applyModifyTransactionRequest(true);
    }

    @Test
    public void testApplyModifyTransactionRequestSimple() {
        applyModifyTransactionRequest(false);
    }

    @Test
    public void testApplyModifyTransactionRequestAbort() {
        final var probe = createProbe();
        final var request = ModifyTransactionRequest.builder(TRANSACTION_ID, probe.ref())
            .setSequence(0L)
            .setAbort()
            .build();
        final Consumer<Response<?, ?>> callback = createCallbackMock();
        transaction.replayModifyTransactionRequest(request, callback, Ticker.systemTicker().read());
        getTester().expectTransactionRequest(AbortLocalTransactionRequest.class);
    }

    @Test
    public void testHandleForwardedRemotePreCommitRequest() {
        final var probe = createProbe();
        testHandleForwardedRemoteRequest(new TransactionPreCommitRequest(TRANSACTION_ID, 0L, probe.ref()));
    }

    @Test
    public void testHandleForwardedRemoteDoCommitRequest() {
        final var probe = createProbe();
        testHandleForwardedRemoteRequest(new TransactionDoCommitRequest(TRANSACTION_ID, 0L, probe.ref()));
    }

    @Test
    public void testHandleForwardedRemoteAbortRequest() {
        final var probe = createProbe();
        testHandleForwardedRemoteRequest(new TransactionAbortRequest(TRANSACTION_ID, 0L, probe.ref()));
    }

    @Test
    public void testForwardToLocalCommit() {
        final var probe = createProbe();
        final var mod = mock(DataTreeModification.class);
        final var request = new CommitLocalTransactionRequest(TRANSACTION_ID, 0L, probe.ref(), mod, null, false);
        testForwardToLocal(request, CommitLocalTransactionRequest.class);
    }

    @Test
    public void testSendAbort() throws Exception {
        final var probe = createProbe();
        transaction.sendAbort(new AbortLocalTransactionRequest(TRANSACTION_ID, probe.ref()), createCallbackMock());
        assertOperationThrowsException(() -> transaction.delete(PATH_1), IllegalStateException.class);
    }

    private void applyModifyTransactionRequest(final boolean coordinated) {
        final var probe = createProbe();
        final var request = ModifyTransactionRequest.builder(TRANSACTION_ID, probe.ref())
            .addWrite(PATH_1, DATA_1)
            .addMerge(PATH_2, DATA_2)
            .addDelete(PATH_3)
            .setSequence(0L)
            .setCommit(coordinated)
            .build();
        final Consumer<Response<?, ?>> callback = createCallbackMock();
        transaction.replayModifyTransactionRequest(request, callback, Ticker.systemTicker().read());
        verify(modification).write(PATH_1, DATA_1);
        verify(modification).merge(PATH_2, DATA_2);
        verify(modification).delete(PATH_3);
        final var commitRequest = getTester().expectTransactionRequest(CommitLocalTransactionRequest.class);
        assertEquals(modification, commitRequest.getModification());
        assertEquals(coordinated, commitRequest.isCoordinated());
    }

}
