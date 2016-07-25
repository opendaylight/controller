/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import static org.mockito.Matchers.any;
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
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;

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

    public static ShardDataTreeCohort immediatePreCommit(final ShardDataTreeCohort cohort) {
        final FutureCallback<DataTreeCandidate> callback = mockCallback();
        doNothing().when(callback).onSuccess(any(DataTreeCandidate.class));
        cohort.preCommit(callback);

        verify(callback).onSuccess(any(DataTreeCandidate.class));
        verifyNoMoreInteractions(callback);
        return cohort;
    }

    public static ShardDataTreeCohort immediateCommit(final ShardDataTreeCohort cohort) {
        final FutureCallback<UnsignedLong> callback = mockCallback();
        doNothing().when(callback).onSuccess(any(UnsignedLong.class));
        cohort.commit(callback);

        verify(callback, timeout(5000)).onSuccess(any(UnsignedLong.class));
        verifyNoMoreInteractions(callback);
        return cohort;
    }

    @SuppressWarnings("unchecked")
    private static <T> Object invokeSuccess(final InvocationOnMock invocation, final T value) {
        invocation.getArgumentAt(0, FutureCallback.class).onSuccess(value);
        return null;
    }

    private static Object invokeFailure(final InvocationOnMock invocation) {
        invocation.getArgumentAt(0, FutureCallback.class).onFailure(mock(Exception.class));
        return null;
    }

    @SuppressWarnings("unchecked")
    public static ShardDataTreeCohort failedCanCommit(final ShardDataTreeCohort mock) {
        doAnswer(invocation -> {
            return invokeFailure(invocation);
        }).when(mock).canCommit(any(FutureCallback.class));
        return mock;
    }

    @SuppressWarnings("unchecked")
    public static ShardDataTreeCohort failedPreCommit(final ShardDataTreeCohort mock) {
        doAnswer(invocation -> {
            return invokeFailure(invocation);
        }).when(mock).preCommit(any(FutureCallback.class));
        return mock;
    }

    @SuppressWarnings("unchecked")
    public static ShardDataTreeCohort failedCommit(final ShardDataTreeCohort mock) {
        doAnswer(invocation -> {
            return invokeFailure(invocation);
        }).when(mock).commit(any(FutureCallback.class));
        return mock;
    }

    @SuppressWarnings("unchecked")
    public static ShardDataTreeCohort successfulCanCommit(final ShardDataTreeCohort mock) {
        doAnswer(invocation -> {
            return invokeSuccess(invocation, null);
        }).when(mock).canCommit(any(FutureCallback.class));

        return mock;
    }

    public static ShardDataTreeCohort successfulPreCommit(final ShardDataTreeCohort mock) {
        return successfulPreCommit(mock, mock(DataTreeCandidate.class));
    }

    @SuppressWarnings("unchecked")
    public static ShardDataTreeCohort successfulPreCommit(final ShardDataTreeCohort mock, final DataTreeCandidate candidate) {
        doAnswer(invocation -> {
            return invokeSuccess(invocation, candidate);
        }).when(mock).preCommit(any(FutureCallback.class));

        return mock;
    }

    public static ShardDataTreeCohort successfulCommit(final ShardDataTreeCohort mock) {
        return successfulCommit(mock, UnsignedLong.ZERO);
    }

    @SuppressWarnings("unchecked")
    public static ShardDataTreeCohort successfulCommit(final ShardDataTreeCohort mock, final UnsignedLong index) {
        doAnswer(invocation -> {
            return invokeSuccess(invocation, index);
        }).when(mock).commit(any(FutureCallback.class));

        return mock;
    }

    @SuppressWarnings("unchecked")
    public static void assertSequencedCommit(final ShardDataTreeCohort mock) {
        final InOrder inOrder = inOrder(mock);
        inOrder.verify(mock).canCommit(any(FutureCallback.class));
        inOrder.verify(mock).preCommit(any(FutureCallback.class));
        inOrder.verify(mock).commit(any(FutureCallback.class));
    }
}
