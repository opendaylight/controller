/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import java.util.function.Consumer;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.messages.Payload;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.spi.LogEntry;
import org.opendaylight.raft.api.EntryMeta;

/**
 * A {@link ReplicatedLog} used during {@link PekkoRecovery}.
 */
@NonNullByDefault
final class PekkoReplicatedLog extends AbstractReplicatedLog<SimpleReplicatedLogEntry> {
    PekkoReplicatedLog(final String memberId) {
        super(memberId);
    }

    /**
     * Removes entries from the in-memory log starting at the given index. This method exists only to deal with the
     * effects of {@link #trimToReceive(long)} with Pekko Persistence.
     *
     * @param fromIndex the index of the first log entry to remove
     * @return the adjusted index of the first log entry removed or -1 if the log entry is not found.
     */
    long removeRecoveredEntries(final long fromIndex) {
        return removeFrom(fromIndex);
    }

    @Override
    public void markLastApplied() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean trimToReceive(final long nextIndex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean appendReceived(final LogEntry entry, final @Nullable Consumer<LogEntry> callback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean appendSubmitted(final long index, final long term, final Payload command,
            final Consumer<ReplicatedLogEntry> callback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void captureSnapshotIfReady(final EntryMeta lastEntry) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean shouldCaptureSnapshot(final long logIndex) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected SimpleReplicatedLogEntry adoptEntry(final LogEntry entry) {
        return SimpleReplicatedLogEntry.of(entry);
    }
}
