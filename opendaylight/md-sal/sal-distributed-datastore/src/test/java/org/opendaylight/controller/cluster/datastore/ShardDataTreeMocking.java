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
import org.mockito.invocation.InvocationOnMock;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.controller.cluster.datastore.ShardDataTree.CommitCallback;
import org.opendaylight.controller.cluster.datastore.persisted.CommitTransactionPayload;
import org.opendaylight.yangtools.yang.common.Empty;
import org.opendaylight.yangtools.yang.data.tree.api.DataTreeCandidate;

public final class ShardDataTreeMocking {

    private ShardDataTreeMocking() {
        throw new UnsupportedOperationException();
    }

    public static CommitCohort immediateCanCommit(final CommitCohort cohort) {
        final CommitCallback<Empty> callback = mock();
        doNothing().when(callback).onSuccess(Empty.value());
        cohort.canCommit(callback);

        verify(callback).onSuccess(Empty.value());
        verifyNoMoreInteractions(callback);
        return cohort;
    }

    public static CommitCallback<Empty> coordinatedCanCommit(final CommitCohort cohort) {
        final CommitCallback<Empty> callback = mock();
        doNothing().when(callback).onSuccess(Empty.value());
        doNothing().when(callback).onFailure(any(Throwable.class));
        cohort.canCommit(callback);
        return callback;
    }

    public static CommitCohort immediatePreCommit(final CommitCohort cohort) {
        final CommitCallback<DataTreeCandidate> callback = mock();
        doNothing().when(callback).onSuccess(any(DataTreeCandidate.class));
        cohort.preCommit(callback);

        verify(callback).onSuccess(any(DataTreeCandidate.class));
        verifyNoMoreInteractions(callback);
        return cohort;
    }

    public static CommitCallback<DataTreeCandidate> coordinatedPreCommit(final CommitCohort cohort) {
        final CommitCallback<DataTreeCandidate> callback = mock();
        doNothing().when(callback).onSuccess(any(DataTreeCandidate.class));
        doNothing().when(callback).onFailure(any(Throwable.class));
        cohort.preCommit(callback);
        return callback;
    }

    public static CommitCohort immediateCommit(final CommitCohort cohort) {
        final CommitCallback<UnsignedLong> callback = mock();
        doNothing().when(callback).onSuccess(any(UnsignedLong.class));
        cohort.commit(callback);

        verify(callback, timeout(5000)).onSuccess(any(UnsignedLong.class));
        verifyNoMoreInteractions(callback);
        return cohort;
    }

    public static CommitCallback<UnsignedLong> coordinatedCommit(final CommitCohort cohort) {
        final CommitCallback<UnsignedLong> callback = mock();
        doNothing().when(callback).onSuccess(any(UnsignedLong.class));
        doNothing().when(callback).onFailure(any(Throwable.class));
        cohort.commit(callback);
        return callback;
    }

    public static CommitCallback<UnsignedLong> immediate3PhaseCommit(final CommitCohort cohort) {
        final CommitCallback<UnsignedLong> commitCallback = mock();
        doNothing().when(commitCallback).onSuccess(any(UnsignedLong.class));
        doNothing().when(commitCallback).onFailure(any(Throwable.class));

        final CommitCallback<DataTreeCandidate> preCommitCallback = mock();
        doAnswer(invocation -> {
            cohort.commit(commitCallback);
            return null;
        }).when(preCommitCallback).onSuccess(any(DataTreeCandidate.class));
        doNothing().when(preCommitCallback).onFailure(any(Throwable.class));

        final CommitCallback<Empty> canCommit = mock();
        doAnswer(invocation -> {
            cohort.preCommit(preCommitCallback);
            return null;
        }).when(canCommit).onSuccess(Empty.value());
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
    public static CommitCohort failedCanCommit(final CommitCohort mock) {
        doAnswer(ShardDataTreeMocking::invokeFailure).when(mock).canCommit(any(CommitCallback.class));
        return mock;
    }

    @SuppressWarnings("unchecked")
    public static CommitCohort failedPreCommit(final CommitCohort mock) {
        doAnswer(ShardDataTreeMocking::invokeFailure).when(mock).preCommit(any(CommitCallback.class));
        return mock;
    }

    @SuppressWarnings("unchecked")
    public static CommitCohort failedCommit(final CommitCohort mock) {
        doAnswer(ShardDataTreeMocking::invokeFailure).when(mock).commit(any(CommitCallback.class));
        return mock;
    }

    @SuppressWarnings("unchecked")
    public static CommitCohort successfulCanCommit(final CommitCohort mock) {
        doAnswer(invocation -> invokeSuccess(invocation, null)).when(mock).canCommit(any(CommitCallback.class));

        return mock;
    }

    public static CommitCohort successfulPreCommit(final CommitCohort mock) {
        return successfulPreCommit(mock, mock(DataTreeCandidate.class));
    }

    @SuppressWarnings("unchecked")
    public static CommitCohort successfulPreCommit(final CommitCohort mock, final DataTreeCandidate candidate) {
        doAnswer(invocation -> invokeSuccess(invocation, candidate)).when(mock).preCommit(any(CommitCallback.class));

        return mock;
    }

    public static CommitCohort successfulCommit(final CommitCohort mock) {
        return successfulCommit(mock, UnsignedLong.ZERO);
    }

    @SuppressWarnings("unchecked")
    public static CommitCohort successfulCommit(final CommitCohort mock, final UnsignedLong index) {
        doAnswer(invocation -> invokeSuccess(invocation, index)).when(mock).commit(any(CommitCallback.class));

        return mock;
    }

    @SuppressWarnings("unchecked")
    public static void assertSequencedCommit(final CommitCohort mock) {
        final var inOrder = inOrder(mock);
        inOrder.verify(mock).canCommit(any(CommitCallback.class));
        inOrder.verify(mock).preCommit(any(CommitCallback.class));
        inOrder.verify(mock).commit(any(CommitCallback.class));
    }

    public static void immediatePayloadReplication(final ShardDataTree shardDataTree, final Shard mockShard) {
        doAnswer(invocation -> {
            shardDataTree.applyReplicatedPayload(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(mockShard).submitCommand(any(TransactionIdentifier.class), any(CommitTransactionPayload.class),
                anyBoolean());
    }
}
