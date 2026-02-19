/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import java.util.function.Consumer;
import org.opendaylight.controller.cluster.raft.messages.Payload;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.spi.LogEntry;
import org.opendaylight.raft.api.EntryMeta;

final class MockReplicatedLog extends AbstractReplicatedLog<ReplicatedLogEntry> {
    MockReplicatedLog() {
        super("");
    }

    @Override
    public boolean trimToReceive(final long fromIndex) {
        return true;
    }

    @Override
    public boolean appendReceived(final LogEntry entry, final Consumer<LogEntry> callback) {
        if (callback != null) {
            callback.accept(entry);
        }
        return false;
    }

    @Override
    public boolean appendSubmitted(final long index, final long term, final Payload command,
            final Consumer<ReplicatedLogEntry> callback) {
        if (callback != null) {
            callback.accept(new SimpleReplicatedLogEntry(index, term, command));
        }
        return true;
    }

    @Override
    public void markLastApplied() {
        // No-op
    }

    @Override
    public void captureSnapshotIfReady(final EntryMeta lastEntry) {
        // No-op
    }

    @Override
    public boolean shouldCaptureSnapshot(final long logIndex) {
        return false;
    }

    @Override
    protected ReplicatedLogEntry adoptEntry(final LogEntry entry) {
        return SimpleReplicatedLogEntry.of(entry);
    }
}
