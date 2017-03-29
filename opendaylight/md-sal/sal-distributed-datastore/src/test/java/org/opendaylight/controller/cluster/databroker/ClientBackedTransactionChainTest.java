/*
 * Copyright (c) 2017 Pantheon Technologies s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker;

import java.lang.reflect.Field;
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
import org.opendaylight.controller.cluster.databroker.actors.dds.AbstractClientHandle;
import org.opendaylight.controller.cluster.databroker.actors.dds.AbstractClientHistory;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientLocalHistory;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientSnapshot;
import org.opendaylight.controller.cluster.databroker.actors.dds.ClientTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreReadWriteTransaction;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;

public class ClientBackedTransactionChainTest {

    private ClientBackedTransactionChain chain;

    @Mock
    private ClientLocalHistory history;
    @Mock
    private ClientSnapshot clientTransaction;
    @Mock
    private ClientTransaction transaction;
    @Mock
    private ClientSnapshot clientSnapshot;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        final FrontendIdentifier frontendId = FrontendIdentifier.create(
                MemberName.forName("member"), FrontendType.forName("frontend"));
        final ClientIdentifier clientId = ClientIdentifier.create(frontendId, 0);
        final LocalHistoryIdentifier identifierValue = new LocalHistoryIdentifier(clientId, 0);

        final Field identifierField = AbstractClientHistory.class.getDeclaredField("identifier");
        identifierField.setAccessible(true);
        identifierField.set(history, identifierValue);

        Mockito.when(history.createTransaction()).thenReturn(transaction);
        final TransactionIdentifier transactionId = new TransactionIdentifier(identifierValue, 0);
        final Field transactionIdField = AbstractClientHandle.class.getDeclaredField("transactionId");
        transactionIdField.setAccessible(true);
        transactionIdField.set(transaction, transactionId);

        //Mockito.when(history.takeSnapshot()).thenReturn(clientSnapshot);
        //Mockito.doReturn(clientSnapshot).when(history).takeSnapshot();

        chain = new ClientBackedTransactionChain(history);
    }

    @Test
    public void newReadOnlyTransaction() throws Exception {
        final DOMStoreReadTransaction tx = chain.newReadOnlyTransaction();
        Assert.assertNotNull(tx);
    }

    @Test
    public void newReadWriteTransaction() throws Exception {
        final DOMStoreReadWriteTransaction tx = chain.newReadWriteTransaction();
        Assert.assertNotNull(tx);
    }

    @Test
    public void newWriteOnlyTransaction() throws Exception {
        final DOMStoreWriteTransaction tx = chain.newWriteOnlyTransaction();
        Assert.assertNotNull(tx);
    }

    @Test
    public void close() throws Exception {
        chain.close();
    }

    @Test
    public void snapshotClosed() throws Exception {
        chain.snapshotClosed(clientTransaction);
    }
}