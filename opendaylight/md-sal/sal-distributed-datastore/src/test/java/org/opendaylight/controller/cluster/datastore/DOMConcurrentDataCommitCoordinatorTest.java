/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.cluster.datastore.ConcurrentDOMDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataWriteTransaction;
import org.opendaylight.controller.md.sal.dom.store.impl.InMemoryDOMDataStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStore;
import org.opendaylight.controller.sal.core.spi.data.DOMStoreThreePhaseCommitCohort;

/**
 * Unit tests for DOMConcurrentDataCommitCoordinator.
 *
 * @author Thomas Pantelis
 */
public class DOMConcurrentDataCommitCoordinatorTest {

    private final DOMDataWriteTransaction transaction = mock(DOMDataWriteTransaction.class);
    private final DOMStoreThreePhaseCommitCohort mockCohort1 = mock(DOMStoreThreePhaseCommitCohort.class);
    private final DOMStoreThreePhaseCommitCohort mockCohort2 = mock(DOMStoreThreePhaseCommitCohort.class);
    private final ThreadPoolExecutor futureExecutor =
            new ThreadPoolExecutor(0, 1, 5, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
    private ConcurrentDOMDataBroker coordinator;

    @Before
    public void setup() {
        doReturn("tx").when(transaction).getIdentifier();

        DOMStore store = new InMemoryDOMDataStore("OPER",
            MoreExecutors.sameThreadExecutor());

        coordinator = new ConcurrentDOMDataBroker(ImmutableMap.of(LogicalDatastoreType.OPERATIONAL, store), futureExecutor);
    }

    @After
    public void tearDown() {
        futureExecutor.shutdownNow();
    }

    @Test
    public void testSuccessfulSubmitAsync() throws Throwable {
        testSuccessfulSubmit(true);
    }

    @Test
    public void testSuccessfulSubmitSync() throws Throwable {
        testSuccessfulSubmit(false);
    }

    private void testSuccessfulSubmit(final boolean doAsync) throws Throwable {
        final CountDownLatch asyncCanCommitContinue = new CountDownLatch(1);
        Answer<ListenableFuture<Boolean>> asyncCanCommit = new Answer<ListenableFuture<Boolean>>() {
            @Override
            public ListenableFuture<Boolean> answer(final InvocationOnMock invocation) {
                final SettableFuture<Boolean> future = SettableFuture.create();
                if(doAsync) {
                    new Thread() {
                        @Override
                        public void run() {
                            Uninterruptibles.awaitUninterruptibly(asyncCanCommitContinue,
                                    10, TimeUnit.SECONDS);
                            future.set(true);
                        }
                    }.start();
                } else {
                    future.set(true);
                }

                return future;
            }
        };

        doAnswer(asyncCanCommit).when(mockCohort1).canCommit();
        doReturn(Futures.immediateFuture(null)).when(mockCohort1).preCommit();
        doReturn(Futures.immediateFuture(null)).when(mockCohort1).commit();

        doReturn(Futures.immediateFuture(true)).when(mockCohort2).canCommit();
        doReturn(Futures.immediateFuture(null)).when(mockCohort2).preCommit();
        doReturn(Futures.immediateFuture(null)).when(mockCohort2).commit();

        CheckedFuture<Void, TransactionCommitFailedException> future = coordinator.submit(
                transaction, Arrays.asList(mockCohort1, mockCohort2));

        final CountDownLatch doneLatch = new CountDownLatch(1);
        final AtomicReference<Throwable> caughtEx = new AtomicReference<>();
        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onSuccess(final Void result) {
                doneLatch.countDown();
            }

            @Override
            public void onFailure(final Throwable t) {
                caughtEx.set(t);
                doneLatch.countDown();
            }
        });

        asyncCanCommitContinue.countDown();

        assertEquals("Submit complete", true, doneLatch.await(5, TimeUnit.SECONDS));

        if(caughtEx.get() != null) {
            throw caughtEx.get();
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
        doReturn(Futures.immediateFuture(true)).when(mockCohort1).canCommit();
        doReturn(Futures.immediateFuture(null)).when(mockCohort1).abort();

        doReturn(Futures.immediateFuture(false)).when(mockCohort2).canCommit();
        doReturn(Futures.immediateFuture(null)).when(mockCohort2).abort();

        DOMStoreThreePhaseCommitCohort mockCohort3 = mock(DOMStoreThreePhaseCommitCohort.class);
        doReturn(Futures.immediateFuture(false)).when(mockCohort3).canCommit();
        doReturn(Futures.immediateFuture(null)).when(mockCohort3).abort();

        CheckedFuture<Void, TransactionCommitFailedException> future = coordinator.submit(
                transaction, Arrays.asList(mockCohort1, mockCohort2, mockCohort3));

        assertFailure(future, null, mockCohort1, mockCohort2, mockCohort3);
    }

