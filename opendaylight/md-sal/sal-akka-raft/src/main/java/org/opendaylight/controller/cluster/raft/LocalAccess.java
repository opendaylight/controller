/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.DataPersistenceProvider;
import org.opendaylight.controller.cluster.raft.spi.TermInfoStore;

/**
 * The concept of {@link RaftActor}'s access to its local state.
 */
@NonNullByDefault
@VisibleForTesting
public final class LocalAccess {
    private final String logId;
    private final TermInfoStore termInfoStore;

    @VisibleForTesting
    public LocalAccess(final String logId, final TermInfoStore termInfoStore) {
        this.logId = requireNonNull(logId);
        this.termInfoStore = requireNonNull(termInfoStore);
    }

    @VisibleForTesting
    LocalAccess(final String logId, final DataPersistenceProvider persistence) {
        this(logId, new PersistenceTermInfoStore(persistence, logId));
    }

    String logId() {
        return logId;
    }

    TermInfoStore termInfoStore() {
        return termInfoStore;
    }

    // FIXME: pretty much all of ReplicatedLog storage access, but not the indexing bits (at least not for now).
    //        Notable mentions:
    //          - receive leader-sourced snapshots into local store for use in Candidate, one at a time, allowing them
    //            to be atomically adopted to as local baseline snapshot
    //          - differentiated 'snapshot' and 'backup' operations?
    // FIXME: storage side of raft journal

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("logId", logId).add("termInfo", termInfoStore).toString();
    }
}
