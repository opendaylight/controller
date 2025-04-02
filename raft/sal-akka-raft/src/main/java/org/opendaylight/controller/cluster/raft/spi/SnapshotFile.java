/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.Beta;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.persisted.ClusterConfig;
import org.opendaylight.raft.spi.InstallableSnapshot;

/**
 * Access to the contents of a RAFT snapshot file.
 */
@Beta
@NonNullByDefault
public interface SnapshotFile extends InstallableSnapshot {
    /**
     * Atomic information retained in a snapshot file. Unapplied entries are those that have been known to have been
     * stored in the journal -- which allows for transitioning from non-persistent to persistent state.
     */
    record ServerState(ClusterConfig clusterConfig, List<ReplicatedLogEntry> unappliedEntries) {
        public ServerState {
            requireNonNull(clusterConfig);
            unappliedEntries = List.copyOf(unappliedEntries);
        }
    }

    Instant timestamp();

    ServerState readServerState() throws IOException;
}
