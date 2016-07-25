/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.UnsignedLong;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;
import org.opendaylight.yangtools.concepts.Identifiable;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidate;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateTip;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;

public abstract class ShardDataTreeCohort implements Identifiable<TransactionIdentifier> {
    public enum State {
        READY,
        CAN_COMMIT_PENDING,
        CAN_COMMIT_COMPLETE,
        PRE_COMMIT_PENDING,
        PRE_COMMIT_COMPLETE,
        COMMIT_PENDING,

        ABORTED,
        COMMITTED,
        FAILED,
    }

    ShardDataTreeCohort() {
        // Prevent foreign instantiation
    }

    // FIXME: This leaks internal state generated in preCommit,
    // should be result of canCommit
    abstract DataTreeCandidateTip getCandidate();

    abstract DataTreeModification getDataTreeModification();

    // FIXME: Should return rebased DataTreeCandidateTip
    @VisibleForTesting
    public abstract void canCommit(FutureCallback<Void> callback);

    @VisibleForTesting
    public abstract void preCommit(FutureCallback<DataTreeCandidate> callback);

    @VisibleForTesting
    public abstract ListenableFuture<Void> abort();

    @VisibleForTesting
    public abstract void commit(FutureCallback<UnsignedLong> callback);

    public abstract boolean isFailed();

    public abstract State getState();
}
