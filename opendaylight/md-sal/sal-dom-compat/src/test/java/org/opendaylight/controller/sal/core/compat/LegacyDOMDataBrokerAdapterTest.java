/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sal.core.compat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.DataStoreUnavailableException;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.OptimisticLockFailedException;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.ClusteredDOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.mdsal.dom.api.DOMDataBrokerExtension;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohort;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohortRegistration;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohortRegistry;
import org.opendaylight.mdsal.dom.broker.SerializedDOMDataBroker;
import org.opendaylight.mdsal.dom.spi.store.DOMStore;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadWriteTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreTransactionChain;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreTreeChangePublisher;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ConflictingModificationAppliedException;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;

/**
 * Unit tests for LegacyDOMDataBrokerAdapter.
 *
 * @author Thomas Pantelis
 */
public class LegacyDOMDataBrokerAdapterTest {
    public static final QName TEST_QNAME = QName.create("test", "2018-07-11", "test");
    private static final YangInstanceIdentifier TEST_PATH = YangInstanceIdentifier.of(TEST_QNAME);

    @Mock
    private TestDOMStore mockOperStore;

    @Mock
    private TestDOMStore mockConfigStore;

    @Mock
    private DOMStoreReadTransaction mockConfigReadTx;

    @Mock
    private DOMStoreWriteTransaction mockConfigWriteTx;

    @Mock
    private DOMStoreReadWriteTransaction mockConfigReadWriteTx;

    @Mock
    private DOMStoreThreePhaseCommitCohort mockConfigCommitCohort;

    @Mock
    private DOMStoreReadTransaction mockOperReadTx;

    @Mock
    private DOMStoreWriteTransaction mockOperWriteTx;

    @Mock
    private DOMStoreReadWriteTransaction mockOperReadWriteTx;

    @Mock
    private DOMStoreThreePhaseCommitCohort mockOperCommitCohort;

    @Mock
    private DOMDataTreeCommitCohortRegistry mockCommitCohortRegistry;

    private LegacyDOMDataBrokerAdapter adapter;
    private NormalizedNode<?,?> dataNode;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        SerializedDOMDataBroker backendBroker = new SerializedDOMDataBroker(ImmutableMap.of(
                org.opendaylight.mdsal.common.api.LogicalDatastoreType.OPERATIONAL, mockOperStore,
                org.opendaylight.mdsal.common.api.LogicalDatastoreType.CONFIGURATION, mockConfigStore),
                MoreExecutors.newDirectExecutorService()) {
            @Override
            public ClassToInstanceMap<DOMDataBrokerExtension> getExtensions() {
                return ImmutableClassToInstanceMap.<DOMDataBrokerExtension>builder().putAll(super.getExtensions())
                        .put(DOMDataTreeCommitCohortRegistry.class, mockCommitCohortRegistry).build();
            }
        };

        adapter = new LegacyDOMDataBrokerAdapter(backendBroker);

        doReturn(Futures.immediateFuture(Boolean.TRUE)).when(mockConfigCommitCohort).canCommit();
        doReturn(Futures.immediateFuture(null)).when(mockConfigCommitCohort).preCommit();
        doReturn(Futures.immediateFuture(null)).when(mockConfigCommitCohort).commit();
        doReturn(Futures.immediateFuture(null)).when(mockConfigCommitCohort).abort();

        dataNode = ImmutableNodes.containerNode(TEST_QNAME);

        doReturn(mockConfigWriteTx).when(mockConfigStore).newWriteOnlyTransaction();
        doNothing().when(mockConfigWriteTx).write(TEST_PATH, dataNode);
        doNothing().when(mockConfigWriteTx).merge(TEST_PATH, dataNode);
        doNothing().when(mockConfigWriteTx).delete(TEST_PATH);
        doNothing().when(mockConfigWriteTx).close();
        doReturn(mockConfigCommitCohort).when(mockConfigWriteTx).ready();

        doReturn(mockConfigReadTx).when(mockConfigStore).newReadOnlyTransaction();
        doReturn(FluentFutures.immediateFluentFuture(Optional.of(dataNode))).when(mockConfigReadTx).read(TEST_PATH);
        doReturn(FluentFutures.immediateFluentFuture(Boolean.TRUE)).when(mockConfigReadTx).exists(TEST_PATH);

