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
import java.util.List;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.persisted.ClusterConfig;

/**
 * Atomic information retained in a snapshot file. Unapplied entries are those that have been known to have been
 * stored in the journal -- which allows for transitioning from non-persistent to persistent state.
 */
@Beta
public record RaftSnapshot(ClusterConfig clusterConfig, List<ReplicatedLogEntry> unappliedEntries) {
    public RaftSnapshot {
        requireNonNull(clusterConfig);
        unappliedEntries = List.copyOf(unappliedEntries);
    }
}