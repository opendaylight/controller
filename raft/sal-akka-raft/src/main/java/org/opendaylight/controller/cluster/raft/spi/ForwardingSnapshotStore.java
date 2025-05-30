/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import com.google.common.base.MoreObjects;
import java.io.IOException;
import java.time.Instant;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.spi.StateSnapshot.ToStorage;
import org.opendaylight.raft.api.EntryInfo;
import org.opendaylight.raft.spi.InstallableSnapshot;

@NonNullByDefault
public abstract class ForwardingSnapshotStore implements SnapshotStore {

    protected abstract SnapshotStore delegate();

    @Override
    public @Nullable SnapshotFile lastSnapshot() throws IOException {
        return delegate().lastSnapshot();
    }

    @Override
    public void saveSnapshot(final RaftSnapshot raftSnapshot, final EntryInfo lastIncluded,
            final @Nullable ToStorage<?> snapshot, final RaftCallback<Instant> callback) {
        delegate().saveSnapshot(raftSnapshot, lastIncluded, snapshot, callback);
    }

    @Override
    public void saveSnapshot(final RaftSnapshot raftSnapshot, final EntryInfo lastIncluded,
            final @Nullable ToStorage<?> snapshot, final Instant timestamp) throws IOException {
        delegate().saveSnapshot(raftSnapshot, lastIncluded, snapshot, timestamp);
    }

    @Override
    public void streamToInstall(final EntryInfo lastIncluded, final ToStorage<?> snapshot,
            final RaftCallback<InstallableSnapshot> callback) {
        delegate().streamToInstall(lastIncluded, snapshot, callback);
    }

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(this).add("delegate", delegate()).toString();
    }
}
