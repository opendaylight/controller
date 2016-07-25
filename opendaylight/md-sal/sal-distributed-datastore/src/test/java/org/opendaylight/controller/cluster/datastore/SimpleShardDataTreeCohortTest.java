/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collections;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.tree.ConflictingModificationAppliedException;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateTip;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataValidationFailedException;
import scala.concurrent.Promise;

/**
 * Unit tests for SimpleShardDataTreeCohort.
 *
 * @author Thomas Pantelis
 */
public class SimpleShardDataTreeCohortTest extends AbstractTest {
    @Mock
    private ShardDataTree mockShardDataTree;

    @Mock
    private DataTreeModification mockModification;

    @Mock
    private CompositeDataTreeCohort mockUserCohorts;

    @Mock
    private FutureCallback<DataTreeCandidate> mockPreCallback;

    private SimpleShardDataTreeCohort cohort;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        doNothing().when(mockUserCohorts).commit();
        doReturn(Optional.empty()).when(mockUserCohorts).abort();

        cohort = new SimpleShardDataTreeCohort(mockShardDataTree, mockModification, nextTransactionId(),
            mockUserCohorts);
    }

    @Test
    public void testCanCommitSuccess() throws Exception {
        canCommitSuccess();
    }

    private void canCommitSuccess() {
        doAnswer(invocation -> {
            invocation.getArgumentAt(0, SimpleShardDataTreeCohort.class).successfulCanCommit();
            return null;
        }).when(mockShardDataTree).startCanCommit(cohort);

        @SuppressWarnings("unchecked")
        final FutureCallback<Void> callback = mock(FutureCallback.class);
        cohort.canCommit(callback);

        verify(callback).onSuccess(null);
        verifyNoMoreInteractions(callback);
    }

    private void testValidatationPropagates(final Exception cause) throws DataValidationFailedException {
        doAnswer(invocation -> {
            invocation.getArgumentAt(0, SimpleShardDataTreeCohort.class).failedCanCommit(cause);
            return null;
        }).when(mockShardDataTree).startCanCommit(cohort);

        @SuppressWarnings("unchecked")
        final FutureCallback<Void> callback = mock(FutureCallback.class);
        cohort.canCommit(callback);

        verify(callback).onFailure(cause);
        verifyNoMoreInteractions(callback);
    }

    @Test
    public void testCanCommitWithConflictingModEx() throws DataValidationFailedException {
        testValidatationPropagates(new ConflictingModificationAppliedException(YangInstanceIdentifier.EMPTY, "mock"));
    }

    @Test
    public void testCanCommitWithDataValidationEx() throws DataValidationFailedException {
        testValidatationPropagates(new DataValidationFailedException(YangInstanceIdentifier.EMPTY, "mock"));
    }

    @Test
    public void testCanCommitWithIllegalArgumentEx() throws DataValidationFailedException {
        testValidatationPropagates(new IllegalArgumentException("mock"));
    }

    private DataTreeCandidateTip preCommitSuccess() {
        final DataTreeCandidateTip mockCandidate = mock(DataTreeCandidateTip.class);
        doAnswer(invocation -> {
            invocation.getArgumentAt(0, SimpleShardDataTreeCohort.class).successfulPreCommit(mockCandidate);
            return null;
        }).when(mockShardDataTree).startPreCommit(cohort);

        @SuppressWarnings("unchecked")
        final FutureCallback<DataTreeCandidate> callback = mock(FutureCallback.class);
        cohort.preCommit(callback);

        verify(callback).onSuccess(mockCandidate);
        verifyNoMoreInteractions(callback);

        assertSame("getCandidate", mockCandidate, cohort.getCandidate());

        return mockCandidate;
    }

    @Test
    public void testPreCommitAndCommitSuccess() throws Exception {
        canCommitSuccess();
        final DataTreeCandidateTip candidate = preCommitSuccess();

        doAnswer(invocation -> {
            invocation.getArgumentAt(0, SimpleShardDataTreeCohort.class).successfulCommit(UnsignedLong.valueOf(0));
            return null;
        }).when(mockShardDataTree).startCommit(cohort, candidate);

        @SuppressWarnings("unchecked")
        final
        FutureCallback<UnsignedLong> mockCommitCallback = mock(FutureCallback.class);
        cohort.commit(mockCommitCallback);

        verify(mockCommitCallback).onSuccess(any(UnsignedLong.class));
        verifyNoMoreInteractions(mockCommitCallback);

        verify(mockUserCohorts).commit();
    }

    @Test
    public void testPreCommitWithIllegalArgumentEx() throws Throwable {
        canCommitSuccess();

        final Exception cause = new IllegalArgumentException("mock");
        doAnswer(invocation -> {
            invocation.getArgumentAt(0, SimpleShardDataTreeCohort.class).failedPreCommit(cause);
            return null;
        }).when(mockShardDataTree).startPreCommit(cohort);

        @SuppressWarnings("unchecked")
        final FutureCallback<DataTreeCandidate> callback = mock(FutureCallback.class);
        cohort.preCommit(callback);

        verify(callback).onFailure(cause);
        verifyNoMoreInteractions(callback);

        verify(mockUserCohorts).abort();
    }

    @Test
    public void testPreCommitWithReportedFailure() throws Throwable {
        canCommitSuccess();

        final Exception cause = new IllegalArgumentException("mock");
        cohort.reportFailure(cause);

        @SuppressWarnings("unchecked")
        final FutureCallback<DataTreeCandidate> callback = mock(FutureCallback.class);
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
            invocation.getArgumentAt(0, SimpleShardDataTreeCohort.class).failedCommit(cause);
            return null;
        }).when(mockShardDataTree).startCommit(cohort, candidate);

        @SuppressWarnings("unchecked")
        final FutureCallback<UnsignedLong> callback = mock(FutureCallback.class);
        cohort.commit(callback);

        verify(callback).onFailure(cause);
        verifyNoMoreInteractions(callback);

        verify(mockUserCohorts).abort();
    }

    @Test
    public void testAbort() throws Exception {
        doNothing().when(mockShardDataTree).startAbort(cohort);

        cohort.abort().get();

        verify(mockShardDataTree).startAbort(cohort);
    }

    @Test
    public void testAbortWithCohorts() throws Exception {
        doNothing().when(mockShardDataTree).startAbort(cohort);

        final Promise<Iterable<Object>> cohortFuture = akka.dispatch.Futures.promise();
        doReturn(Optional.of(cohortFuture.future())).when(mockUserCohorts).abort();

        final ListenableFuture<Void> abortFuture = cohort.abort();

        cohortFuture.success(Collections.emptyList());

        abortFuture.get();
        verify(mockShardDataTree).startAbort(cohort);
    }
}
