/*
 * Copyright (c) 2025 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.spi;

import java.util.List;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.persisted.VotingConfig;

/**
 * Atomic information retained in a snapshot file. Unapplied entries are those that have been known to have been
 * stored in the journal -- which allows for transitioning from non-persistent to persistent state.
 */
// FIXME: remove unappliedEntries once we remove PekkoRecovery
@NonNullByDefault
public record RaftSnapshot(@Nullable VotingConfig votingConfig, List<LogEntry> unappliedEntries) {
    public RaftSnapshot {
        unappliedEntries = List.copyOf(unappliedEntries);
    }

    public RaftSnapshot(@Nullable VotingConfig votingConfig) {
        this(votingConfig, List.of());
    }
}
