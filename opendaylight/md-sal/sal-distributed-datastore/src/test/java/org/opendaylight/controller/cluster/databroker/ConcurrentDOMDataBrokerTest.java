/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.databroker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.opendaylight.yangtools.util.concurrent.FluentFutures.immediateFalseFluentFuture;
import static org.opendaylight.yangtools.util.concurrent.FluentFutures.immediateNullFluentFuture;
import static org.opendaylight.yangtools.util.concurrent.FluentFutures.immediateTrueFluentFuture;

import com.google.common.base.Throwables;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.cluster.datastore.DistributedDataStore;
import org.opendaylight.controller.cluster.datastore.exceptions.NoShardLeaderException;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.DataStoreUnavailableException;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.TransactionCommitFailedException;
import org.opendaylight.mdsal.dom.api.DOMDataBrokerExtension;
import org.opendaylight.mdsal.dom.api.DOMDataTreeChangeService;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohort;
import org.opendaylight.mdsal.dom.api.DOMDataTreeCommitCohortRegistry;
import org.opendaylight.mdsal.dom.api.DOMDataTreeIdentifier;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeReadWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMDataTreeWriteTransaction;
import org.opendaylight.mdsal.dom.api.DOMTransactionChain;
import org.opendaylight.mdsal.dom.api.DOMTransactionChainListener;
import org.opendaylight.mdsal.dom.broker.TransactionCommitFailedExceptionMapper;
import org.opendaylight.mdsal.dom.spi.store.DOMStore;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreReadWriteTransaction;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreThreePhaseCommitCohort;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreTransactionChain;
import org.opendaylight.mdsal.dom.spi.store.DOMStoreWriteTransaction;
import org.opendaylight.mdsal.dom.store.inmemory.InMemoryDOMDataStore;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

/**
 * Unit tests for DOMConcurrentDataCommitCoordinator.
 *
 * @author Thomas Pantelis
 */
public class ConcurrentDOMDataBrokerTest {

    private final DOMDataTreeWriteTransaction transaction = mock(DOMDataTreeWriteTransaction.class);
    private final DOMStoreThreePhaseCommitCohort mockCohort1 = mock(DOMStoreThreePhaseCommitCohort.class);
    private final DOMStoreThreePhaseCommitCohort mockCohort2 = mock(DOMStoreThreePhaseCommitCohort.class);
    private final ThreadPoolExecutor futureExecutor =
            new ThreadPoolExecutor(0, 1, 5, TimeUnit.SECONDS, new SynchronousQueue<>());
    private ConcurrentDOMDataBroker coordinator;

    @Before
    public void setup() {
        doReturn("tx").when(transaction).getIdentifier();

        DOMStore store = new InMemoryDOMDataStore("OPER", MoreExecutors.newDirectExecutorService());

        coordinator = new ConcurrentDOMDataBroker(ImmutableMap.of(LogicalDatastoreType.OPERATIONAL, store),
                futureExecutor);
    }

    @After
    public void tearDown() {
        futureExecutor.shutdownNow();
    }

    @Test
    public void testSuccessfulSubmitAsync() throws Exception {
        testSuccessfulSubmit(true);
    }

    @Test
    public void testSuccessfulSubmitSync() throws Exception {
        testSuccessfulSubmit(false);
    }

