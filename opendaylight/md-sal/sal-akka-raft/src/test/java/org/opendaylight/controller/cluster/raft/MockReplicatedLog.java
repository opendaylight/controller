/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import java.util.List;
import java.util.function.Consumer;
import org.opendaylight.controller.cluster.raft.spi.RaftEntryMeta;

final class MockReplicatedLog extends AbstractReplicatedLog {
    MockReplicatedLog() {
        super("", null, List.of());
    }

    @Override
    public boolean removeFromAndPersist(final long index) {
        return true;
    }

    @Override
    public <T extends ReplicatedLogEntry> boolean appendAndPersist(final T replicatedLogEntry,
            final Consumer<T> callback, final boolean doAsync) {
        if (callback != null) {
            callback.accept(replicatedLogEntry);
        }
        return true;
    }

    @Override
    public void captureSnapshotIfReady(final RaftEntryMeta replicatedLogEntry) {
        // No-op
    }

    @Override
    public boolean shouldCaptureSnapshot(final long logIndex) {
        return false;
    }
}