/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Internal interface towards storage entities.
 */
@NonNullByDefault
public abstract sealed class RaftStorage implements DataPersistenceProvider
        permits DisabledRaftStorage, EnabledRaftStorage {
    // Nothing else for now, but there is lot more to come.
    // FIXME: we should bave the concept of being 'open', when we have a thread pool to perform the asynchronous part
    //        of RaftActorSnapshotCohort.createSnapshot(), using virtual-thread-per-task

    // FIXME: this class should also be tracking the last snapshot bytes -- i.e. what AbstractLeader.SnapshotHolder.
    //        for file-based enabled storage, this means keeping track of the last snapshot we have. For disabled the
    //        case is similar, except we want to have a smarter strategy:
    //         - for small snapshots just use memory and throw them away as sson as unreferenced
    //         - for large snapshots keep them on disk even after they become unreferenced -- for some time, or journal
    //           activity.
}