    private void testSuccessfulSubmit(final boolean doAsync) throws InterruptedException {
        final CountDownLatch asyncCanCommitContinue = new CountDownLatch(1);
        Answer<ListenableFuture<Boolean>> asyncCanCommit = invocation -> {
            final SettableFuture<Boolean> future = SettableFuture.create();
            if (doAsync) {
                new Thread(() -> {
                    Uninterruptibles.awaitUninterruptibly(asyncCanCommitContinue,
                            10, TimeUnit.SECONDS);
                    future.set(Boolean.TRUE);
                }).start();
            } else {
                future.set(Boolean.TRUE);
            }

            return future;
        };

        doAnswer(asyncCanCommit).when(mockCohort1).canCommit();
        doReturn(immediateNullFluentFuture()).when(mockCohort1).preCommit();
        doReturn(immediateNullFluentFuture()).when(mockCohort1).commit();

        doReturn(immediateTrueFluentFuture()).when(mockCohort2).canCommit();
        doReturn(immediateNullFluentFuture()).when(mockCohort2).preCommit();
        doReturn(immediateNullFluentFuture()).when(mockCohort2).commit();

        ListenableFuture<? extends CommitInfo> future =
                coordinator.commit(transaction, Arrays.asList(mockCohort1, mockCohort2));

        final CountDownLatch doneLatch = new CountDownLatch(1);
        final AtomicReference<Throwable> caughtEx = new AtomicReference<>();
        Futures.addCallback(future, new FutureCallback<CommitInfo>() {
            @Override
            public void onSuccess(final CommitInfo result) {
                doneLatch.countDown();
            }

            @Override
            public void onFailure(final Throwable failure) {
                caughtEx.set(failure);
                doneLatch.countDown();
            }
        }, MoreExecutors.directExecutor());

        asyncCanCommitContinue.countDown();

        assertTrue("Submit complete", doneLatch.await(5, TimeUnit.SECONDS));

        if (caughtEx.get() != null) {
            Throwables.throwIfUnchecked(caughtEx.get());
            throw new RuntimeException(caughtEx.get());
        }

        assertEquals("Task count", doAsync ? 1 : 0, futureExecutor.getTaskCount());

        InOrder inOrder = inOrder(mockCohort1, mockCohort2);
        inOrder.verify(mockCohort1).canCommit();
        inOrder.verify(mockCohort2).canCommit();
        inOrder.verify(mockCohort1).preCommit();
        inOrder.verify(mockCohort2).preCommit();
        inOrder.verify(mockCohort1).commit();
        inOrder.verify(mockCohort2).commit();
    }

    @Test
    public void testSubmitWithNegativeCanCommitResponse() throws Exception {
        doReturn(immediateTrueFluentFuture()).when(mockCohort1).canCommit();
        doReturn(immediateNullFluentFuture()).when(mockCohort1).abort();

        doReturn(Futures.immediateFuture(Boolean.FALSE)).when(mockCohort2).canCommit();
        doReturn(immediateNullFluentFuture()).when(mockCohort2).abort();

        DOMStoreThreePhaseCommitCohort mockCohort3 = mock(DOMStoreThreePhaseCommitCohort.class);
        doReturn(Futures.immediateFuture(Boolean.FALSE)).when(mockCohort3).canCommit();
        doReturn(immediateNullFluentFuture()).when(mockCohort3).abort();

        ListenableFuture<? extends CommitInfo> future = coordinator.commit(
                transaction, Arrays.asList(mockCohort1, mockCohort2, mockCohort3));

        assertFailure(future, null, mockCohort1, mockCohort2, mockCohort3);
    }

    private static void assertFailure(final ListenableFuture<?> future, final Exception expCause,
            final DOMStoreThreePhaseCommitCohort... mockCohorts) throws Exception {
        try {
            future.get(5, TimeUnit.SECONDS);
            fail("Expected TransactionCommitFailedException");
        } catch (ExecutionException e) {
            TransactionCommitFailedException tcf = TransactionCommitFailedExceptionMapper.COMMIT_ERROR_MAPPER.apply(e);
            if (expCause != null) {
                assertSame("Expected cause", expCause.getClass(), tcf.getCause().getClass());
            }

            InOrder inOrder = inOrder((Object[])mockCohorts);
            for (DOMStoreThreePhaseCommitCohort c: mockCohorts) {
                inOrder.verify(c).abort();
            }
        } catch (TimeoutException e) {
            throw e;
        }
    }

    @Test
    public void testSubmitWithCanCommitException() throws Exception {
        doReturn(immediateTrueFluentFuture()).when(mockCohort1).canCommit();
        doReturn(immediateNullFluentFuture()).when(mockCohort1).abort();

        IllegalStateException cause = new IllegalStateException("mock");
        doReturn(Futures.immediateFailedFuture(cause)).when(mockCohort2).canCommit();
        doReturn(immediateNullFluentFuture()).when(mockCohort2).abort();

        FluentFuture<? extends CommitInfo> future = coordinator.commit(
                transaction, Arrays.asList(mockCohort1, mockCohort2));

        assertFailure(future, cause, mockCohort1, mockCohort2);
    }

