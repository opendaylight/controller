/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.datastore;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeCandidateTip;
import org.opendaylight.yangtools.yang.data.api.schema.tree.DataTreeModification;

public abstract class ShardDataTreeCohort {
    ShardDataTreeCohort() {
        // Prevent foreign instantiation
    }

    abstract DataTreeCandidateTip getCandidate();
    abstract DataTreeModification getDataTreeModification();

    @VisibleForTesting
    public abstract ListenableFuture<Boolean> canCommit();
    @VisibleForTesting
    public abstract ListenableFuture<Void> preCommit();
    @VisibleForTesting
    public abstract ListenableFuture<Void> abort();
    @VisibleForTesting
    public abstract ListenableFuture<Void> commit();
}
