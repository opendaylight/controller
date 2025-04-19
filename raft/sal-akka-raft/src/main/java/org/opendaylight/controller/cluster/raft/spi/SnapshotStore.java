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
     * Serialize a {@link StateSnapshot} and make the result available as an {@link InstallableSnapshot} to the
     * specified callback.
     *
     * @param <T> the type of {@link StateSnapshot}
     * @param lastIncluded last included index/term
     * @param snapshot the snapshot
     * @param writer the writer to use
     * @param callback the callback to invoke
     */
    <T extends StateSnapshot> void streamToInstall(EntryInfo lastIncluded, T snapshot, StateSnapshot.Writer<T> writer,
        RaftCallback<InstallableSnapshot> callback);

    /**
     * Saves a snapshot asynchronously.
     *
     * @param <T> the type of {@link StateSnapshot}
     * @param raftSnapshot the {@link RaftSnapshot}, receiving the snapshot timestamp
     * @param lastIncluded last included index/term
     * @param snapshot the snapshot
     * @param writer the writer to use
     * @param callback the callback to invoke
     */
    // FIXME: imply async deletion of all other snapshots, only the last one will be reported
    <T extends StateSnapshot> void saveSnapshot(RaftSnapshot raftSnapshot, EntryInfo lastIncluded, T snapshot,
        StateSnapshot.Writer<T> writer, RaftCallback<Instant> callback);

    /**
     * Saves a snapshot synchronously. This method should only be called during recovery.
     *
     * @param <T> the type of {@link StateSnapshot}
     * @param raftSnapshot the {@link RaftSnapshot}
     * @param lastIncluded last included index/term
     * @param snapshot the snapshot
     * @param writer the writer to use
     * @param timestamp snapshot timestamp
     * @throws IOException when an I/O error occurs
     */
    <T extends StateSnapshot> void saveSnapshot(RaftSnapshot raftSnapshot, EntryInfo lastIncluded,
        T snapshot, StateSnapshot.Writer<T> writer, Instant timestamp) throws IOException;

    /**
     * Deletes snapshots up to and including a time.
     *
     * @param maxTimestamp the timestamp, in Epoch milliseconds
     */
    // FIXME: integrate into saveSnapshot()
    void deleteSnapshots(long maxTimestamp);

    /**
     * Receive and potentially handle a {@link SnapshotProtocol} response.
     *
     * @param response A {@link SnapshotProtocol} response
     * @return {@code true} if the response was handled
     */
    boolean handleSnapshotResponse(SnapshotProtocol.Response response);
}
