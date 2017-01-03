/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker.compat;

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

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.controller.cluster.databroker.ConcurrentDOMDataBroker;
import org.opendaylight.controller.cluster.datastore.DistributedDataStoreInterface;
import org.opendaylight.controller.cluster.datastore.exceptions.NoShardLeaderException;
import org.opendaylight.controller.md.cluster.datastore.model.TestModel;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.DataStoreUnavailableException;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.OptimisticLockFailedException;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.ClusteredDOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadOnlyTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataReadWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeListener;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeCommitCohortRegistry;
import org.opendaylight.controller.md.sal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.api.DOMTransactionChain;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohort;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohortRegistration;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadWriteTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreTransactionChain;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreTreeChangePublisher;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreWriteTransaction;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
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
    @Mock
    private TestDOMStore mockOperStore;

    @Mock
    private TestDOMStore mockConfigStore;

    @Mock
    private DOMStoreReadTransaction mockReadTx;

    @Mock
    private DOMStoreWriteTransaction mockWriteTx;

    @Mock
    private DOMStoreReadWriteTransaction mockReadWriteTx;

    @Mock
    private DOMStoreThreePhaseCommitCohort mockCommitCohort;

    private LegacyDOMDataBrokerAdapter adapter;
    private NormalizedNode<?,?> dataNode;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        ConcurrentDOMDataBroker backendBroker = new ConcurrentDOMDataBroker(ImmutableMap.of(
                org.opendaylight.mdsal.common.api.LogicalDatastoreType.OPERATIONAL, mockOperStore,
                org.opendaylight.mdsal.common.api.LogicalDatastoreType.CONFIGURATION, mockConfigStore),
                MoreExecutors.newDirectExecutorService());

        adapter = new LegacyDOMDataBrokerAdapter(backendBroker);

        doReturn(Futures.immediateFuture(Boolean.TRUE)).when(mockCommitCohort).canCommit();
        doReturn(Futures.immediateFuture(null)).when(mockCommitCohort).preCommit();
        doReturn(Futures.immediateFuture(null)).when(mockCommitCohort).commit();
        doReturn(Futures.immediateFuture(null)).when(mockCommitCohort).abort();

        dataNode = ImmutableNodes.containerNode(TestModel.TEST_QNAME);

        doReturn(mockWriteTx).when(mockConfigStore).newWriteOnlyTransaction();
        doNothing().when(mockWriteTx).write(TestModel.TEST_PATH, dataNode);
        doNothing().when(mockWriteTx).merge(TestModel.TEST_PATH, dataNode);
        doNothing().when(mockWriteTx).delete(TestModel.TEST_PATH);
        doNothing().when(mockWriteTx).close();
        doReturn(mockCommitCohort).when(mockWriteTx).ready();

        doReturn(mockReadTx).when(mockConfigStore).newReadOnlyTransaction();
        doReturn(Futures.immediateCheckedFuture(Optional.of(dataNode))).when(mockReadTx).read(TestModel.TEST_PATH);
        doReturn(Futures.immediateCheckedFuture(Boolean.TRUE)).when(mockReadTx).exists(TestModel.TEST_PATH);

        doReturn(mockReadWriteTx).when(mockConfigStore).newReadWriteTransaction();
        doNothing().when(mockReadWriteTx).write(TestModel.TEST_PATH, dataNode);
        doReturn(mockCommitCohort).when(mockReadWriteTx).ready();
        doReturn(Futures.immediateCheckedFuture(Optional.of(dataNode))).when(mockReadWriteTx).read(TestModel.TEST_PATH);

        DOMStoreTransactionChain mockTxChain = mock(DOMStoreTransactionChain.class);
        doReturn(mockReadTx).when(mockTxChain).newReadOnlyTransaction();
        doReturn(mockWriteTx).when(mockTxChain).newWriteOnlyTransaction();
        doReturn(mockReadWriteTx).when(mockTxChain).newReadWriteTransaction();
        doReturn(mockTxChain).when(mockConfigStore).createTransactionChain();

        doReturn(mock(DOMStoreTransactionChain.class)).when(mockOperStore).createTransactionChain();
    }

    @Test
    public void testReadOnlyTransaction() throws Exception {
        DOMDataReadOnlyTransaction tx = adapter.newReadOnlyTransaction();

        // Test successful read

        CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> readFuture =
                tx.read(LogicalDatastoreType.CONFIGURATION, TestModel.TEST_PATH);
        Optional<NormalizedNode<?, ?>> readOptional = readFuture.get();
        assertEquals("isPresent", true, readOptional.isPresent());
        assertEquals("NormalizedNode", dataNode, readOptional.get());

        // Test successful exists

        CheckedFuture<Boolean, ReadFailedException> existsFuture =
                tx.exists(LogicalDatastoreType.CONFIGURATION, TestModel.TEST_PATH);
        assertEquals("exists", Boolean.TRUE, existsFuture.get());

        // Test failed read

        String errorMsg = "mock read error";
        Throwable cause = new RuntimeException();
        doReturn(Futures.immediateFailedCheckedFuture(new org.opendaylight.mdsal.common.api.ReadFailedException(
                errorMsg, cause))).when(mockReadTx).read(TestModel.TEST_PATH);

        try {
            tx.read(LogicalDatastoreType.CONFIGURATION, TestModel.TEST_PATH).checkedGet();
            fail("Expected ReadFailedException");
        } catch (ReadFailedException e) {
            assertEquals("getMessage", errorMsg, e.getMessage());
            assertEquals("getCause", cause, e.getCause());
        }

        // Test close
        tx.close();
        verify(mockReadTx).close();
    }

    @Test
    public void testWriteOnlyTransaction() throws Exception {
        // Test successful write operations and submit

        DOMDataWriteTransaction tx = adapter.newWriteOnlyTransaction();

        tx.put(LogicalDatastoreType.CONFIGURATION, TestModel.TEST_PATH, dataNode);
        verify(mockWriteTx).write(TestModel.TEST_PATH, dataNode);

        tx.merge(LogicalDatastoreType.CONFIGURATION, TestModel.TEST_PATH, dataNode);
        verify(mockWriteTx).merge(TestModel.TEST_PATH, dataNode);

        tx.delete(LogicalDatastoreType.CONFIGURATION, TestModel.TEST_PATH);
        verify(mockWriteTx).delete(TestModel.TEST_PATH);

        CheckedFuture<Void, TransactionCommitFailedException> submitFuture = tx.submit();
        submitFuture.get(5, TimeUnit.SECONDS);

        InOrder inOrder = inOrder(mockCommitCohort);
        inOrder.verify(mockCommitCohort).canCommit();
        inOrder.verify(mockCommitCohort).preCommit();
        inOrder.verify(mockCommitCohort).commit();

        // Test cancel

        tx = adapter.newWriteOnlyTransaction();
        tx.put(LogicalDatastoreType.CONFIGURATION, TestModel.TEST_PATH, dataNode);
        tx.cancel();
        verify(mockWriteTx).close();

        // Test submit with OptimisticLockFailedException

        String errorMsg = "mock OptimisticLockFailedException";
        Throwable cause = new ConflictingModificationAppliedException(TestModel.TEST_PATH, "mock");
        doReturn(Futures.immediateFailedFuture(new org.opendaylight.mdsal.common.api.OptimisticLockFailedException(
                errorMsg, cause))).when(mockCommitCohort).canCommit();

        try {
            tx = adapter.newWriteOnlyTransaction();
            tx.put(LogicalDatastoreType.CONFIGURATION, TestModel.TEST_PATH, dataNode);
            submitFuture = tx.submit();
            submitFuture.checkedGet(5, TimeUnit.SECONDS);
            fail("Expected OptimisticLockFailedException");
        } catch (OptimisticLockFailedException e) {
            assertEquals("getMessage", errorMsg, e.getMessage());
            assertEquals("getCause", cause, e.getCause());
        }

        // Test submit with TransactionCommitFailedException

        errorMsg = "mock TransactionCommitFailedException";
        cause = new DataValidationFailedException(TestModel.TEST_PATH, "mock");
        doReturn(Futures.immediateFailedFuture(new org.opendaylight.mdsal.common.api.TransactionCommitFailedException(
                errorMsg, cause))).when(mockCommitCohort).canCommit();

        try {
            tx = adapter.newWriteOnlyTransaction();
            tx.put(LogicalDatastoreType.CONFIGURATION, TestModel.TEST_PATH, dataNode);
            submitFuture = tx.submit();
            submitFuture.checkedGet(5, TimeUnit.SECONDS);
            fail("Expected TransactionCommitFailedException");
        } catch (TransactionCommitFailedException e) {
            assertEquals("getMessage", errorMsg, e.getMessage());
            assertEquals("getCause", cause, e.getCause());
        }

        // Test submit with DataStoreUnavailableException

        errorMsg = "mock NoShardLeaderException";
        cause = new NoShardLeaderException("mock");
        doReturn(Futures.immediateFailedFuture(cause)).when(mockCommitCohort).canCommit();

        try {
            tx = adapter.newWriteOnlyTransaction();
            tx.put(LogicalDatastoreType.CONFIGURATION, TestModel.TEST_PATH, dataNode);
            submitFuture = tx.submit();
            submitFuture.checkedGet(5, TimeUnit.SECONDS);
            fail("Expected TransactionCommitFailedException");
        } catch (TransactionCommitFailedException e) {
            assertEquals("getCause type", DataStoreUnavailableException.class, e.getCause().getClass());
            assertEquals("Root cause", cause, e.getCause().getCause());
        }

        // Test submit with RuntimeException

        errorMsg = "mock RuntimeException";
        cause = new RuntimeException(errorMsg);
        doReturn(Futures.immediateFailedFuture(cause)).when(mockCommitCohort).canCommit();

        try {
            tx = adapter.newWriteOnlyTransaction();
            tx.put(LogicalDatastoreType.CONFIGURATION, TestModel.TEST_PATH, dataNode);
            submitFuture = tx.submit();
            submitFuture.checkedGet(5, TimeUnit.SECONDS);
            fail("Expected TransactionCommitFailedException");
        } catch (TransactionCommitFailedException e) {
            assertEquals("getCause", cause, e.getCause());
        }
    }

    @Test
    public void testReadWriteTransaction() throws Exception {
        DOMDataReadWriteTransaction tx = adapter.newReadWriteTransaction();

        CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> readFuture =
                tx.read(LogicalDatastoreType.CONFIGURATION, TestModel.TEST_PATH);
        Optional<NormalizedNode<?, ?>> readOptional = readFuture.get();
        assertEquals("isPresent", true, readOptional.isPresent());
        assertEquals("NormalizedNode", dataNode, readOptional.get());

        tx.put(LogicalDatastoreType.CONFIGURATION, TestModel.TEST_PATH, dataNode);
        verify(mockReadWriteTx).write(TestModel.TEST_PATH, dataNode);

        CheckedFuture<Void, TransactionCommitFailedException> submitFuture = tx.submit();
        submitFuture.get(5, TimeUnit.SECONDS);

        InOrder inOrder = inOrder(mockCommitCohort);
        inOrder.verify(mockCommitCohort).canCommit();
        inOrder.verify(mockCommitCohort).preCommit();
        inOrder.verify(mockCommitCohort).commit();
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

        CheckedFuture<Optional<NormalizedNode<?, ?>>, ReadFailedException> readFuture =
                readTx.read(LogicalDatastoreType.CONFIGURATION, TestModel.TEST_PATH);
        Optional<NormalizedNode<?, ?>> readOptional = readFuture.get();
        assertEquals("isPresent", true, readOptional.isPresent());
        assertEquals("NormalizedNode", dataNode, readOptional.get());

        // Test write-only tx

        DOMDataWriteTransaction writeTx = chain.newWriteOnlyTransaction();

        writeTx.put(LogicalDatastoreType.CONFIGURATION, TestModel.TEST_PATH, dataNode);
        verify(mockWriteTx).write(TestModel.TEST_PATH, dataNode);
        CheckedFuture<Void, TransactionCommitFailedException> submitFuture = writeTx.submit();
        submitFuture.get(5, TimeUnit.SECONDS);

        InOrder inOrder = inOrder(mockCommitCohort);
        inOrder.verify(mockCommitCohort).canCommit();
        inOrder.verify(mockCommitCohort).preCommit();
        inOrder.verify(mockCommitCohort).commit();

        // Test read-write tx

        DOMDataReadWriteTransaction readWriteTx = chain.newReadWriteTransaction();

        readFuture = readWriteTx.read(LogicalDatastoreType.CONFIGURATION, TestModel.TEST_PATH);
        readOptional = readFuture.get();
        assertEquals("isPresent", true, readOptional.isPresent());
        assertEquals("NormalizedNode", dataNode, readOptional.get());

        chain.close();
        verify(mockListener).onTransactionChainSuccessful(chain);

        // Test failed chain

        doReturn(Futures.immediateFailedFuture(new org.opendaylight.mdsal.common.api.TransactionCommitFailedException(
                "mock", (Throwable)null))).when(mockCommitCohort).canCommit();

        chain = adapter.createTransactionChain(mockListener);

        writeTx = chain.newWriteOnlyTransaction();

        try {
            writeTx.put(LogicalDatastoreType.CONFIGURATION, TestModel.TEST_PATH, dataNode);
            writeTx.submit().checkedGet(5, TimeUnit.SECONDS);
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
        doReturn(mockReg).when(mockConfigStore).registerTreeChangeListener(eq(TestModel.TEST_PATH),
                storeDTCL.capture());

        DOMDataTreeChangeListener brokerDTCL = mock(DOMDataTreeChangeListener.class);
        ListenerRegistration<DOMDataTreeChangeListener> reg = domDTCLService.registerDataTreeChangeListener(
                new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION, TestModel.TEST_PATH), brokerDTCL);
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
        doReturn(mockReg).when(mockConfigStore).registerTreeChangeListener(eq(TestModel.TEST_PATH),
                storeClusteredDTCL.capture());

        final ClusteredDOMDataTreeChangeListener brokerClusteredDTCL = mock(ClusteredDOMDataTreeChangeListener.class);
        domDTCLService.registerDataTreeChangeListener(new DOMDataTreeIdentifier(LogicalDatastoreType.CONFIGURATION,
                TestModel.TEST_PATH), brokerClusteredDTCL);

        assertTrue("Expected ClusteredDOMDataTreeChangeListener: " + storeClusteredDTCL.getValue(),
                storeClusteredDTCL.getValue()
                    instanceof org.opendaylight.mdsal.dom.api.ClusteredDOMDataTreeChangeListener);
        storeClusteredDTCL.getValue().onDataTreeChanged(changes);
        verify(brokerClusteredDTCL).onDataTreeChanged(changes);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDataTreeCommitCohortRegistry() {
        DOMDataTreeCommitCohortRegistry domCohortRegistry = (DOMDataTreeCommitCohortRegistry)
                adapter.getSupportedExtensions().get(DOMDataTreeCommitCohortRegistry.class);
        assertNotNull("DOMDataTreeCommitCohortRegistry not found", domCohortRegistry);

        DOMDataTreeCommitCohort mockCohort = mock(DOMDataTreeCommitCohort.class);
        org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier treeId =
                new org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier(
                    org.opendaylight.mdsal.common.api.LogicalDatastoreType.CONFIGURATION, TestModel.TEST_PATH);
        DOMDataTreeCommitCohortRegistration<DOMDataTreeCommitCohort> mockReg =
                mock(DOMDataTreeCommitCohortRegistration.class);
        doReturn(mockReg).when(mockConfigStore).registerCommitCohort(treeId, mockCohort);

        DOMDataTreeCommitCohortRegistration<DOMDataTreeCommitCohort> reg = domCohortRegistry.registerCommitCohort(
                treeId, mockCohort);
        assertEquals("DOMDataTreeCommitCohortRegistration", mockReg, reg);

        verify(mockConfigStore).registerCommitCohort(treeId, mockCohort);
    }

    @Test
    public void testDataChangeListener() {
        DOMDataChangeListener listener = mock(DOMDataChangeListener.class);
        ListenerRegistration<DOMDataChangeListener> mockReg = mock(ListenerRegistration.class);
        doReturn(mockReg).when(mockConfigStore).registerChangeListener(
                TestModel.TEST_PATH, listener, DataChangeScope.ONE);

        ListenerRegistration<DOMDataChangeListener> reg = adapter.registerDataChangeListener(
                LogicalDatastoreType.CONFIGURATION, TestModel.TEST_PATH, listener, DataChangeScope.ONE);
        assertEquals("ListenerRegistration<DOMDataChangeListener>", mockReg, reg);

        verify(mockConfigStore).registerChangeListener(TestModel.TEST_PATH, listener, DataChangeScope.ONE);
    }

    private interface TestDOMStore extends DistributedDataStoreInterface, DOMStoreTreeChangePublisher,
            org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohortRegistry {
    }
}