    @Test
    public void testSubmitWithCanCommitDataStoreUnavailableException() throws Exception {
        doReturn(immediateTrueFluentFuture()).when(mockCohort1).canCommit();
        doReturn(immediateNullFluentFuture()).when(mockCohort1).abort();
        NoShardLeaderException rootCause = new NoShardLeaderException("mock");
        DataStoreUnavailableException cause = new DataStoreUnavailableException(rootCause.getMessage(), rootCause);
        doReturn(Futures.immediateFailedFuture(rootCause)).when(mockCohort2).canCommit();
        doReturn(immediateNullFluentFuture()).when(mockCohort2).abort();

        FluentFuture<? extends CommitInfo> future = coordinator.commit(
            transaction, Arrays.asList(mockCohort1, mockCohort2));

        assertFailure(future, cause, mockCohort1, mockCohort2);
    }

    @Test
    public void testSubmitWithPreCommitException() throws Exception {
        doReturn(immediateTrueFluentFuture()).when(mockCohort1).canCommit();
        doReturn(immediateNullFluentFuture()).when(mockCohort1).preCommit();
        doReturn(immediateNullFluentFuture()).when(mockCohort1).abort();

        doReturn(immediateTrueFluentFuture()).when(mockCohort2).canCommit();
        IllegalStateException cause = new IllegalStateException("mock");
        doReturn(Futures.immediateFailedFuture(cause)).when(mockCohort2).preCommit();
        doReturn(immediateNullFluentFuture()).when(mockCohort2).abort();

        DOMStoreThreePhaseCommitCohort mockCohort3 = mock(DOMStoreThreePhaseCommitCohort.class);
        doReturn(immediateTrueFluentFuture()).when(mockCohort3).canCommit();
        doReturn(Futures.immediateFailedFuture(new IllegalStateException("mock2")))
                .when(mockCohort3).preCommit();
        doReturn(immediateNullFluentFuture()).when(mockCohort3).abort();

        FluentFuture<? extends CommitInfo> future = coordinator.commit(
                transaction, Arrays.asList(mockCohort1, mockCohort2, mockCohort3));

        assertFailure(future, cause, mockCohort1, mockCohort2, mockCohort3);
    }

    @Test
    public void testSubmitWithCommitException() throws Exception {
        doReturn(immediateTrueFluentFuture()).when(mockCohort1).canCommit();
        doReturn(immediateNullFluentFuture()).when(mockCohort1).preCommit();
        doReturn(immediateNullFluentFuture()).when(mockCohort1).commit();
        doReturn(immediateNullFluentFuture()).when(mockCohort1).abort();

        doReturn(immediateTrueFluentFuture()).when(mockCohort2).canCommit();
        doReturn(immediateNullFluentFuture()).when(mockCohort2).preCommit();
        IllegalStateException cause = new IllegalStateException("mock");
        doReturn(Futures.immediateFailedFuture(cause)).when(mockCohort2).commit();
        doReturn(immediateNullFluentFuture()).when(mockCohort2).abort();

        DOMStoreThreePhaseCommitCohort mockCohort3 = mock(DOMStoreThreePhaseCommitCohort.class);
        doReturn(immediateTrueFluentFuture()).when(mockCohort3).canCommit();
        doReturn(immediateNullFluentFuture()).when(mockCohort3).preCommit();
        doReturn(Futures.immediateFailedFuture(new IllegalStateException("mock2")))
                .when(mockCohort3).commit();
        doReturn(immediateNullFluentFuture()).when(mockCohort3).abort();

        FluentFuture<? extends CommitInfo> future = coordinator.commit(
                transaction, Arrays.asList(mockCohort1, mockCohort2, mockCohort3));

        assertFailure(future, cause, mockCohort1, mockCohort2, mockCohort3);
    }

