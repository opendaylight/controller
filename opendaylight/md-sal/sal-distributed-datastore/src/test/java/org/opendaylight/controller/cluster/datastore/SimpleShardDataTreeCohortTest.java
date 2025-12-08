/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.FutureCallback;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.tree.api.ConflictingModificationAppliedException;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidateTip;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeModification;
import org.opendaylight.yangtools.yang.data.tree.api.DataValidationFailedException;

/**
 * Unit tests for SimpleShardDataTreeCohort.
 *
 * @author Thomas Pantelis
 */
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class SimpleShardDataTreeCohortTest extends AbstractTest {
    @Mock
    private ShardDataTree mockShardDataTree;
    @Mock
    private DataTreeModification mockModification;
    @Mock
    private CompositeDataTreeCohort mockUserCohorts;
    @Mock
    private FutureCallback<DataTreeCandidate> mockPreCallback;
    @Mock
    private ShardStats stats;

    private SimpleCommitCohort cohort;

    @Before
    public void setup() {
        doReturn(null).when(mockUserCohorts).commit();
        doReturn(null).when(mockUserCohorts).abort();
        doReturn(stats).when(mockShardDataTree).getStats();

        final var parent = new SimpleTransactionParent(mockShardDataTree);
        final var transaction = new ReadWriteShardDataTreeTransaction(parent, nextTransactionId(), mockModification);
        transaction.close();

        cohort = new SimpleCommitCohort(transaction, mockUserCohorts);
    }

    @Test
    public void testCanCommitSuccess() {
        canCommitSuccess();
    }

    private void canCommitSuccess() {
        doAnswer(invocation -> {
            invocation.<CommitCohort>getArgument(0).successfulCanCommit();
            return null;
        }).when(mockShardDataTree).startCanCommit(cohort);

        final FutureCallback<Empty> callback = mock();
        cohort.canCommit(callback);

        verify(callback).onSuccess(Empty.value());
        verifyNoMoreInteractions(callback);
    }

    private void testValidatationPropagates(final Exception cause) {
        doAnswer(invocation -> {
            invocation.<SimpleCommitCohort>getArgument(0).failedCanCommit(cause);
            return null;
        }).when(mockShardDataTree).startCanCommit(cohort);

        final FutureCallback<Empty> callback = mock();
        cohort.canCommit(callback);

        verify(callback).onFailure(cause);
        verifyNoMoreInteractions(callback);
    }

    @Test
    public void testCanCommitWithConflictingModEx() {
        testValidatationPropagates(new ConflictingModificationAppliedException(YangInstanceIdentifier.of(), "mock"));
    }

    @Test
    public void testCanCommitWithDataValidationEx() {
        testValidatationPropagates(new DataValidationFailedException(YangInstanceIdentifier.of(), "mock"));
    }

    @Test
    public void testCanCommitWithIllegalArgumentEx() {
        testValidatationPropagates(new IllegalArgumentException("mock"));
    }

    private DataTreeCandidateTip preCommitSuccess() {
        final DataTreeCandidateTip mockCandidate = mock(DataTreeCandidateTip.class);
        doAnswer(invocation -> {
            invocation.<SimpleCommitCohort>getArgument(0).successfulPreCommit(mockCandidate);
            return null;
        }).when(mockShardDataTree).startPreCommit(cohort);

        final FutureCallback<DataTreeCandidate> callback = mock();
        cohort.preCommit(callback);

        verify(callback).onSuccess(mockCandidate);
        verifyNoMoreInteractions(callback);

        assertSame("getCandidate", mockCandidate, cohort.getCandidate());

        return mockCandidate;
    }

    @Test
    public void testPreCommitAndCommitSuccess() {
        canCommitSuccess();
        final DataTreeCandidateTip candidate = preCommitSuccess();

        doAnswer(invocation -> {
            invocation.<SimpleCommitCohort>getArgument(0).successfulCommit(UnsignedLong.valueOf(0), () -> { });
            return null;
        }).when(mockShardDataTree).startCommit(cohort, candidate);

        final FutureCallback<UnsignedLong> mockCommitCallback = mock();
        cohort.commit(mockCommitCallback);

        verify(mockCommitCallback).onSuccess(any(UnsignedLong.class));
        verifyNoMoreInteractions(mockCommitCallback);

        verify(mockUserCohorts).commit();
    }

    @Test
    public void testPreCommitWithIllegalArgumentEx() {
        canCommitSuccess();

        final Exception cause = new IllegalArgumentException("mock");
        doAnswer(invocation -> {
            invocation.<SimpleCommitCohort>getArgument(0).failedPreCommit(cause);
            return null;
        }).when(mockShardDataTree).startPreCommit(cohort);

        final FutureCallback<DataTreeCandidate> callback = mock();
        cohort.preCommit(callback);

        verify(callback).onFailure(cause);
        verifyNoMoreInteractions(callback);

        verify(mockUserCohorts).abort();
    }

    @Test
    public void testPreCommitWithReportedFailure() {
        canCommitSuccess();

        final Exception cause = new IllegalArgumentException("mock");
        cohort.reportFailure(cause);

        final FutureCallback<DataTreeCandidate> callback = mock();
        cohort.preCommit(callback);

        verify(callback).onFailure(cause);
        verifyNoMoreInteractions(callback);

        verify(mockShardDataTree, never()).startPreCommit(cohort);
    }

    @Test
    public void testCommitWithIllegalArgumentEx() {
        canCommitSuccess();
        final DataTreeCandidateTip candidate = preCommitSuccess();

        final Exception cause = new IllegalArgumentException("mock");
        doAnswer(invocation -> {
            invocation.<SimpleCommitCohort>getArgument(0).failedCommit(cause);
            return null;
        }).when(mockShardDataTree).startCommit(cohort, candidate);

        final FutureCallback<UnsignedLong> callback = mock();
        cohort.commit(callback);

        verify(callback).onFailure(cause);
        verifyNoMoreInteractions(callback);

        verify(mockUserCohorts).abort();
    }

    private static Future<?> abort(final CommitCohort cohort) {
        final var f = new CompletableFuture<Empty>();
        cohort.abort(new FutureCallback<>() {
            @Override
            public void onSuccess(final Empty result) {
                f.complete(result);
            }

            @Override
            public void onFailure(final Throwable failure) {
                f.completeExceptionally(failure);
            }
        });

        return f;
    }

    @Test
    public void testAbort() throws Exception {
        doReturn(Boolean.TRUE).when(mockShardDataTree).startAbort(cohort);

        abort(cohort).get();
        verify(mockShardDataTree).startAbort(cohort);
    }

    @Test
    public void testAbortWithCohorts() throws Exception {
        doReturn(true).when(mockShardDataTree).startAbort(cohort);

        doReturn(CompletableFuture.completedFuture(null)).when(mockUserCohorts).abort();

        final var abortFuture = abort(cohort);

        abortFuture.get();
        verify(mockShardDataTree).startAbort(cohort);
    }
}
