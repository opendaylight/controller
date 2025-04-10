/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import java.util.function.Consumer;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.spi.LogEntry;
import org.opendaylight.raft.api.EntryMeta;

final class MockReplicatedLog extends AbstractReplicatedLog {
    MockReplicatedLog() {
        super("");
    }

    @Override
    public boolean removeFromAndPersist(final long index) {
        return true;
    }

    @Override
    public boolean appendReceived(final ReplicatedLogEntry entry, final Consumer<LogEntry> callback) {
        if (callback != null) {
            callback.accept(entry);
        }
        return false;
    }

    @Override
    void appendSubmitted(final SimpleReplicatedLogEntry entry, final Consumer<LogEntry> callback) {
        if (callback != null) {
            entry.setPersistencePending(false);
            callback.accept(entry);
        }
    }

    @Override
    public void captureSnapshotIfReady(final EntryMeta replicatedLogEntry) {
        // No-op
    }

    @Override
    public boolean shouldCaptureSnapshot(final long logIndex) {
        return false;
    }
}