    @Test
    public void testSubmitWithAbortException() throws Exception {
        doReturn(immediateTrueFluentFuture()).when(mockCohort1).canCommit();
        doReturn(Futures.immediateFailedFuture(new IllegalStateException("mock abort error")))
                .when(mockCohort1).abort();

        IllegalStateException cause = new IllegalStateException("mock canCommit error");
        doReturn(Futures.immediateFailedFuture(cause)).when(mockCohort2).canCommit();
        doReturn(immediateNullFluentFuture()).when(mockCohort2).abort();

        FluentFuture<? extends CommitInfo> future = coordinator.commit(
                transaction, Arrays.asList(mockCohort1, mockCohort2));

        assertFailure(future, cause, mockCohort1, mockCohort2);
    }

    @Test
    public void testCreateReadWriteTransaction() {
        DOMStore domStore = mock(DOMStore.class);
        try (ConcurrentDOMDataBroker dataBroker = new ConcurrentDOMDataBroker(ImmutableMap.of(
                LogicalDatastoreType.OPERATIONAL, domStore, LogicalDatastoreType.CONFIGURATION, domStore),
                futureExecutor)) {
            dataBroker.newReadWriteTransaction();

            verify(domStore, never()).newReadWriteTransaction();
        }
    }

    @Test
    public void testCreateWriteOnlyTransaction() {
        DOMStore domStore = mock(DOMStore.class);
        try (ConcurrentDOMDataBroker dataBroker = new ConcurrentDOMDataBroker(ImmutableMap.of(
                LogicalDatastoreType.OPERATIONAL, domStore, LogicalDatastoreType.CONFIGURATION, domStore),
                futureExecutor)) {
            dataBroker.newWriteOnlyTransaction();

            verify(domStore, never()).newWriteOnlyTransaction();
        }
    }

    @Test
    public void testCreateReadOnlyTransaction() {
        DOMStore domStore = mock(DOMStore.class);
        try (ConcurrentDOMDataBroker dataBroker = new ConcurrentDOMDataBroker(ImmutableMap.of(
                LogicalDatastoreType.OPERATIONAL, domStore, LogicalDatastoreType.CONFIGURATION, domStore),
                futureExecutor)) {
            dataBroker.newReadOnlyTransaction();

            verify(domStore, never()).newReadOnlyTransaction();
        }
    }

    @Test
    public void testLazySubTransactionCreationForReadWriteTransactions() {
        DOMStore configDomStore = mock(DOMStore.class);
        DOMStore operationalDomStore = mock(DOMStore.class);
        DOMStoreReadWriteTransaction storeTxn = mock(DOMStoreReadWriteTransaction.class);

        doReturn(storeTxn).when(operationalDomStore).newReadWriteTransaction();
        doReturn(storeTxn).when(configDomStore).newReadWriteTransaction();

        try (ConcurrentDOMDataBroker dataBroker = new ConcurrentDOMDataBroker(ImmutableMap.of(
                LogicalDatastoreType.OPERATIONAL, operationalDomStore, LogicalDatastoreType.CONFIGURATION,
                configDomStore), futureExecutor)) {
            DOMDataTreeReadWriteTransaction dataTxn = dataBroker.newReadWriteTransaction();

            dataTxn.put(LogicalDatastoreType.OPERATIONAL, YangInstanceIdentifier.empty(), mock(NormalizedNode.class));
            dataTxn.put(LogicalDatastoreType.OPERATIONAL, YangInstanceIdentifier.empty(), mock(NormalizedNode.class));
            dataTxn.read(LogicalDatastoreType.OPERATIONAL, YangInstanceIdentifier.empty());

            verify(configDomStore, never()).newReadWriteTransaction();
            verify(operationalDomStore, times(1)).newReadWriteTransaction();

            dataTxn.put(LogicalDatastoreType.CONFIGURATION, YangInstanceIdentifier.empty(), mock(NormalizedNode.class));

            verify(configDomStore, times(1)).newReadWriteTransaction();
            verify(operationalDomStore, times(1)).newReadWriteTransaction();
        }

    }

