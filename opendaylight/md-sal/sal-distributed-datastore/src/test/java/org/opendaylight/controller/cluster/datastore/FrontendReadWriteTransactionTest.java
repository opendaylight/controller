/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.apache.pekko.actor.ActorRef;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.ReadTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionDelete;
import org.opendaylight.controller.cluster.access.commands.TransactionRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionSuccess;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.access.concepts.RequestEnvelope;
import org.opendaylight.controller.cluster.access.concepts.RequestException;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeModification;

public class FrontendReadWriteTransactionTest {

    private static final ClientIdentifier CLIENT_ID = ClientIdentifier.create(FrontendIdentifier.create(
        MemberName.forName("mock"), FrontendType.forName("mock")), 0);
    private static final LocalHistoryIdentifier HISTORY_ID = new LocalHistoryIdentifier(CLIENT_ID, 0);
    private static final TransactionIdentifier TX_ID = new TransactionIdentifier(HISTORY_ID, 0);

    private AbstractFrontendHistory mockHistory;
    private ReadWriteShardDataTreeTransaction shardTransaction;
    private DataTreeModification mockModification;
    private TransactionParent mockParent;
    private FrontendReadWriteTransaction openTx;
    private CommitCohort mockCohort;

    @Before
    public void setup() {
        mockHistory = mock(AbstractFrontendHistory.class);
        mockParent = mock(TransactionParent.class);
        mockModification = mock(DataTreeModification.class);
        mockCohort = mock(CommitCohort.class);

        shardTransaction = new ReadWriteShardDataTreeTransaction(mockParent, TX_ID, mockModification);
        openTx = FrontendReadWriteTransaction.createOpen(mockHistory, shardTransaction);

        when(mockParent.finishTransaction(same(shardTransaction))).thenReturn(mockCohort);
    }

    private TransactionSuccess<?> handleRequest(final TransactionRequest<?> request) throws RequestException {
        return openTx.doHandleRequest(request, new RequestEnvelope(request, 0, 0), 0);
    }

    @Test
    public void testDuplicateModifyAbort() throws RequestException {
        final var abortReq = ModifyTransactionRequest.builder(TX_ID, mock(ActorRef.class))
            .setSequence(0)
            .setAbort()
            .build();
        assertNull(handleRequest(abortReq));
        verify(mockParent).abortTransaction(same(shardTransaction), any(Runnable.class));

        assertNull(handleRequest(abortReq));
        verifyNoMoreInteractions(mockParent);
    }

    @Test
    public void testDuplicateReady() throws RequestException {
        final var readyReq = ModifyTransactionRequest.builder(TX_ID, mock(ActorRef.class))
            .setSequence(0)
            .setReady()
            .build();

        assertNotNull(handleRequest(readyReq));
        verify(mockParent).finishTransaction(same(shardTransaction));

        assertNotNull(handleRequest(readyReq));
        verifyNoMoreInteractions(mockParent);
    }

    @Test
    public void testDuplicateDirect() throws RequestException {
        final var readyReq = ModifyTransactionRequest.builder(TX_ID, mock(ActorRef.class))
            .setSequence(0)
            .setCommit(false)
            .build();

        assertNull(handleRequest(readyReq));
        verify(mockParent).finishTransaction(same(shardTransaction));

        assertNull(handleRequest(readyReq));
        verifyNoMoreInteractions(mockParent);
    }

    @Test
    public void testDuplicateCoordinated() throws RequestException {
        final var readyReq = ModifyTransactionRequest.builder(TX_ID, mock(ActorRef.class))
            .setSequence(0)
            .setCommit(true)
            .build();

        assertNull(handleRequest(readyReq));
        verify(mockParent).finishTransaction(same(shardTransaction));

        assertNull(handleRequest(readyReq));
        verifyNoMoreInteractions(mockParent);
    }

    @Test
    public void testReadAfterReady() throws RequestException {
        final var readyReq = ModifyTransactionRequest.builder(TX_ID, mock(ActorRef.class))
            .setSequence(0)
            .setReady()
            .build();

        assertNotNull(handleRequest(readyReq));
        verify(mockParent).finishTransaction(same(shardTransaction));

        final var req = new ReadTransactionRequest(TX_ID, 0, mock(ActorRef.class), YangInstanceIdentifier.of(), true);
        final var ex = assertThrows(IllegalStateException.class, () -> handleRequest(req));
        assertEquals("mock-mock-fe-0-txn-0-0 expect to be open, is in state READY (READY)", ex.getMessage());
    }

    @Test
    public void testModifyAfterReady() throws RequestException {
        final var builder = ModifyTransactionRequest.builder(TX_ID, mock(ActorRef.class))
            .setSequence(0)
            .setReady();
        final var readyReq = builder.build();

        assertNotNull(handleRequest(readyReq));
        verify(mockParent).finishTransaction(same(shardTransaction));

        final var req = builder.setSequence(1).addModification(mock(TransactionDelete.class)).build();
        final var ex = assertThrows(IllegalStateException.class, () -> handleRequest(req));
        assertEquals("mock-mock-fe-0-txn-0-0 expect to be open, is in state READY (READY)", ex.getMessage());
    }

    @Test
    public void testReadAfterAbort() throws RequestException {
        final var abortReq = ModifyTransactionRequest.builder(TX_ID, mock(ActorRef.class))
            .setSequence(0)
            .setAbort().build();
        assertNull(handleRequest(abortReq));
        verify(mockParent).abortTransaction(same(shardTransaction), any(Runnable.class));

        final var req = new ReadTransactionRequest(TX_ID, 0, mock(ActorRef.class), YangInstanceIdentifier.of(), true);
        final var ex = assertThrows(IllegalStateException.class, () -> handleRequest(req));
        assertEquals("mock-mock-fe-0-txn-0-0 expect to be open, is in state ABORTING", ex.getMessage());
    }
}
