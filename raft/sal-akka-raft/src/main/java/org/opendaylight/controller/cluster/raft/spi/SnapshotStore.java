/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import java.io.IOException;
import org.apache.pekko.persistence.SnapshotProtocol;
import org.apache.pekko.persistence.SnapshotSelectionCriteria;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.RaftActor;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
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

    /**
     * Returns the last available snapshot.
     *
     * @return the last available snapshot
     * @throws IOException if an I/O error occurs
     */
    @Nullable SnapshotFile lastSnapshot() throws IOException;

    /**
     * Serialize a {@link StateSnapshot} and make the result available as an {@link InstallableSnapshot} to the
     * specified callback.
     *
     * @param <T> the type of {@link StateSnapshot}
     * @param lastIncluded last included index/term
     * @param snapshot the snapshot
     * @param writer the writer to use
     * @param callback the callback to invoke
     */
    @NonNullByDefault
    <T extends StateSnapshot> void streamToInstall(EntryInfo lastIncluded, T snapshot, StateSnapshot.Writer<T> writer,
        Callback<InstallableSnapshot> callback);

    /**
     * Saves a snapshot.
     *
     * @param snapshot the snapshot object to save
     */
    // FIXME: Callback<SnapshotFile> callback
    void saveSnapshot(@NonNull Snapshot snapshot);

    //  @NonNullByDefault
    //  <T extends StateSnapshot> void storeSnapshot(T snapshot, StateSnapshot.Writer<T> writer,
    //      Callback<SnapshotFile> callback);

    /**
     * Deletes snapshots based on the given criteria.
     *
     * @param criteria the search criteria
     */
    // FIXME: criteria == max size? max snapshots?
    // FIXME: throws IOException
    void deleteSnapshots(@NonNull SnapshotSelectionCriteria criteria);

    /**
     * Receive and potentially handle a {@link SnapshotProtocol} response.
     *
     * @param response A {@link SnapshotProtocol} response
     * @return {@code true} if the response was handled
     */
    boolean handleSnapshotResponse(SnapshotProtocol.@NonNull Response response);
}