    @Test
    public void testLazySubTransactionCreationForWriteOnlyTransactions() {
        DOMStore configDomStore = mock(DOMStore.class);
        DOMStore operationalDomStore = mock(DOMStore.class);
        DOMStoreWriteTransaction storeTxn = mock(DOMStoreWriteTransaction.class);

        doReturn(storeTxn).when(operationalDomStore).newWriteOnlyTransaction();
        doReturn(storeTxn).when(configDomStore).newWriteOnlyTransaction();

        try (ConcurrentDOMDataBroker dataBroker = new ConcurrentDOMDataBroker(ImmutableMap.of(
                LogicalDatastoreType.OPERATIONAL, operationalDomStore, LogicalDatastoreType.CONFIGURATION,
                configDomStore), futureExecutor)) {
            DOMDataTreeWriteTransaction dataTxn = dataBroker.newWriteOnlyTransaction();

            dataTxn.put(LogicalDatastoreType.OPERATIONAL, YangInstanceIdentifier.empty(), mock(NormalizedNode.class));
            dataTxn.put(LogicalDatastoreType.OPERATIONAL, YangInstanceIdentifier.empty(), mock(NormalizedNode.class));

            verify(configDomStore, never()).newWriteOnlyTransaction();
            verify(operationalDomStore, times(1)).newWriteOnlyTransaction();

            dataTxn.put(LogicalDatastoreType.CONFIGURATION, YangInstanceIdentifier.empty(), mock(NormalizedNode.class));

            verify(configDomStore, times(1)).newWriteOnlyTransaction();
            verify(operationalDomStore, times(1)).newWriteOnlyTransaction();
        }
    }

    @Test
    public void testLazySubTransactionCreationForReadOnlyTransactions() {
        DOMStore configDomStore = mock(DOMStore.class);
        DOMStore operationalDomStore = mock(DOMStore.class);
        DOMStoreReadTransaction storeTxn = mock(DOMStoreReadTransaction.class);

        doReturn(storeTxn).when(operationalDomStore).newReadOnlyTransaction();
        doReturn(storeTxn).when(configDomStore).newReadOnlyTransaction();

        try (ConcurrentDOMDataBroker dataBroker = new ConcurrentDOMDataBroker(ImmutableMap.of(
                LogicalDatastoreType.OPERATIONAL, operationalDomStore, LogicalDatastoreType.CONFIGURATION,
                configDomStore), futureExecutor)) {
            DOMDataTreeReadTransaction dataTxn = dataBroker.newReadOnlyTransaction();

            dataTxn.read(LogicalDatastoreType.OPERATIONAL, YangInstanceIdentifier.empty());
            dataTxn.read(LogicalDatastoreType.OPERATIONAL, YangInstanceIdentifier.empty());

            verify(configDomStore, never()).newReadOnlyTransaction();
            verify(operationalDomStore, times(1)).newReadOnlyTransaction();

            dataTxn.read(LogicalDatastoreType.CONFIGURATION, YangInstanceIdentifier.empty());

            verify(configDomStore, times(1)).newReadOnlyTransaction();
            verify(operationalDomStore, times(1)).newReadOnlyTransaction();
        }
    }

