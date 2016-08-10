/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.md.sal.dom.broker.impl.ShardedDOMAdapter;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public class ShardedDOMDataBrokerDelegatingTransactionChainTest {

    @Mock
    private SchemaContext ctx;

    @Mock
    private DOMDataBroker dataBroker;

    @Mock
    private DOMTransactionChain delegateTxChain;

    @Mock
    private TransactionChainListener txChainlistener;

    private ShardedDOMDataBrokerDelegatingTransactionChain txChain;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn(delegateTxChain).when(dataBroker).createTransactionChain(any());
        txChain = new ShardedDOMDataBrokerDelegatingTransactionChain("1", ctx, dataBroker, txChainlistener);
    }

    @Test
    public void testClose() {
        doNothing().when(delegateTxChain).close();
        txChain.close();
        verify(delegateTxChain).close();
    }

    @Test
    public void testNewWriteTransaction() {
        doReturn(mock(DOMDataTreeWriteTransaction.class)).when(delegateTxChain).newWriteOnlyTransaction();
        txChain.newWriteOnlyTransaction();
        verify(delegateTxChain).newWriteOnlyTransaction();
    }

    @Test
    public void testNewReadOnlyTransaction() {
        doReturn(mock(DOMDataTreeReadTransaction.class)).when(delegateTxChain).newReadOnlyTransaction();
        txChain.newReadOnlyTransaction();
        verify(delegateTxChain).newReadOnlyTransaction();
    }


    @Test
    public void testNewReadWriteTransaction() {
        doReturn(mock(DOMDataTreeReadTransaction.class)).when(delegateTxChain).newReadOnlyTransaction();
        doReturn(mock(DOMDataTreeWriteTransaction.class)).when(delegateTxChain).newWriteOnlyTransaction();

        txChain.newReadWriteTransaction();
        verify(delegateTxChain).newReadOnlyTransaction();
        verify(delegateTxChain).newWriteOnlyTransaction();
    }

    @Test
    public void testTransactionChainFailed() {
        final DOMDataTreeWriteTransaction writeTxDelegate = mock(DOMDataTreeWriteTransaction.class);
        doReturn("DELEGATE-WRITE-TX-1").when(writeTxDelegate).getIdentifier();
        doReturn(writeTxDelegate).when(delegateTxChain).newWriteOnlyTransaction();
        doNothing().when(txChainlistener).onTransactionChainFailed(any(), any(), any());

        // verify writetx fail
        txChain.newWriteOnlyTransaction();
        txChain.onTransactionChainFailed(delegateTxChain, writeTxDelegate, new Throwable("Fail"));

        final ArgumentCaptor<AsyncTransaction> txCaptor = ArgumentCaptor.forClass(AsyncTransaction.class);
        final ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(txChainlistener)
                .onTransactionChainFailed(eq(txChain), txCaptor.capture(), throwableCaptor.capture());
        assertEquals("DOM-CHAIN-1-0", txCaptor.getValue().getIdentifier());
        assertEquals("Fail", throwableCaptor.getValue().getMessage());

        // verify readtx fail
        final DOMDataTreeReadTransaction readTxDelegate = mock(DOMDataTreeReadTransaction.class);
        doReturn("DELEGATE-READ-TX-1").when(readTxDelegate).getIdentifier();
        doReturn(readTxDelegate).when(delegateTxChain).newReadOnlyTransaction();
        doNothing().when(txChainlistener).onTransactionChainFailed(any(), any(), any());
        txChain.newReadOnlyTransaction();
        txChain.onTransactionChainFailed(delegateTxChain, readTxDelegate, new Throwable("Fail"));
        verify(txChainlistener, times(2))
                .onTransactionChainFailed(eq(txChain), txCaptor.capture(), throwableCaptor.capture());
        assertEquals("DOM-CHAIN-1-1", txCaptor.getValue().getIdentifier());
        assertEquals("Fail", throwableCaptor.getValue().getMessage());


        // verify readwritetx fail, we must check both read and write failure
        // translates to returned readwritetx

        // we can reuse write and read tx delegates, just return different
        // identifiers to avoid conflicts in keys in tx dictionary
        doReturn("DELEGATE-WRITE-RWTX-1").when(writeTxDelegate).getIdentifier();
        doReturn("DELEGATE-READ-RWTX-1").when(readTxDelegate).getIdentifier();
        txChain.newReadWriteTransaction();
        txChain.onTransactionChainFailed(delegateTxChain, writeTxDelegate, new Throwable("Fail"));
        verify(txChainlistener, times(3))
                .onTransactionChainFailed(eq(txChain), txCaptor.capture(), throwableCaptor.capture());
        assertEquals("DOM-CHAIN-1-2", txCaptor.getValue().getIdentifier());
        assertEquals("Fail", throwableCaptor.getValue().getMessage());

        txChain.onTransactionChainFailed(delegateTxChain, readTxDelegate, new Throwable("Fail"));
        verify(txChainlistener, times(4))
                .onTransactionChainFailed(eq(txChain), txCaptor.capture(), throwableCaptor.capture());
        assertEquals("DOM-CHAIN-1-2", txCaptor.getValue().getIdentifier());
        assertEquals("Fail", throwableCaptor.getValue().getMessage());
    }

    @Test
    public void testTransactionChainSuccessful() {
        doNothing().when(txChainlistener).onTransactionChainSuccessful(any());
        txChain.onTransactionChainSuccessful(delegateTxChain);
        verify(txChainlistener).onTransactionChainSuccessful(eq(txChain));
    }
}