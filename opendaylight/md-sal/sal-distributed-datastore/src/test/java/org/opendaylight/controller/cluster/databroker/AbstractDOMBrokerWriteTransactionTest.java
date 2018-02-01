/*
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;

import com.google.common.util.concurrent.CheckedFuture;
import java.util.Collection;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreWriteTransaction;

public class AbstractDOMBrokerWriteTransactionTest {

    @Mock
    private AbstractDOMTransactionFactory abstractDOMTransactionFactory;

    @Mock
    private DOMStoreWriteTransaction domStoreWriteTransaction;

    private class AbstractDOMBrokerWriteTransactionTestImpl
            extends AbstractDOMBrokerWriteTransaction<DOMStoreWriteTransaction> {

        AbstractDOMBrokerWriteTransactionTestImpl() {
            super(new Object(), Collections.emptyMap(), abstractDOMTransactionFactory);
        }

        @Override
        protected DOMStoreWriteTransaction createTransaction(LogicalDatastoreType key) {
            return null;
        }

        @Override
        protected Collection<DOMStoreWriteTransaction> getSubtransactions() {
            return Collections.singletonList(domStoreWriteTransaction);
        }
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void readyRuntimeExceptionAndCancel() {
        RuntimeException thrown = new RuntimeException();
        doThrow(thrown).when(domStoreWriteTransaction).ready();
        AbstractDOMBrokerWriteTransactionTestImpl abstractDOMBrokerWriteTransactionTestImpl =
                new AbstractDOMBrokerWriteTransactionTestImpl();

        CheckedFuture<Void, TransactionCommitFailedException> submitFuture =
                abstractDOMBrokerWriteTransactionTestImpl.submit();
        try {
            submitFuture.checkedGet();
            Assert.fail("TransactionCommitFailedException expected");
        } catch (TransactionCommitFailedException e) {
            assertTrue(e.getCause() == thrown);
            abstractDOMBrokerWriteTransactionTestImpl.cancel();
        }
    }

    @Test
    public void submitRuntimeExceptionAndCancel() {
        RuntimeException thrown = new RuntimeException();
        doThrow(thrown).when(abstractDOMTransactionFactory).submit(any(), any());
        AbstractDOMBrokerWriteTransactionTestImpl abstractDOMBrokerWriteTransactionTestImpl
                = new AbstractDOMBrokerWriteTransactionTestImpl();

        CheckedFuture<Void, TransactionCommitFailedException> submitFuture =
                abstractDOMBrokerWriteTransactionTestImpl.submit();
        try {
            submitFuture.checkedGet();
            Assert.fail("TransactionCommitFailedException expected");
        } catch (TransactionCommitFailedException e) {
            assertTrue(e.getCause() == thrown);
            abstractDOMBrokerWriteTransactionTestImpl.cancel();
        }
    }
}