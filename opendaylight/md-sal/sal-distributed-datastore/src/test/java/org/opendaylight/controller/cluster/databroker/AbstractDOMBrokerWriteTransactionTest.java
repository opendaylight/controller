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

import com.google.common.util.concurrent.FluentFuture;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.TransactionCommitFailedException;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreWriteTransaction;

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
    public void readyRuntimeExceptionAndCancel() throws InterruptedException {
        RuntimeException thrown = new RuntimeException();
        doThrow(thrown).when(domStoreWriteTransaction).ready();
        AbstractDOMBrokerWriteTransactionTestImpl abstractDOMBrokerWriteTransactionTestImpl =
                new AbstractDOMBrokerWriteTransactionTestImpl();

        FluentFuture<? extends CommitInfo> submitFuture = abstractDOMBrokerWriteTransactionTestImpl.commit();
        try {
            submitFuture.get();
            Assert.fail("TransactionCommitFailedException expected");
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof TransactionCommitFailedException);
            assertTrue(e.getCause().getCause() == thrown);
            abstractDOMBrokerWriteTransactionTestImpl.cancel();
        }
    }

    @Test
    public void submitRuntimeExceptionAndCancel() throws InterruptedException {
        RuntimeException thrown = new RuntimeException();
        doThrow(thrown).when(abstractDOMTransactionFactory).commit(any(), any());
        AbstractDOMBrokerWriteTransactionTestImpl abstractDOMBrokerWriteTransactionTestImpl
                = new AbstractDOMBrokerWriteTransactionTestImpl();

        FluentFuture<? extends CommitInfo> submitFuture = abstractDOMBrokerWriteTransactionTestImpl.commit();
        try {
            submitFuture.get();
            Assert.fail("TransactionCommitFailedException expected");
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof TransactionCommitFailedException);
            assertTrue(e.getCause().getCause() == thrown);
            abstractDOMBrokerWriteTransactionTestImpl.cancel();
        }
    }
}
