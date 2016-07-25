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
import static org.mockito.Mockito.mock;
import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.FutureCallback;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;

public final class ShardDataTreeMocking {

    private ShardDataTreeMocking() {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    public static ShardDataTreeCohort successfulCanCommit(final ShardDataTreeCohort mock) {
        doAnswer(invocation -> {
            invocation.getArgumentAt(0, FutureCallback.class).onSuccess(null);
            return null;
        }).when(mock).canCommit(any(FutureCallback.class));

        return mock;
    }

    public static ShardDataTreeCohort successfulPreCommit(final ShardDataTreeCohort mock) {
        return successfulPreCommit(mock, mock(DataTreeCandidate.class));
    }

    @SuppressWarnings("unchecked")
    public static ShardDataTreeCohort successfulPreCommit(final ShardDataTreeCohort mock, final DataTreeCandidate candidate) {
        doAnswer(invocation -> {
            invocation.getArgumentAt(0, FutureCallback.class).onSuccess(candidate);
            return null;
        }).when(mock).preCommit(any(FutureCallback.class));

        return mock;
    }

    public static ShardDataTreeCohort successfulCommit(final ShardDataTreeCohort mock) {
        return successfulCommit(mock, UnsignedLong.ZERO);
    }

    @SuppressWarnings("unchecked")
    public static ShardDataTreeCohort successfulCommit(final ShardDataTreeCohort mock, final UnsignedLong index) {
        doAnswer(invocation -> {
            invocation.getArgumentAt(0, FutureCallback.class).onSuccess(index);
            return null;
        }).when(mock).commit(any(FutureCallback.class));

        return mock;
    }
}
