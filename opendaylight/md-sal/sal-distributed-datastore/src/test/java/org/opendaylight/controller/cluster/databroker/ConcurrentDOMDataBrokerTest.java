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
import org.opendaylight.controller.cluster.datastore.AbstractDataStore;
import org.opendaylight.mdsal.common.api.CommitInfo;
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
    private final DOMStoreThreePhaseCommitCohort mockCohort = mock(DOMStoreThreePhaseCommitCohort.class);
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
                    Uninterruptibles.awaitUninterruptibly(asyncCanCommitContinue, 10, TimeUnit.SECONDS);
                    future.set(Boolean.TRUE);
                }).start();
            } else {
                future.set(Boolean.TRUE);
            }

            return future;
        };

        doAnswer(asyncCanCommit).when(mockCohort).canCommit();
        doReturn(immediateNullFluentFuture()).when(mockCohort).preCommit();
        doReturn(immediateNullFluentFuture()).when(mockCohort).commit();

        ListenableFuture<? extends CommitInfo> future = coordinator.commit(transaction, mockCohort);

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

        InOrder inOrder = inOrder(mockCohort);
        inOrder.verify(mockCohort, times(1)).canCommit();
        inOrder.verify(mockCohort, times(1)).preCommit();
        inOrder.verify(mockCohort, times(1)).commit();
    }

    @Test
    public void testSubmitWithNegativeCanCommitResponse() throws Exception {
        doReturn(Futures.immediateFuture(Boolean.FALSE)).when(mockCohort).canCommit();
        doReturn(immediateNullFluentFuture()).when(mockCohort).abort();

        assertFailure(coordinator.commit(transaction, mockCohort), null, mockCohort);
    }

    private static void assertFailure(final ListenableFuture<?> future, final Exception expCause,
            final DOMStoreThreePhaseCommitCohort mockCohort) throws Exception {
        try {
            future.get(5, TimeUnit.SECONDS);
            fail("Expected TransactionCommitFailedException");
        } catch (ExecutionException e) {
            TransactionCommitFailedException tcf = TransactionCommitFailedExceptionMapper.COMMIT_ERROR_MAPPER.apply(e);
            if (expCause != null) {
                assertSame("Expected cause", expCause.getClass(), tcf.getCause().getClass());
            }
            verify(mockCohort, times(1)).abort();
        } catch (TimeoutException e) {
            throw e;
        }
    }

    @Test
    public void testSubmitWithCanCommitException() throws Exception {
        final Exception cause = new IllegalStateException("mock");
        doReturn(Futures.immediateFailedFuture(cause)).when(mockCohort).canCommit();
        doReturn(immediateNullFluentFuture()).when(mockCohort).abort();

        assertFailure(coordinator.commit(transaction, mockCohort), cause, mockCohort);
    }

    @Test
    public void testSubmitWithPreCommitException() throws Exception {
        doReturn(immediateTrueFluentFuture()).when(mockCohort).canCommit();
        final IllegalStateException cause = new IllegalStateException("mock");
        doReturn(Futures.immediateFailedFuture(cause)).when(mockCohort).preCommit();
        doReturn(immediateNullFluentFuture()).when(mockCohort).abort();

        assertFailure(coordinator.commit(transaction, mockCohort), cause, mockCohort);
    }

    @Test
    public void testSubmitWithCommitException() throws Exception {
        doReturn(immediateTrueFluentFuture()).when(mockCohort).canCommit();
        doReturn(immediateNullFluentFuture()).when(mockCohort).preCommit();
        final IllegalStateException cause = new IllegalStateException("mock");
        doReturn(Futures.immediateFailedFuture(cause)).when(mockCohort).commit();
        doReturn(immediateNullFluentFuture()).when(mockCohort).abort();

        assertFailure(coordinator.commit(transaction, mockCohort), cause, mockCohort);
    }

    @Test
    public void testSubmitWithAbortException() throws Exception {
        final Exception canCommitCause = new IllegalStateException("canCommit error");
        doReturn(Futures.immediateFailedFuture(canCommitCause)).when(mockCohort).canCommit();
        final Exception abortCause = new IllegalStateException("abort error");
        doReturn(Futures.immediateFailedFuture(abortCause)).when(mockCohort).abort();

        assertFailure(coordinator.commit(transaction, mockCohort), canCommitCause, mockCohort);
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

            dataTxn.put(LogicalDatastoreType.OPERATIONAL, YangInstanceIdentifier.of(), mock(NormalizedNode.class));
            dataTxn.put(LogicalDatastoreType.OPERATIONAL, YangInstanceIdentifier.of(), mock(NormalizedNode.class));
            dataTxn.read(LogicalDatastoreType.OPERATIONAL, YangInstanceIdentifier.of());

            verify(configDomStore, never()).newReadWriteTransaction();
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

            dataTxn.put(LogicalDatastoreType.OPERATIONAL, YangInstanceIdentifier.of(), mock(NormalizedNode.class));
            dataTxn.put(LogicalDatastoreType.OPERATIONAL, YangInstanceIdentifier.of(), mock(NormalizedNode.class));

            verify(configDomStore, never()).newWriteOnlyTransaction();
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

            dataTxn.read(LogicalDatastoreType.OPERATIONAL, YangInstanceIdentifier.of());
            dataTxn.read(LogicalDatastoreType.OPERATIONAL, YangInstanceIdentifier.of());

            verify(configDomStore, never()).newReadOnlyTransaction();
            verify(operationalDomStore, times(1)).newReadOnlyTransaction();
        }
    }

    @Test
    public void testSubmitWithOnlyOneSubTransaction() throws InterruptedException {
        DOMStore configDomStore = mock(DOMStore.class);
        DOMStore operationalDomStore = mock(DOMStore.class);
        DOMStoreReadWriteTransaction mockStoreReadWriteTransaction = mock(DOMStoreReadWriteTransaction.class);

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
                    DOMStoreThreePhaseCommitCohort cohort) {
                commitCohorts.add(cohort);
                latch.countDown();
                return super.commit(writeTx, cohort);
            }
        }) {
            DOMDataTreeReadWriteTransaction domDataReadWriteTransaction = dataBroker.newReadWriteTransaction();

            domDataReadWriteTransaction.delete(LogicalDatastoreType.OPERATIONAL, YangInstanceIdentifier.of());

            domDataReadWriteTransaction.commit();

            assertTrue(latch.await(10, TimeUnit.SECONDS));

            assertTrue(commitCohorts.size() == 1);
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

            domDataWriteTransaction.put(LogicalDatastoreType.OPERATIONAL, YangInstanceIdentifier.of(),
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
        final var mockConfigStore = mock(AbstractDataStore.class);
        final var mockOperStore = mock(AbstractDataStore.class);
        try (ConcurrentDOMDataBroker dataBroker = new ConcurrentDOMDataBroker(ImmutableMap.of(
                LogicalDatastoreType.OPERATIONAL, mockOperStore,
                LogicalDatastoreType.CONFIGURATION, mockConfigStore), futureExecutor)) {

            ClassToInstanceMap<DOMDataBrokerExtension> supportedExtensions = dataBroker.getExtensions();
            assertNotNull(supportedExtensions.getInstance(DOMDataTreeChangeService.class));

            DOMDataTreeCommitCohortRegistry cohortRegistry = supportedExtensions.getInstance(
                    DOMDataTreeCommitCohortRegistry.class);
            assertNotNull(cohortRegistry);

            DOMDataTreeCommitCohort cohort = mock(DOMDataTreeCommitCohort.class);
            DOMDataTreeIdentifier path = new DOMDataTreeIdentifier(
                    org.opendaylight.mdsal.common.api.LogicalDatastoreType.CONFIGURATION,
                    YangInstanceIdentifier.of());
            cohortRegistry.registerCommitCohort(path, cohort);

            verify(mockConfigStore).registerCommitCohort(path, cohort);
        }
    }
}
