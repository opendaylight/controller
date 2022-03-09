/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.FutureCallback;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.persisted.CommitTransactionPayload;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;

public final class ShardDataTreeMocking {

    private ShardDataTreeMocking() {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    private static <T> FutureCallback<T> mockCallback() {
        return mock(FutureCallback.class);
    }

    public static ShardDataTreeCohort immediateCanCommit(final ShardDataTreeCohort cohort) {
        final FutureCallback<Void> callback = mockCallback();
        doNothing().when(callback).onSuccess(null);
        cohort.canCommit(callback);

        verify(callback).onSuccess(null);
        verifyNoMoreInteractions(callback);
        return cohort;
    }

    public static FutureCallback<Void> coordinatedCanCommit(final ShardDataTreeCohort cohort) {
        final FutureCallback<Void> callback = mockCallback();
        doNothing().when(callback).onSuccess(null);
        doNothing().when(callback).onFailure(any(Throwable.class));
        cohort.canCommit(callback);
        return callback;
    }

    public static ShardDataTreeCohort immediatePreCommit(final ShardDataTreeCohort cohort) {
        final FutureCallback<DataTreeCandidate> callback = mockCallback();
        doNothing().when(callback).onSuccess(any(DataTreeCandidate.class));
        cohort.preCommit(callback);

        verify(callback).onSuccess(any(DataTreeCandidate.class));
        verifyNoMoreInteractions(callback);
        return cohort;
    }

    public static FutureCallback<DataTreeCandidate> coordinatedPreCommit(final ShardDataTreeCohort cohort) {
        final FutureCallback<DataTreeCandidate> callback = mockCallback();
        doNothing().when(callback).onSuccess(any(DataTreeCandidate.class));
        doNothing().when(callback).onFailure(any(Throwable.class));
        cohort.preCommit(callback);
        return callback;
    }

    public static ShardDataTreeCohort immediateCommit(final ShardDataTreeCohort cohort) {
        final FutureCallback<UnsignedLong> callback = mockCallback();
        doNothing().when(callback).onSuccess(any(UnsignedLong.class));
        cohort.commit(callback);

        verify(callback, timeout(5000)).onSuccess(any(UnsignedLong.class));
        verifyNoMoreInteractions(callback);
        return cohort;
    }

    public static FutureCallback<UnsignedLong> coordinatedCommit(final ShardDataTreeCohort cohort) {
        final FutureCallback<UnsignedLong> callback = mockCallback();
        doNothing().when(callback).onSuccess(any(UnsignedLong.class));
        doNothing().when(callback).onFailure(any(Throwable.class));
        cohort.commit(callback);
        return callback;
    }

    public static FutureCallback<UnsignedLong> immediate3PhaseCommit(final ShardDataTreeCohort cohort) {
        final FutureCallback<UnsignedLong> commitCallback = mockCallback();
        doNothing().when(commitCallback).onSuccess(any(UnsignedLong.class));
        doNothing().when(commitCallback).onFailure(any(Throwable.class));

        final FutureCallback<DataTreeCandidate> preCommitCallback = mockCallback();
        doAnswer(invocation -> {
            cohort.commit(commitCallback);
            return null;
        }).when(preCommitCallback).onSuccess(any(DataTreeCandidate.class));
        doNothing().when(preCommitCallback).onFailure(any(Throwable.class));

        final FutureCallback<Void> canCommit = mockCallback();
        doAnswer(invocation -> {
            cohort.preCommit(preCommitCallback);
            return null;
        }).when(canCommit).onSuccess(null);
        doNothing().when(canCommit).onFailure(any(Throwable.class));

        cohort.canCommit(canCommit);
        return commitCallback;
    }

    private static <T> Object invokeSuccess(final InvocationOnMock invocation, final T value) {
        invocation.<FutureCallback<T>>getArgument(0).onSuccess(value);
        return null;
    }

    private static Object invokeFailure(final InvocationOnMock invocation) {
        invocation.<FutureCallback<?>>getArgument(0).onFailure(mock(Exception.class));
        return null;
    }

    @SuppressWarnings("unchecked")
    public static ShardDataTreeCohort failedCanCommit(final ShardDataTreeCohort mock) {
        doAnswer(ShardDataTreeMocking::invokeFailure).when(mock).canCommit(any(FutureCallback.class));
        return mock;
    }

    @SuppressWarnings("unchecked")
    public static ShardDataTreeCohort failedPreCommit(final ShardDataTreeCohort mock) {
        doAnswer(ShardDataTreeMocking::invokeFailure).when(mock).preCommit(any(FutureCallback.class));
        return mock;
    }

    @SuppressWarnings("unchecked")
    public static ShardDataTreeCohort failedCommit(final ShardDataTreeCohort mock) {
        doAnswer(ShardDataTreeMocking::invokeFailure).when(mock).commit(any(FutureCallback.class));
        return mock;
    }

    @SuppressWarnings("unchecked")
    public static ShardDataTreeCohort successfulCanCommit(final ShardDataTreeCohort mock) {
        doAnswer(invocation -> invokeSuccess(invocation, null)).when(mock).canCommit(any(FutureCallback.class));

        return mock;
    }

    public static ShardDataTreeCohort successfulPreCommit(final ShardDataTreeCohort mock) {
        return successfulPreCommit(mock, mock(DataTreeCandidate.class));
    }

    @SuppressWarnings("unchecked")
    public static ShardDataTreeCohort successfulPreCommit(final ShardDataTreeCohort mock,
            final DataTreeCandidate candidate) {
        doAnswer(invocation -> invokeSuccess(invocation, candidate)).when(mock).preCommit(any(FutureCallback.class));

        return mock;
    }

    public static ShardDataTreeCohort successfulCommit(final ShardDataTreeCohort mock) {
        return successfulCommit(mock, UnsignedLong.ZERO);
    }

    @SuppressWarnings("unchecked")
    public static ShardDataTreeCohort successfulCommit(final ShardDataTreeCohort mock, final UnsignedLong index) {
        doAnswer(invocation -> invokeSuccess(invocation, index)).when(mock).commit(any(FutureCallback.class));

        return mock;
    }

    @SuppressWarnings("unchecked")
    public static void assertSequencedCommit(final ShardDataTreeCohort mock) {
        final InOrder inOrder = inOrder(mock);
        inOrder.verify(mock).canCommit(any(FutureCallback.class));
        inOrder.verify(mock).preCommit(any(FutureCallback.class));
        inOrder.verify(mock).commit(any(FutureCallback.class));
    }

    public static void immediatePayloadReplication(final ShardDataTree shardDataTree, final Shard mockShard) {
        doAnswer(invocation -> {
            shardDataTree.applyReplicatedPayload(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(mockShard).persistPayload(any(TransactionIdentifier.class), any(CommitTransactionPayload.class),
                anyBoolean());
    }
}