        doReturn(mockConfigReadWriteTx).when(mockConfigStore).newReadWriteTransaction();
        doNothing().when(mockConfigReadWriteTx).write(TEST_PATH, dataNode);
        doReturn(mockConfigCommitCohort).when(mockConfigReadWriteTx).ready();
        doReturn(FluentFutures.immediateFluentFuture(Optional.of(dataNode)))
                .when(mockConfigReadWriteTx).read(TEST_PATH);

        DOMStoreTransactionChain mockTxChain = mock(DOMStoreTransactionChain.class);
        doReturn(mockConfigReadTx).when(mockTxChain).newReadOnlyTransaction();
        doReturn(mockConfigWriteTx).when(mockTxChain).newWriteOnlyTransaction();
        doReturn(mockConfigReadWriteTx).when(mockTxChain).newReadWriteTransaction();
        doReturn(mockTxChain).when(mockConfigStore).createTransactionChain();

        doReturn(mock(DOMStoreTransactionChain.class)).when(mockOperStore).createTransactionChain();

        doReturn(Futures.immediateFuture(Boolean.TRUE)).when(mockOperCommitCohort).canCommit();
        doReturn(Futures.immediateFuture(null)).when(mockOperCommitCohort).preCommit();
        doReturn(Futures.immediateFuture(null)).when(mockOperCommitCohort).commit();
        doReturn(Futures.immediateFuture(null)).when(mockOperCommitCohort).abort();

        doReturn(mockOperReadTx).when(mockOperStore).newReadOnlyTransaction();

        doReturn(mockOperWriteTx).when(mockOperStore).newWriteOnlyTransaction();
        doReturn(mockOperCommitCohort).when(mockOperWriteTx).ready();

        doReturn(mockOperReadWriteTx).when(mockOperStore).newReadWriteTransaction();
        doReturn(mockOperCommitCohort).when(mockOperReadWriteTx).ready();