    @Test
    public void testSubmitWithOnlyOneSubTransaction() throws InterruptedException {
        DOMStore configDomStore = mock(DOMStore.class);
        DOMStore operationalDomStore = mock(DOMStore.class);
        DOMStoreReadWriteTransaction mockStoreReadWriteTransaction = mock(DOMStoreReadWriteTransaction.class);
        DOMStoreThreePhaseCommitCohort mockCohort = mock(DOMStoreThreePhaseCommitCohort.class);

        doReturn(mockStoreReadWriteTransaction).when(operationalDomStore).newReadWriteTransaction();
        doReturn(mockCohort).when(mockStoreReadWriteTransaction).ready();
        doReturn(immediateFalseFluentFuture()).when(mockCohort).canCommit();
        doReturn(immediateNullFluentFuture()).when(mockCohort).abort();

        final CountDownLatch latch = new CountDownLatch(1);
        final List<DOMStoreThreePhaseCommitCohort> commitCohorts = new ArrayList<>();

        try (ConcurrentDOMDataBroker dataBroker = new ConcurrentDOMDataBroker(ImmutableMap.of(
                LogicalDatastoreType.OPERATIONAL, operationalDomStore, LogicalDatastoreType.CONFIGURATION,
                configDomStore), futureExecutor) {
            @Override
            public FluentFuture<? extends CommitInfo> commit(DOMDataTreeWriteTransaction writeTx,
                    Collection<DOMStoreThreePhaseCommitCohort> cohorts) {
                commitCohorts.addAll(cohorts);
                latch.countDown();
                return super.commit(writeTx, cohorts);
            }
        }) {
            DOMDataTreeReadWriteTransaction domDataReadWriteTransaction = dataBroker.newReadWriteTransaction();

            domDataReadWriteTransaction.delete(LogicalDatastoreType.OPERATIONAL, YangInstanceIdentifier.empty());

            domDataReadWriteTransaction.commit();

            assertTrue(latch.await(10, TimeUnit.SECONDS));

            assertTrue(commitCohorts.size() == 1);
        }
    }

    @Test
    public void testSubmitWithOnlyTwoSubTransactions() throws InterruptedException {
        DOMStore configDomStore = mock(DOMStore.class);
        DOMStore operationalDomStore = mock(DOMStore.class);
        DOMStoreReadWriteTransaction operationalTransaction = mock(DOMStoreReadWriteTransaction.class);
        DOMStoreReadWriteTransaction configTransaction = mock(DOMStoreReadWriteTransaction.class);
        DOMStoreThreePhaseCommitCohort mockCohortOperational = mock(DOMStoreThreePhaseCommitCohort.class);
        DOMStoreThreePhaseCommitCohort mockCohortConfig = mock(DOMStoreThreePhaseCommitCohort.class);

        doReturn(operationalTransaction).when(operationalDomStore).newReadWriteTransaction();
        doReturn(configTransaction).when(configDomStore).newReadWriteTransaction();

        doReturn(mockCohortOperational).when(operationalTransaction).ready();
        doReturn(immediateFalseFluentFuture()).when(mockCohortOperational).canCommit();
        doReturn(immediateNullFluentFuture()).when(mockCohortOperational).abort();

        doReturn(mockCohortConfig).when(configTransaction).ready();
        doReturn(immediateFalseFluentFuture()).when(mockCohortConfig).canCommit();
        doReturn(immediateNullFluentFuture()).when(mockCohortConfig).abort();

        final CountDownLatch latch = new CountDownLatch(1);
        final List<DOMStoreThreePhaseCommitCohort> commitCohorts = new ArrayList<>();

        try (ConcurrentDOMDataBroker dataBroker = new ConcurrentDOMDataBroker(ImmutableMap.of(
                LogicalDatastoreType.OPERATIONAL, operationalDomStore, LogicalDatastoreType.CONFIGURATION,
                configDomStore), futureExecutor) {
            @Override
            @SuppressWarnings("checkstyle:hiddenField")
            public FluentFuture<? extends CommitInfo> commit(DOMDataTreeWriteTransaction writeTx,
                    Collection<DOMStoreThreePhaseCommitCohort> cohorts) {
                commitCohorts.addAll(cohorts);
                latch.countDown();
                return super.commit(writeTx, cohorts);
            }
        }) {
            DOMDataTreeReadWriteTransaction domDataReadWriteTransaction = dataBroker.newReadWriteTransaction();

            domDataReadWriteTransaction.put(LogicalDatastoreType.OPERATIONAL, YangInstanceIdentifier.empty(),
                    mock(NormalizedNode.class));
            domDataReadWriteTransaction.merge(LogicalDatastoreType.CONFIGURATION, YangInstanceIdentifier.empty(),
                    mock(NormalizedNode.class));

            domDataReadWriteTransaction.commit();

            assertTrue(latch.await(10, TimeUnit.SECONDS));

            assertTrue(commitCohorts.size() == 2);
        }
    }

