/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import java.io.IOException;
import java.time.Instant;
import org.apache.pekko.persistence.SnapshotProtocol;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.spi.StateSnapshot.ToStorage;
import org.opendaylight.raft.api.EntryInfo;
import org.opendaylight.raft.spi.InstallableSnapshot;

/**
 * Interface to a access and manage {@link SnapshotFile}s.
 */
@NonNullByDefault
public interface SnapshotStore {
    /**
     * Returns the last available snapshot.
     *
     * @return the last available snapshot
     * @throws IOException if an I/O error occurs
     */
    @Nullable SnapshotFile lastSnapshot() throws IOException;

    /**
     * Serialize a {@link ToStorage} snapshot and make the result available as an {@link InstallableSnapshot} to the
     * specified callback.
     *
     * @param lastIncluded last included index/term
     * @param snapshot the snapshot
     * @param callback the callback to invoke
     */
    void streamToInstall(EntryInfo lastIncluded, ToStorage<?> snapshot, RaftCallback<InstallableSnapshot> callback);

    /**
     * Saves a snapshot asynchronously.
     *
     * @param raftSnapshot the {@link RaftSnapshot}, receiving the snapshot timestamp
     * @param lastIncluded last included index/term
     * @param snapshot the snapshot, or {@code null} if not applicable
     * @param callback the callback to invoke
     */
    // FIXME: imply async deletion of all other snapshots, only the last one will be reported
    void saveSnapshot(RaftSnapshot raftSnapshot, EntryInfo lastIncluded, @Nullable ToStorage<?> snapshot,
        RaftCallback<Instant> callback);

    /**
     * Saves a snapshot synchronously. This method should only be called during recovery.
     *
     * @param raftSnapshot the {@link RaftSnapshot}
     * @param lastIncluded last included index/term
     * @param snapshot the snapshot, or {@code null} if not applicable
     * @param timestamp snapshot timestamp
     * @throws IOException when an I/O error occurs
     */
    void saveSnapshot(RaftSnapshot raftSnapshot, EntryInfo lastIncluded, @Nullable ToStorage<?> snapshot,
        Instant timestamp) throws IOException;

    /**
     * Deletes all snapshots older than a timestamp.
     *
     * @param firstRetained the cut-off timestamp
     */
    // FIXME: integrate into saveSnapshot()
    void retainSnapshots(Instant firstRetained);

    /**
     * Receive and potentially handle a {@link SnapshotProtocol} response.
     *
     * @param response A {@link SnapshotProtocol} response
     * @return {@code true} if the response was handled
     */
    boolean handleSnapshotResponse(SnapshotProtocol.Response response);
}
