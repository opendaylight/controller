/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.base.messages;

import java.util.List;
import org.apache.pekko.dispatch.ControlMessage;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.spi.ImmutableRaftEntryMeta;
import org.opendaylight.controller.cluster.raft.spi.RaftEntryMeta;

public final class CaptureSnapshot implements ControlMessage {
    private final ImmutableRaftEntryMeta lastApplied;
    private final long lastIndex;
    private final long lastTerm;
    private final long replicatedToAllIndex;
    private final long replicatedToAllTerm;
    private final List<ReplicatedLogEntry> unAppliedEntries;
    private final boolean mandatoryTrim;

    public CaptureSnapshot(final long lastIndex, final long lastTerm, final @Nullable RaftEntryMeta lastApplied,
            final long replicatedToAllIndex, final long replicatedToAllTerm,
            final List<ReplicatedLogEntry> unAppliedEntries, final boolean mandatoryTrim) {
        this.lastIndex = lastIndex;
        this.lastTerm = lastTerm;
        this.lastApplied = ImmutableRaftEntryMeta.ofNullable(lastApplied);
        this.replicatedToAllIndex = replicatedToAllIndex;
        this.replicatedToAllTerm = replicatedToAllTerm;
        this.unAppliedEntries = unAppliedEntries != null ? unAppliedEntries : List.of();
        this.mandatoryTrim = mandatoryTrim;
    }

    public @Nullable ImmutableRaftEntryMeta lastApplied() {
        return lastApplied;
    }

    public long getLastIndex() {
        return lastIndex;
    }

    public long getLastTerm() {
        return lastTerm;
    }

    public long getReplicatedToAllIndex() {
        return replicatedToAllIndex;
    }

    public long getReplicatedToAllTerm() {
        return replicatedToAllTerm;
    }

    public List<ReplicatedLogEntry> getUnAppliedEntries() {
        return unAppliedEntries;
    }

    public boolean isMandatoryTrim() {
        return mandatoryTrim;
    }

    @Override
    public String toString() {
        final long lastAppliedIndex;
        final long lastAppliedTerm;
        final var local = lastApplied;
        if (local != null) {
            lastAppliedIndex = local.index();
            lastAppliedTerm = local.term();
        } else {
            lastAppliedIndex = lastAppliedTerm = -1;
        }

        return "CaptureSnapshot [lastAppliedIndex=" + lastAppliedIndex
                + ", lastAppliedTerm=" + lastAppliedTerm
                + ", lastIndex=" + lastIndex
                + ", lastTerm=" + lastTerm
                + ", installSnapshotInitiated="
                + ", replicatedToAllIndex=" + replicatedToAllIndex
                + ", replicatedToAllTerm=" + replicatedToAllTerm
                + ", unAppliedEntries size=" + unAppliedEntries.size()
                + ", mandatoryTrim=" + mandatoryTrim + "]";
    }
}
