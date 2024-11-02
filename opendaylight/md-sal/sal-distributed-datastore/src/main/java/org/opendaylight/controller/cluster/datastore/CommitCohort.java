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
import java.util.SortedSet;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.access.concepts.TransactionIdentifier;

// Non-sealed for mocking
@VisibleForTesting
public abstract class CommitCohort {

    private final @Nullable SortedSet<String> participatingShardNames;

    private long lastAccess;

    CommitCohort(final @Nullable SortedSet<String> participatingShardNames) {
        this.participatingShardNames = participatingShardNames;
    }

    abstract @NonNull TransactionIdentifier transactionId();

    final long lastAccess() {
        return lastAccess;
    }

    final void setLastAccess(final long lastAccess) {
        this.lastAccess = lastAccess;
    }

    final @Nullable SortedSet<String> participatingShardNames() {
        return participatingShardNames;
    }

    abstract FutureCallback<UnsignedLong> wrapCommitCallback(FutureCallback<UnsignedLong> callback);
}
