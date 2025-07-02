/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.spi.RaftCallback;
import org.opendaylight.controller.cluster.raft.spi.RaftSnapshot;
import org.opendaylight.controller.cluster.raft.spi.RaftStorageCompleter;
import org.opendaylight.controller.cluster.raft.spi.SnapshotFile;
import org.opendaylight.controller.cluster.raft.spi.SnapshotStore;
import org.opendaylight.controller.cluster.raft.spi.StateSnapshot.ToStorage;
import org.opendaylight.raft.api.EntryInfo;
import org.opendaylight.raft.spi.ByteArray;
import org.opendaylight.raft.spi.InstallableSnapshot;
import org.opendaylight.raft.spi.InstallableSnapshotSource;
import org.opendaylight.raft.spi.PlainSnapshotSource;

/**
 * An immediate {@link SnapshotStore}. Offloads asynchronous persist responses via {@link RaftStorageCompleter}
 * exposed via {@link #completer()}.
 */
@NonNullByDefault
interface ImmediateSnapshotStore extends SnapshotStore {

    RaftStorageCompleter completer();

    @Override
    default @Nullable SnapshotFile lastSnapshot() throws IOException {
        return null;
    }

    @Override
    default void saveSnapshot(final RaftSnapshot raftSnapshot, final EntryInfo lastIncluded,
            final @Nullable ToStorage<?> snapshot, final RaftCallback<Instant> callback) {
        // no-op
    }

    @Override
    default void saveSnapshot(final RaftSnapshot raftSnapshot, final EntryInfo lastIncluded,
            final @Nullable ToStorage<?> snapshot, final Instant timestamp) {
        // no-op
    }

    @Override
    default void streamToInstall(final EntryInfo lastIncluded, final ToStorage<?> snapshot,
            final RaftCallback<InstallableSnapshot> callback) {
        final byte[] bytes;
        try (var baos = new ByteArrayOutputStream()) {
            snapshot.writeTo(baos);
            bytes = baos.toByteArray();
        } catch (IOException e) {
            completer().enqueueCompletion(() -> callback.invoke(e, null));
            return;
        }

        final var result = new InstallableSnapshotSource(lastIncluded, new PlainSnapshotSource(ByteArray.wrap(bytes)));
        completer().enqueueCompletion(() -> callback.invoke(null, result));
    }
}
