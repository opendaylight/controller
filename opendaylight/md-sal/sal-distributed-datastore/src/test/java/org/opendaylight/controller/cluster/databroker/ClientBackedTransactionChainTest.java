/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientLocalHistory;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientSnapshot;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientTransaction;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class ClientBackedTransactionChainTest {
    private ClientBackedTransactionChain chain;

    @Mock
    private ClientLocalHistory history;
    @Mock
    private ClientSnapshot snapshot;
    @Mock
    private ClientTransaction transaction;

    @Before
    public void setUp() {
        final FrontendIdentifier frontendId = FrontendIdentifier.create(
                MemberName.forName("member"), FrontendType.forName("frontend"));
        final ClientIdentifier clientId = ClientIdentifier.create(frontendId, 0);
        final LocalHistoryIdentifier historyId = new LocalHistoryIdentifier(clientId, 0);
        final TransactionIdentifier transactionId = new TransactionIdentifier(historyId, 0);

        doReturn(transactionId).when(transaction).getIdentifier();
        doReturn(transactionId).when(snapshot).getIdentifier();
        doReturn(snapshot).when(history).takeSnapshot();
        doReturn(transaction).when(history).createTransaction();

        chain = new ClientBackedTransactionChain(history, false);
    }

    @Test
    public void testNewReadOnlyTransaction() {
        assertNotNull(chain.newReadOnlyTransaction());
        verify(history).takeSnapshot();
    }

    @Test
    public void testNewReadWriteTransaction() {
        assertNotNull(chain.newReadWriteTransaction());
        verify(history).createTransaction();
    }

    @Test
    public void testNewWriteOnlyTransaction() {
        assertNotNull(chain.newWriteOnlyTransaction());
        verify(history).createTransaction();
    }

    @Test
    public void testClose() {
        chain.newReadOnlyTransaction();
        chain.close();
        verify(snapshot).abort();
        verify(history).close();
    }

    @Test
    public void testSnapshotClosed() {
        chain.snapshotClosed(snapshot);
        // snap is removed, so cannot be aborted
        chain.close();
        verify(snapshot, never()).abort();
        verify(history).close();
    }
}
