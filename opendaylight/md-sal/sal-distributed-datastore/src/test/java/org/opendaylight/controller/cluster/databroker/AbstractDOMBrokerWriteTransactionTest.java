/*
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.opendaylight.mdsal.common.api.LogicalDatastoreType.CONFIGURATION;
import static org.opendaylight.mdsal.common.api.LogicalDatastoreType.OPERATIONAL;

import com.google.common.util.concurrent.FluentFuture;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.TransactionCommitFailedException;
import org.opendaylight.mdsal.common.api.TransactionDatastoreMismatchException;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreTransactionFactory;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreWriteTransaction;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class AbstractDOMBrokerWriteTransactionTest {

    @Mock
    private DOMStoreTransactionFactory txFactory;
    @Mock
    private AbstractDOMTransactionFactory<?> abstractDOMTransactionFactory;
    @Mock
    private DOMStoreWriteTransaction domStoreWriteTransaction;

    private class AbstractDOMBrokerWriteTransactionTestImpl
            extends AbstractDOMBrokerWriteTransaction<DOMStoreWriteTransaction> {

        AbstractDOMBrokerWriteTransactionTestImpl() {
            this(Map.of(CONFIGURATION, txFactory));
        }

        AbstractDOMBrokerWriteTransactionTestImpl(Map<LogicalDatastoreType, DOMStoreTransactionFactory> txFactoryMap) {
            super(new Object(), txFactoryMap, abstractDOMTransactionFactory);
        }

        @Override
        protected DOMStoreWriteTransaction createTransaction(final LogicalDatastoreType key) {
            return domStoreWriteTransaction;
        }

        @Override
        protected DOMStoreWriteTransaction getSubtransaction() {
            return domStoreWriteTransaction;
        }
    }

    @Test
    public void readyRuntimeExceptionAndCancel() {
        RuntimeException thrown = new RuntimeException();
        doThrow(thrown).when(domStoreWriteTransaction).ready();
        AbstractDOMBrokerWriteTransactionTestImpl abstractDOMBrokerWriteTransactionTestImpl =
                new AbstractDOMBrokerWriteTransactionTestImpl();

        FluentFuture<? extends CommitInfo> submitFuture = abstractDOMBrokerWriteTransactionTestImpl.commit();
        final var cause = assertThrows(ExecutionException.class, submitFuture::get).getCause();
        assertTrue(cause instanceof TransactionCommitFailedException);
        assertSame(thrown, cause.getCause());
        abstractDOMBrokerWriteTransactionTestImpl.cancel();
    }

    @Test
    public void submitRuntimeExceptionAndCancel() throws InterruptedException {
        RuntimeException thrown = new RuntimeException();
        doThrow(thrown).when(abstractDOMTransactionFactory).commit(any(), any());
        AbstractDOMBrokerWriteTransactionTestImpl abstractDOMBrokerWriteTransactionTestImpl
                = new AbstractDOMBrokerWriteTransactionTestImpl();

        FluentFuture<? extends CommitInfo> submitFuture = abstractDOMBrokerWriteTransactionTestImpl.commit();
        final var cause = assertThrows(ExecutionException.class, submitFuture::get).getCause();
        assertTrue(cause instanceof TransactionCommitFailedException);
        assertSame(thrown, cause.getCause());
        abstractDOMBrokerWriteTransactionTestImpl.cancel();
    }

    @Test
    public void getSubtransactionStoreMismatch() {
        final var testTx = new AbstractDOMBrokerWriteTransactionTestImpl(
                Map.of(CONFIGURATION, txFactory, OPERATIONAL, txFactory));

        assertEquals(domStoreWriteTransaction, testTx.getSubtransaction(CONFIGURATION));

        final var exception = assertThrows(TransactionDatastoreMismatchException.class,
                () -> testTx.getSubtransaction(OPERATIONAL));
        assertEquals(CONFIGURATION, exception.expected());
        assertEquals(OPERATIONAL, exception.encountered());
    }

    @Test
    public void getSubtransactionStoreUndefined() {
        final var testTx = new AbstractDOMBrokerWriteTransactionTestImpl(Map.of(OPERATIONAL, txFactory));

        final var exception = assertThrows(IllegalArgumentException.class,
                () -> testTx.getSubtransaction(CONFIGURATION));
        assertEquals("CONFIGURATION is not supported", exception.getMessage());
    }
}
