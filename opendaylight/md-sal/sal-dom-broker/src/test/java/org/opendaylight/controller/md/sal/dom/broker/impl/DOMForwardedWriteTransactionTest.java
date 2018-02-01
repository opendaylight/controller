/*
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.sal.dom.broker.impl;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import com.google.common.util.concurrent.CheckedFuture;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;

public class DOMForwardedWriteTransactionTest {

    @Mock
    private AbstractDOMForwardedTransactionFactory abstractDOMForwardedTransactionFactory;

    @Mock
    private DOMStoreWriteTransaction domStoreWriteTransaction;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void readyRuntimeExceptionAndCancel() {
        RuntimeException thrown = new RuntimeException();
        doThrow(thrown).when(domStoreWriteTransaction).ready();
        DOMForwardedWriteTransaction<DOMStoreWriteTransaction> domForwardedWriteTransaction =
                new DOMForwardedWriteTransaction<>(
                        new Object(),
                        Collections.singletonMap(LogicalDatastoreType.OPERATIONAL, domStoreWriteTransaction),
                        abstractDOMForwardedTransactionFactory);
        CheckedFuture<Void, TransactionCommitFailedException> submitFuture = domForwardedWriteTransaction.submit();
        try {
            submitFuture.checkedGet();
            Assert.fail("TransactionCommitFailedException expected");
        } catch (TransactionCommitFailedException e) {
            assertTrue(e.getCause() == thrown);
            domForwardedWriteTransaction.cancel();
        }
    }

    @Test
    public void submitRuntimeExceptionAndCancel() {
        RuntimeException thrown = new RuntimeException();
        doReturn(null).when(domStoreWriteTransaction).ready();
        doThrow(thrown).when(abstractDOMForwardedTransactionFactory).submit(any(), any());
        DOMForwardedWriteTransaction<DOMStoreWriteTransaction> domForwardedWriteTransaction =
                new DOMForwardedWriteTransaction<>(
                    new Object(),
                    Collections.singletonMap(LogicalDatastoreType.OPERATIONAL, domStoreWriteTransaction),
                    abstractDOMForwardedTransactionFactory);
        CheckedFuture<Void, TransactionCommitFailedException> submitFuture = domForwardedWriteTransaction.submit();
        try {
            submitFuture.checkedGet();
            Assert.fail("TransactionCommitFailedException expected");
        } catch (TransactionCommitFailedException e) {
            assertTrue(e.getCause() == thrown);
            domForwardedWriteTransaction.cancel();
        }
    }
}