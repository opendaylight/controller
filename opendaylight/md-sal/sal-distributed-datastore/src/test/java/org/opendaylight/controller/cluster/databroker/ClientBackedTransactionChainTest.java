/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.access.concepts.ClientIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendIdentifier;
import org.opendaylight.controller.cluster.access.concepts.FrontendType;
import org.opendaylight.controller.cluster.access.concepts.LocalHistoryIdentifier;
import org.opendaylight.controller.cluster.access.concepts.MemberName;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientLocalHistory;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientSnapshot;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientTransaction;

public class ClientBackedTransactionChainTest {
    private ClientBackedTransactionChain chain;

    @Mock
    private ClientLocalHistory history;
    @Mock
    private ClientSnapshot snapshot;
    @Mock
    private ClientTransaction transaction;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        final FrontendIdentifier frontendId = FrontendIdentifier.create(
                MemberName.forName("member"), FrontendType.forName("frontend"));
        final ClientIdentifier clientId = ClientIdentifier.create(frontendId, 0);
        final LocalHistoryIdentifier historyId = new LocalHistoryIdentifier(clientId, 0);
        final TransactionIdentifier transactionId = new TransactionIdentifier(historyId, 0);

        Mockito.when(history.getIdentifier()).thenReturn(historyId);
        Mockito.when(transaction.getIdentifier()).thenReturn(transactionId);
        Mockito.when(snapshot.getIdentifier()).thenReturn(transactionId);
        Mockito.when(history.takeSnapshot()).thenReturn(snapshot);
        Mockito.when(history.createTransaction()).thenReturn(transaction);

        chain = new ClientBackedTransactionChain(history, false);
    }

    @Test
    public void testNewReadOnlyTransaction() throws Exception {
        Assert.assertNotNull(chain.newReadOnlyTransaction());
        Mockito.verify(history).takeSnapshot();
    }

    @Test
    public void testNewReadWriteTransaction() throws Exception {
        Assert.assertNotNull(chain.newReadWriteTransaction());
        Mockito.verify(history).createTransaction();
    }

    @Test
    public void testNewWriteOnlyTransaction() throws Exception {
        Assert.assertNotNull(chain.newWriteOnlyTransaction());
        Mockito.verify(history).createTransaction();
    }

    @Test
    public void testClose() throws Exception {
        chain.newReadOnlyTransaction();
        chain.close();
        Mockito.verify(snapshot).abort();
        Mockito.verify(history).close();
    }

    @Test
    public void testSnapshotClosed() throws Exception {
        chain.snapshotClosed(snapshot);
        // snap is removed, so cannot be aborted
        chain.close();
        Mockito.verify(snapshot, Mockito.never()).abort();
        Mockito.verify(history).close();
    }
}