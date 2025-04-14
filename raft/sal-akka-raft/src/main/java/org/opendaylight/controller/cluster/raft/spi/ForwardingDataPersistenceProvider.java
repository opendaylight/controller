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
import java.util.function.Consumer;
import org.apache.pekko.persistence.JournalProtocol;
import org.apache.pekko.persistence.SnapshotProtocol;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot;
import org.opendaylight.raft.api.EntryInfo;
import org.opendaylight.raft.spi.InstallableSnapshot;

@NonNullByDefault
public abstract class ForwardingDataPersistenceProvider implements DataPersistenceProvider {

    protected abstract DataPersistenceProvider delegate();

    @Override
    public boolean isRecoveryApplicable() {
        return delegate().isRecoveryApplicable();
    }

    @Override
    public @Nullable SnapshotFile lastSnapshot() throws IOException {
        return delegate().lastSnapshot();
    }

    @Override
    public void persistEntry(final ReplicatedLogEntry entry, final Consumer<ReplicatedLogEntry> callback) {
        delegate().persistEntry(entry, callback);
    }

    @Override
    public void deleteEntries(final long fromIndex) {
        delegate().deleteEntries(fromIndex);
    }

    @Override
    public <T> void persistAsync(final T entry, final Consumer<T> callback) {
        delegate().persistAsync(entry, callback);
    }

    @Override
    public void saveSnapshot(final Snapshot entry) {
        delegate().saveSnapshot(entry);
    }

    @Override
    public <T extends StateSnapshot> void streamToInstall(final EntryInfo lastIncluded, final T snapshot,
            final StateSnapshot.Writer<T> writer, final Callback<InstallableSnapshot> callback) {
        delegate().streamToInstall(lastIncluded, snapshot, writer, callback);
    }

    @Override
    public void deleteSnapshots(final long maxTimestamp) {
        delegate().deleteSnapshots(maxTimestamp);
    }

    @Override
    public void deleteMessages(final long sequenceNumber) {
        delegate().deleteMessages(sequenceNumber);
    }

    @Override
    public long getLastSequenceNumber() {
        return delegate().getLastSequenceNumber();
    }

    @Override
    public boolean handleJournalResponse(final JournalProtocol.Response response) {
        return delegate().handleJournalResponse(response);
    }

    @Override
    public boolean handleSnapshotResponse(final SnapshotProtocol.Response response) {
        return delegate().handleSnapshotResponse(response);
    }

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(this).add("delegate", delegate()).toString();
    }
}