    private void assertFailure(final CheckedFuture<Void, TransactionCommitFailedException> future,
            final Exception expCause, final DOMStoreThreePhaseCommitCohort... mockCohorts)
                    throws Exception {
        try {
            future.checkedGet(5, TimeUnit.SECONDS);
            fail("Expected TransactionCommitFailedException");
        } catch (TransactionCommitFailedException e) {
            if(expCause != null) {
                assertSame("Expected cause", expCause, e.getCause());
            }

            InOrder inOrder = inOrder((Object[])mockCohorts);
            for(DOMStoreThreePhaseCommitCohort c: mockCohorts) {
                inOrder.verify(c).abort();
            }
        } catch (TimeoutException e) {
            throw e;
        }
    }

    @Test
    public void testSubmitWithCanCommitException() throws Exception {
        doReturn(Futures.immediateFuture(true)).when(mockCohort1).canCommit();
        doReturn(Futures.immediateFuture(null)).when(mockCohort1).abort();

        IllegalStateException cause = new IllegalStateException("mock");
        doReturn(Futures.immediateFailedFuture(cause)).when(mockCohort2).canCommit();
        doReturn(Futures.immediateFuture(null)).when(mockCohort2).abort();

        CheckedFuture<Void, TransactionCommitFailedException> future = coordinator.submit(
                transaction, Arrays.asList(mockCohort1, mockCohort2));

        assertFailure(future, cause, mockCohort1, mockCohort2);
    }

    @Test
    public void testSubmitWithPreCommitException() throws Exception {
        doReturn(Futures.immediateFuture(true)).when(mockCohort1).canCommit();
        doReturn(Futures.immediateFuture(null)).when(mockCohort1).preCommit();
        doReturn(Futures.immediateFuture(null)).when(mockCohort1).abort();

        doReturn(Futures.immediateFuture(true)).when(mockCohort2).canCommit();
        IllegalStateException cause = new IllegalStateException("mock");
        doReturn(Futures.immediateFailedFuture(cause)).when(mockCohort2).preCommit();
        doReturn(Futures.immediateFuture(null)).when(mockCohort2).abort();

        DOMStoreThreePhaseCommitCohort mockCohort3 = mock(DOMStoreThreePhaseCommitCohort.class);
        doReturn(Futures.immediateFuture(true)).when(mockCohort3).canCommit();
        doReturn(Futures.immediateFailedFuture(new IllegalStateException("mock2"))).
                when(mockCohort3).preCommit();
        doReturn(Futures.immediateFuture(null)).when(mockCohort3).abort();

        CheckedFuture<Void, TransactionCommitFailedException> future = coordinator.submit(
                transaction, Arrays.asList(mockCohort1, mockCohort2, mockCohort3));

        assertFailure(future, cause, mockCohort1, mockCohort2, mockCohort3);
    }

    @Test
    public void testSubmitWithCommitException() throws Exception {
        doReturn(Futures.immediateFuture(true)).when(mockCohort1).canCommit();
        doReturn(Futures.immediateFuture(null)).when(mockCohort1).preCommit();
        doReturn(Futures.immediateFuture(null)).when(mockCohort1).commit();
        doReturn(Futures.immediateFuture(null)).when(mockCohort1).abort();

        doReturn(Futures.immediateFuture(true)).when(mockCohort2).canCommit();
        doReturn(Futures.immediateFuture(null)).when(mockCohort2).preCommit();
        IllegalStateException cause = new IllegalStateException("mock");
        doReturn(Futures.immediateFailedFuture(cause)).when(mockCohort2).commit();
        doReturn(Futures.immediateFuture(null)).when(mockCohort2).abort();

        DOMStoreThreePhaseCommitCohort mockCohort3 = mock(DOMStoreThreePhaseCommitCohort.class);
        doReturn(Futures.immediateFuture(true)).when(mockCohort3).canCommit();
        doReturn(Futures.immediateFuture(null)).when(mockCohort3).preCommit();
        doReturn(Futures.immediateFailedFuture(new IllegalStateException("mock2"))).
                when(mockCohort3).commit();
        doReturn(Futures.immediateFuture(null)).when(mockCohort3).abort();

        CheckedFuture<Void, TransactionCommitFailedException> future = coordinator.submit(
                transaction, Arrays.asList(mockCohort1, mockCohort2, mockCohort3));

        assertFailure(future, cause, mockCohort1, mockCohort2, mockCohort3);
    }

    @Test
    public void testSubmitWithAbortException() throws Exception {
        doReturn(Futures.immediateFuture(true)).when(mockCohort1).canCommit();
        doReturn(Futures.immediateFailedFuture(new IllegalStateException("mock abort error"))).
                when(mockCohort1).abort();

        IllegalStateException cause = new IllegalStateException("mock canCommit error");
        doReturn(Futures.immediateFailedFuture(cause)).when(mockCohort2).canCommit();
        doReturn(Futures.immediateFuture(null)).when(mockCohort2).abort();

        CheckedFuture<Void, TransactionCommitFailedException> future = coordinator.submit(
                transaction, Arrays.asList(mockCohort1, mockCohort2));

        assertFailure(future, cause, mockCohort1, mockCohort2);
    }
}
