/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import java.io.IOException;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Internal interface towards storage entities.
 */
@NonNullByDefault
public abstract sealed class RaftStorage implements DataPersistenceProvider
        permits DisabledRaftStorage, EnabledRaftStorage {
    // FIXME: we should bave the concept of being 'open', when we have a thread pool to perform the asynchronous part
    //        of RaftActorSnapshotCohort.createSnapshot(), using virtual-thread-per-task

    // FIXME: this class should also be tracking the last snapshot bytes -- i.e. what AbstractLeader.SnapshotHolder.
    //        for file-based enabled storage, this means keeping track of the last snapshot we have. For disabled the
    //        case is similar, except we want to have a smarter strategy:
    //         - for small snapshots just use memory and throw them away as sson as unreferenced
    //         - for large snapshots keep them on disk even after they become unreferenced -- for some time, or journal
    //           activity.

    @Beta
    public abstract @Nullable SnapshotSource findLatestSnapshot() throws IOException;

    /**
     * Returns the member name associated with this storage.
     *
     * @return the member name associated with this storage
     */
    protected abstract String memberId();

    @Override
    public final String toString() {
        return addToStringAtrributes(MoreObjects.toStringHelper(this)).toString();
    }

    protected ToStringHelper addToStringAtrributes(final ToStringHelper helper) {
        return helper.add("memberId", memberId());
    }
}
