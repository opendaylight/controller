/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import akka.actor.ActorRef;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.cluster.access.commands.ModifyTransactionRequestBuilder;
import org.opendaylight.controller.cluster.access.commands.ReadTransactionRequest;
import org.opendaylight.controller.cluster.access.commands.TransactionModification;
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
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;

public class FrontendReadWriteTransactionTest {

    private static final ClientIdentifier CLIENT_ID = ClientIdentifier.create(FrontendIdentifier.create(
        MemberName.forName("mock"), FrontendType.forName("mock")), 0);
    private static final LocalHistoryIdentifier HISTORY_ID = new LocalHistoryIdentifier(CLIENT_ID, 0);
    private static final TransactionIdentifier TX_ID = new TransactionIdentifier(HISTORY_ID, 0);

    private AbstractFrontendHistory mockHistory;
    private ReadWriteShardDataTreeTransaction shardTransaction;
    private DataTreeModification mockModification;
    private ShardDataTreeTransactionParent mockParent;
    private FrontendReadWriteTransaction openTx;
    private ShardDataTreeCohort mockCohort;

    @Before
    public void setup() {
        mockHistory = mock(AbstractFrontendHistory.class);
        mockParent = mock(ShardDataTreeTransactionParent.class);
        mockModification = mock(DataTreeModification.class);
        mockCohort = mock(ShardDataTreeCohort.class);

        shardTransaction = new ReadWriteShardDataTreeTransaction(mockParent, TX_ID, mockModification);
        openTx = FrontendReadWriteTransaction.createOpen(mockHistory, shardTransaction);

        when(mockParent.finishTransaction(same(shardTransaction))).thenReturn(mockCohort);
    }

    private TransactionSuccess<?> handleRequest(final TransactionRequest<?> request) throws RequestException {
        return openTx.doHandleRequest(request, new RequestEnvelope(request, 0, 0), 0);
    }

    @Test
    public void testDuplicateModifyAbort() throws RequestException {
        final ModifyTransactionRequestBuilder b = new ModifyTransactionRequestBuilder(TX_ID, mock(ActorRef.class));
        b.setSequence(0);
        b.setAbort();
        final TransactionRequest<?> abortReq = b.build();
        assertNull(handleRequest(abortReq));
        verify(mockParent).abortTransaction(same(shardTransaction), any(Runnable.class));

        assertNull(handleRequest(abortReq));
        verifyNoMoreInteractions(mockParent);
    }

    @Test
    public void testDuplicateReady() throws RequestException {
        final ModifyTransactionRequestBuilder b = new ModifyTransactionRequestBuilder(TX_ID, mock(ActorRef.class));
        b.setSequence(0);
        b.setReady();
        final TransactionRequest<?> readyReq = b.build();

        assertNotNull(handleRequest(readyReq));
        verify(mockParent).finishTransaction(same(shardTransaction));

        assertNotNull(handleRequest(readyReq));
        verifyNoMoreInteractions(mockParent);
    }

    @Test
    public void testDuplicateDirect() throws RequestException {
        final ModifyTransactionRequestBuilder b = new ModifyTransactionRequestBuilder(TX_ID, mock(ActorRef.class));
        b.setSequence(0);
        b.setCommit(false);
        final TransactionRequest<?> readyReq = b.build();

        assertNull(handleRequest(readyReq));
        verify(mockParent).finishTransaction(same(shardTransaction));

        assertNull(handleRequest(readyReq));
        verifyNoMoreInteractions(mockParent);
    }

    @Test
    public void testDuplicateCoordinated() throws RequestException {
        final ModifyTransactionRequestBuilder b = new ModifyTransactionRequestBuilder(TX_ID, mock(ActorRef.class));
        b.setSequence(0);
        b.setCommit(true);
        final TransactionRequest<?> readyReq = b.build();

        assertNull(handleRequest(readyReq));
        verify(mockParent).finishTransaction(same(shardTransaction));

        assertNull(handleRequest(readyReq));
        verifyNoMoreInteractions(mockParent);
    }

    @Test(expected = IllegalStateException.class)
    public void testReadAfterReady() throws RequestException {
        final ModifyTransactionRequestBuilder b = new ModifyTransactionRequestBuilder(TX_ID, mock(ActorRef.class));
        b.setSequence(0);
        b.setReady();
        final TransactionRequest<?> readyReq = b.build();

        assertNotNull(handleRequest(readyReq));
        verify(mockParent).finishTransaction(same(shardTransaction));

        handleRequest(new ReadTransactionRequest(TX_ID, 0, mock(ActorRef.class), YangInstanceIdentifier.EMPTY, true));
    }

    @Test(expected = IllegalStateException.class)
    public void testModifyAfterReady() throws RequestException {
        final ModifyTransactionRequestBuilder b = new ModifyTransactionRequestBuilder(TX_ID, mock(ActorRef.class));
        b.setSequence(0);
        b.setReady();
        final TransactionRequest<?> readyReq = b.build();

        assertNotNull(handleRequest(readyReq));
        verify(mockParent).finishTransaction(same(shardTransaction));

        b.setSequence(1);
        b.addModification(mock(TransactionModification.class));
        handleRequest(b.build());
    }

    @Test(expected = IllegalStateException.class)
    public void testReadAfterAbort() throws RequestException {
        final ModifyTransactionRequestBuilder b = new ModifyTransactionRequestBuilder(TX_ID, mock(ActorRef.class));
        b.setSequence(0);
        b.setAbort();
        final TransactionRequest<?> abortReq = b.build();
        assertNull(handleRequest(abortReq));
        verify(mockParent).abortTransaction(same(shardTransaction), any(Runnable.class));

        handleRequest(new ReadTransactionRequest(TX_ID, 0, mock(ActorRef.class), YangInstanceIdentifier.EMPTY, true));
    }
}
