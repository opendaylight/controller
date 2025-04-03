/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import java.io.IOException;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.RaftActor;
import org.opendaylight.raft.api.EntryInfo;
import org.opendaylight.raft.spi.InstallableSnapshot;

/**
 * Interface to a access and manage {@link SnapshotFile}s.
 */
public interface SnapshotStore {
    /**
     * A simple callback interface. Guaranteed to be invoked in {@link RaftActor} confinement.
     *
     * @param <T> type of successful result
     */
    // TODO: generalize?
    @FunctionalInterface
    interface Callback<T> {

        void invoke(Exception failure, T success);
    }

    @Nullable SnapshotFile lastSnapshot() throws IOException;

//    @NonNullByDefault
//    <T extends StateSnapshot> void storeSnapshot(T snapshot, StateSnapshot.Writer<T> writer,
//        Callback<SnapshotFile> callback);

    @NonNullByDefault
    <T extends StateSnapshot> void streamToInstall(EntryInfo lastIncluded, T snapshot, StateSnapshot.Writer<T> writer,
        Callback<InstallableSnapshot> callback);
}