        DOMStoreTransactionChain mockOperTxChain = mock(DOMStoreTransactionChain.class);
        doReturn(mockOperReadTx).when(mockOperTxChain).newReadOnlyTransaction();
        doReturn(mockOperWriteTx).when(mockOperTxChain).newWriteOnlyTransaction();
        doReturn(mockOperReadWriteTx).when(mockOperTxChain).newReadWriteTransaction();
        doReturn(mockOperTxChain).when(mockOperStore).createTransactionChain();
    }

    @Test
    public void testReadOnlyTransaction() throws Exception {
        DOMDataReadOnlyTransaction tx = adapter.newReadOnlyTransaction();

        // Test successful read

        CheckedFuture<com.google.common.base.Optional<NormalizedNode<?, ?>>, ReadFailedException> readFuture =
                tx.read(LogicalDatastoreType.CONFIGURATION, TEST_PATH);
        com.google.common.base.Optional<NormalizedNode<?, ?>> readOptional = readFuture.get();
        assertEquals("isPresent", true, readOptional.isPresent());
        assertEquals("NormalizedNode", dataNode, readOptional.get());

        // Test successful exists

        CheckedFuture<Boolean, ReadFailedException> existsFuture =
                tx.exists(LogicalDatastoreType.CONFIGURATION, TEST_PATH);
        assertEquals("exists", Boolean.TRUE, existsFuture.get());

        // Test failed read

        String errorMsg = "mock read error";
        Throwable cause = new RuntimeException();
        doReturn(Futures.immediateFailedCheckedFuture(new org.opendaylight.mdsal.common.api.ReadFailedException(
                errorMsg, cause))).when(mockConfigReadTx).read(TEST_PATH);

        try {
            tx.read(LogicalDatastoreType.CONFIGURATION, TEST_PATH).checkedGet();
            fail("Expected ReadFailedException");
        } catch (ReadFailedException e) {
            assertEquals("getMessage", errorMsg, e.getMessage());
            assertEquals("getCause", cause, e.getCause());
        }

        // Test close
        tx.close();
        verify(mockConfigReadTx).close();
    }

    @Test
    public void testWriteOnlyTransaction() throws Exception {
        // Test successful write operations and submit

        DOMDataWriteTransaction tx = adapter.newWriteOnlyTransaction();

        tx.put(LogicalDatastoreType.CONFIGURATION, TEST_PATH, dataNode);
        verify(mockConfigWriteTx).write(TEST_PATH, dataNode);

        tx.merge(LogicalDatastoreType.CONFIGURATION, TEST_PATH, dataNode);
        verify(mockConfigWriteTx).merge(TEST_PATH, dataNode);

        tx.delete(LogicalDatastoreType.CONFIGURATION, TEST_PATH);
        verify(mockConfigWriteTx).delete(TEST_PATH);

        tx.commit().get(5, TimeUnit.SECONDS);

        InOrder inOrder = inOrder(mockConfigCommitCohort);
        inOrder.verify(mockConfigCommitCohort).canCommit();
        inOrder.verify(mockConfigCommitCohort).preCommit();
        inOrder.verify(mockConfigCommitCohort).commit();

        // Test cancel

        tx = adapter.newWriteOnlyTransaction();
        tx.put(LogicalDatastoreType.CONFIGURATION, TEST_PATH, dataNode);
        tx.cancel();
        verify(mockConfigWriteTx).close();

        // Test submit with OptimisticLockFailedException

        String errorMsg = "mock OptimisticLockFailedException";
        Throwable cause = new ConflictingModificationAppliedException(TEST_PATH, "mock");
        doReturn(Futures.immediateFailedFuture(new org.opendaylight.mdsal.common.api.OptimisticLockFailedException(
                errorMsg, cause))).when(mockConfigCommitCohort).canCommit();

        try {
            tx = adapter.newWriteOnlyTransaction();
            tx.put(LogicalDatastoreType.CONFIGURATION, TEST_PATH, dataNode);
            commit(tx);
            fail("Expected OptimisticLockFailedException");
        } catch (OptimisticLockFailedException e) {
            assertEquals("getMessage", errorMsg, e.getMessage());
            assertEquals("getCause", cause, e.getCause());
        }

        // Test submit with TransactionCommitFailedException

        errorMsg = "mock TransactionCommitFailedException";
        cause = new DataValidationFailedException(TEST_PATH, "mock");
        doReturn(Futures.immediateFailedFuture(new org.opendaylight.mdsal.common.api.TransactionCommitFailedException(
                errorMsg, cause))).when(mockConfigCommitCohort).canCommit();

        try {
            tx = adapter.newWriteOnlyTransaction();
            tx.put(LogicalDatastoreType.CONFIGURATION, TEST_PATH, dataNode);
            commit(tx);
            fail("Expected TransactionCommitFailedException");
        } catch (TransactionCommitFailedException e) {
            assertEquals("getMessage", errorMsg, e.getMessage());
            assertEquals("getCause", cause, e.getCause());
        }

        // Test submit with DataStoreUnavailableException

        errorMsg = "mock NoShardLeaderException";
        cause = new DataStoreUnavailableException("mock", new RuntimeException());
        doReturn(Futures.immediateFailedFuture(cause)).when(mockConfigCommitCohort).canCommit();

        try {
            tx = adapter.newWriteOnlyTransaction();
            tx.put(LogicalDatastoreType.CONFIGURATION, TEST_PATH, dataNode);
            commit(tx);
            fail("Expected TransactionCommitFailedException");
        } catch (TransactionCommitFailedException e) {
            assertEquals("getCause type", DataStoreUnavailableException.class, e.getCause().getClass());
        }

        // Test submit with RuntimeException

        errorMsg = "mock RuntimeException";
        cause = new RuntimeException(errorMsg);
        doReturn(Futures.immediateFailedFuture(cause)).when(mockConfigCommitCohort).canCommit();

        try {
            tx = adapter.newWriteOnlyTransaction();
            tx.put(LogicalDatastoreType.CONFIGURATION, TEST_PATH, dataNode);
            commit(tx);
            fail("Expected TransactionCommitFailedException");
        } catch (TransactionCommitFailedException e) {
            assertEquals("getCause", cause, e.getCause());
        }
    }

    @Test
    public void testReadWriteTransaction() throws Exception {
        DOMDataReadWriteTransaction tx = adapter.newReadWriteTransaction();

        CheckedFuture<com.google.common.base.Optional<NormalizedNode<?, ?>>, ReadFailedException> readFuture =
                tx.read(LogicalDatastoreType.CONFIGURATION, TEST_PATH);
        com.google.common.base.Optional<NormalizedNode<?, ?>> readOptional = readFuture.get();
        assertEquals("isPresent", true, readOptional.isPresent());
        assertEquals("NormalizedNode", dataNode, readOptional.get());

        tx.put(LogicalDatastoreType.CONFIGURATION, TEST_PATH, dataNode);
        verify(mockConfigReadWriteTx).write(TEST_PATH, dataNode);

        tx.commit().get(5, TimeUnit.SECONDS);

        InOrder inOrder = inOrder(mockConfigCommitCohort);
        inOrder.verify(mockConfigCommitCohort).canCommit();
        inOrder.verify(mockConfigCommitCohort).preCommit();
        inOrder.verify(mockConfigCommitCohort).commit();
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testTransactionChain() throws Exception {
        TransactionChainListener mockListener = mock(TransactionChainListener.class);
        doNothing().when(mockListener).onTransactionChainSuccessful(anyObject());
        doNothing().when(mockListener).onTransactionChainFailed(anyObject(), anyObject(), anyObject());

        DOMTransactionChain chain = adapter.createTransactionChain(mockListener);

        // Test read-only tx

        DOMDataReadOnlyTransaction readTx = chain.newReadOnlyTransaction();

        CheckedFuture<com.google.common.base.Optional<NormalizedNode<?, ?>>, ReadFailedException> readFuture =
                readTx.read(LogicalDatastoreType.CONFIGURATION, TEST_PATH);
        com.google.common.base.Optional<NormalizedNode<?, ?>> readOptional = readFuture.get();
        assertEquals("isPresent", true, readOptional.isPresent());
        assertEquals("NormalizedNode", dataNode, readOptional.get());

        // Test write-only tx

        DOMDataWriteTransaction writeTx = chain.newWriteOnlyTransaction();

        writeTx.put(LogicalDatastoreType.CONFIGURATION, TEST_PATH, dataNode);
        verify(mockConfigWriteTx).write(TEST_PATH, dataNode);
        writeTx.commit().get(5, TimeUnit.SECONDS);

        InOrder inOrder = inOrder(mockConfigCommitCohort);
        inOrder.verify(mockConfigCommitCohort).canCommit();
        inOrder.verify(mockConfigCommitCohort).preCommit();
        inOrder.verify(mockConfigCommitCohort).commit();

        // Test read-write tx

        DOMDataReadWriteTransaction readWriteTx = chain.newReadWriteTransaction();

        readFuture = readWriteTx.read(LogicalDatastoreType.CONFIGURATION, TEST_PATH);
        readOptional = readFuture.get();
        assertEquals("isPresent", true, readOptional.isPresent());
        assertEquals("NormalizedNode", dataNode, readOptional.get());

        chain.close();
        verify(mockListener).onTransactionChainSuccessful(chain);

        // Test failed chain

        doReturn(Futures.immediateFailedFuture(new org.opendaylight.mdsal.common.api.TransactionCommitFailedException(
                "mock", (Throwable)null))).when(mockConfigCommitCohort).canCommit();

        chain = adapter.createTransactionChain(mockListener);

        writeTx = chain.newWriteOnlyTransaction();

        try {
            writeTx.put(LogicalDatastoreType.CONFIGURATION, TEST_PATH, dataNode);
            commit(writeTx);
            fail("Expected TransactionCommitFailedException");
        } catch (TransactionCommitFailedException e) {
            // expected
        }

        ArgumentCaptor<AsyncTransaction> failedTx = ArgumentCaptor.forClass(AsyncTransaction.class);
        verify(mockListener).onTransactionChainFailed(eq(chain), failedTx.capture(),
                any(TransactionCommitFailedException.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDataTreeChangeListener() {
        DOMDataTreeChangeService domDTCLService =
                (DOMDataTreeChangeService) adapter.getSupportedExtensions().get(DOMDataTreeChangeService.class);
        assertNotNull("DOMDataTreeChangeService not found", domDTCLService);

        ArgumentCaptor<org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener> storeDTCL =
                ArgumentCaptor.forClass(org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener.class);
        ListenerRegistration<org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener> mockReg =
                mock(ListenerRegistration.class);
        doNothing().when(mockReg).close();
        doAnswer(invocation -> storeDTCL.getValue()).when(mockReg).getInstance();
        doReturn(mockReg).when(mockConfigStore).registerTreeChangeListener(eq(TEST_PATH),
                storeDTCL.capture());

        DOMDataTreeChangeListener brokerDTCL = mock(DOMDataTreeChangeListener.class);
        ListenerRegistration<DOMDataTreeChangeListener> reg = domDTCLService.registerDataTreeChangeListener(
                new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, TEST_PATH), brokerDTCL);
        assertEquals("getInstance", brokerDTCL, reg.getInstance());

        Collection<DataTreeCandidate> changes = Arrays.asList(mock(DataTreeCandidate.class));
        storeDTCL.getValue().onDataTreeChanged(changes);
        verify(brokerDTCL).onDataTreeChanged(changes);

        reg.close();
        verify(mockReg).close();

        // Test ClusteredDOMDataTreeChangeListener

        ArgumentCaptor<org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener> storeClusteredDTCL =
                ArgumentCaptor.forClass(org.opendaylight.mdsal.dom.api.DOMDataTreeChangeListener.class);
        mockReg = mock(ListenerRegistration.class);
        doReturn(mockReg).when(mockConfigStore).registerTreeChangeListener(eq(TEST_PATH),
                storeClusteredDTCL.capture());

        final ClusteredDOMDataTreeChangeListener brokerClusteredDTCL = mock(ClusteredDOMDataTreeChangeListener.class);
        domDTCLService.registerDataTreeChangeListener(new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION,
                TEST_PATH), brokerClusteredDTCL);

        assertTrue("Expected ClusteredDOMDataTreeChangeListener: " + storeClusteredDTCL.getValue(),
                storeClusteredDTCL.getValue()
                    instanceof org.opendaylight.mdsal.dom.api.ClusteredDOMDataTreeChangeListener);
        storeClusteredDTCL.getValue().onDataTreeChanged(changes);
        verify(brokerClusteredDTCL).onDataTreeChanged(changes);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDataTreeCommitCohortRegistry() {
        org.opendaylight.controller.md.sal.dom.api.DOMDataTreeCommitCohortRegistry domCohortRegistry =
            (org.opendaylight.controller.md.sal.dom.api.DOMDataTreeCommitCohortRegistry)
                adapter.getSupportedExtensions().get(
                    org.opendaylight.controller.md.sal.dom.api.DOMDataTreeCommitCohortRegistry.class);
        assertNotNull("DOMDataTreeCommitCohortRegistry not found", domCohortRegistry);

        DOMDataTreeCommitCohort mockCohort = mock(DOMDataTreeCommitCohort.class);
        org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier treeId =
                new org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier(
                    org.opendaylight.mdsal.common.api.LogicalDatastoreType.CONFIGURATION, TEST_PATH);
        DOMDataTreeCommitCohortRegistration<DOMDataTreeCommitCohort> mockReg =
                mock(DOMDataTreeCommitCohortRegistration.class);
        doReturn(mockReg).when(mockCommitCohortRegistry).registerCommitCohort(treeId, mockCohort);

        DOMDataTreeCommitCohortRegistration<DOMDataTreeCommitCohort> reg = domCohortRegistry.registerCommitCohort(
                treeId, mockCohort);
        assertEquals("DOMDataTreeCommitCohortRegistration", mockReg, reg);

        verify(mockCommitCohortRegistry).registerCommitCohort(treeId, mockCohort);
    }

    @Test
    @Deprecated
    public void testSubmit() throws Exception {
        DOMDataWriteTransaction tx = adapter.newWriteOnlyTransaction();

        tx.put(LogicalDatastoreType.CONFIGURATION, TEST_PATH, dataNode);
        verify(mockConfigWriteTx).write(TEST_PATH, dataNode);

        tx.submit().get(5, TimeUnit.SECONDS);

        InOrder inOrder = inOrder(mockConfigCommitCohort);
        inOrder.verify(mockConfigCommitCohort).canCommit();
        inOrder.verify(mockConfigCommitCohort).preCommit();
        inOrder.verify(mockConfigCommitCohort).commit();

        String errorMsg = "mock OptimisticLockFailedException";
        Throwable cause = new ConflictingModificationAppliedException(TEST_PATH, "mock");
        doReturn(Futures.immediateFailedFuture(new org.opendaylight.mdsal.common.api.TransactionCommitFailedException(
                errorMsg, cause))).when(mockConfigCommitCohort).canCommit();

        try {
            tx = adapter.newWriteOnlyTransaction();
            tx.put(LogicalDatastoreType.CONFIGURATION, TEST_PATH, dataNode);
            commit(tx);
            fail("Expected TransactionCommitFailedException");
        } catch (TransactionCommitFailedException e) {
            assertEquals("getMessage", errorMsg, e.getMessage());
            assertEquals("getCause", cause, e.getCause());
        }
    }

    @SuppressWarnings("checkstyle:AvoidHidingCauseException")
    private static void commit(DOMDataWriteTransaction tx)
            throws TransactionCommitFailedException, InterruptedException, TimeoutException {
        try {
            tx.commit().get(5, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            assertTrue("Expected TransactionCommitFailedException. Actual: " + e.getCause(),
                    e.getCause() instanceof TransactionCommitFailedException);
            throw (TransactionCommitFailedException)e.getCause();
        }
    }

    private interface TestDOMStore extends DOMStore, DOMStoreTreeChangePublisher,
            org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohortRegistry {
    }
}