    @Test
    public void testCreateTransactionChain() {
        DOMStore domStore = mock(DOMStore.class);
        try (ConcurrentDOMDataBroker dataBroker = new ConcurrentDOMDataBroker(ImmutableMap.of(
                LogicalDatastoreType.OPERATIONAL, domStore, LogicalDatastoreType.CONFIGURATION, domStore),
                futureExecutor)) {

            dataBroker.createTransactionChain(mock(DOMTransactionChainListener.class));

            verify(domStore, times(2)).createTransactionChain();
        }

    }

    @Test
    public void testCreateTransactionOnChain() {
        DOMStore domStore = mock(DOMStore.class);
        try (ConcurrentDOMDataBroker dataBroker = new ConcurrentDOMDataBroker(ImmutableMap.of(
                LogicalDatastoreType.OPERATIONAL, domStore, LogicalDatastoreType.CONFIGURATION, domStore),
                futureExecutor)) {

            DOMStoreReadWriteTransaction operationalTransaction = mock(DOMStoreReadWriteTransaction.class);
            DOMStoreTransactionChain mockChain = mock(DOMStoreTransactionChain.class);

            doReturn(mockChain).when(domStore).createTransactionChain();
            doReturn(operationalTransaction).when(mockChain).newWriteOnlyTransaction();

            DOMTransactionChain transactionChain = dataBroker.createTransactionChain(
                    mock(DOMTransactionChainListener.class));

            DOMDataTreeWriteTransaction domDataWriteTransaction = transactionChain.newWriteOnlyTransaction();

            verify(mockChain, never()).newWriteOnlyTransaction();

            domDataWriteTransaction.put(LogicalDatastoreType.OPERATIONAL, YangInstanceIdentifier.empty(),
                    mock(NormalizedNode.class));
        }
    }

    @Test
    public void testEmptyTransactionSubmitSucceeds() throws ExecutionException, InterruptedException {
        DOMStore domStore = mock(DOMStore.class);
        try (ConcurrentDOMDataBroker dataBroker = new ConcurrentDOMDataBroker(ImmutableMap.of(
                LogicalDatastoreType.OPERATIONAL, domStore, LogicalDatastoreType.CONFIGURATION, domStore),
                futureExecutor)) {

            FluentFuture<? extends CommitInfo> submit1 = dataBroker.newWriteOnlyTransaction().commit();

            assertNotNull(submit1);

            submit1.get();

            FluentFuture<? extends CommitInfo> submit2 = dataBroker.newReadWriteTransaction().commit();

            assertNotNull(submit2);

            submit2.get();
        }
    }

    @Test
    public void testExtensions() {
        DistributedDataStore mockConfigStore = mock(DistributedDataStore.class);
        DistributedDataStore mockOperStore = mock(DistributedDataStore.class);
        try (ConcurrentDOMDataBroker dataBroker = new ConcurrentDOMDataBroker(ImmutableMap.of(
                LogicalDatastoreType.OPERATIONAL, mockOperStore,
                LogicalDatastoreType.CONFIGURATION, mockConfigStore), futureExecutor)) {

            ClassToInstanceMap<DOMDataBrokerExtension> supportedExtensions = dataBroker.getExtensions();
            assertNotNull(supportedExtensions.getInstance(DOMDataTreeChangeService.class));

            DOMDataTreeCommitCohortRegistry cohortRegistry = supportedExtensions.getInstance(
                DOMDataTreeCommitCohortRegistry.class);
            assertNotNull(cohortRegistry);

            DOMDataTreeCommitCohort mockCohort = mock(DOMDataTreeCommitCohort.class);
            DOMDataTreeIdentifier path = new DOMDataTreeIdentifier(
                    org.opendaylight.mdsal.common.api.LogicalDatastoreType.CONFIGURATION,
                    YangInstanceIdentifier.empty());
            cohortRegistry.registerCommitCohort(path, mockCohort);

            verify(mockConfigStore).registerCommitCohort(path, mockCohort);
        }
    }
}
