/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.List;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.spi.RaftEntryMeta;
import org.opendaylight.controller.cluster.raft.spi.TermInfo;
import org.opendaylight.yangtools.concepts.Immutable;

/**
 * Represents a snapshot of the raft data.
 *
 * @author Thomas Pantelis
 */
public final class Snapshot implements Serializable {
    /**
     * Implementations of this interface are used as the state payload for a snapshot.
     *
     * @author Thomas Pantelis
     */
    public interface State extends Immutable, Serializable {
        /**
         * Indicate whether the snapshot requires migration, i.e. a new snapshot should be created after recovery.
         * Default implementation returns false, i.e. do not re-snapshot.
         *
         * @return True if complete recovery based upon this snapshot should trigger a new snapshot.
         */
        default boolean needsMigration() {
            return false;
        }
    }

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final State state;
    private final List<ReplicatedLogEntry> unAppliedEntries;
    private final long lastIndex;
    private final long lastTerm;
    private final long lastAppliedIndex;
    private final long lastAppliedTerm;
    private final @NonNull TermInfo termInfo;
    private final ClusterConfig serverConfig;

    private Snapshot(final State state, final List<ReplicatedLogEntry> unAppliedEntries, final long lastIndex,
            final long lastTerm, final long lastAppliedIndex, final long lastAppliedTerm, final TermInfo termInfo,
            final ClusterConfig serverConfig) {
        this.state = state;
        this.unAppliedEntries = unAppliedEntries;
        this.lastIndex = lastIndex;
        this.lastTerm = lastTerm;
        this.lastAppliedIndex = lastAppliedIndex;
        this.lastAppliedTerm = lastAppliedTerm;
        this.termInfo = requireNonNull(termInfo);
        this.serverConfig = serverConfig;
    }

    public static Snapshot create(final State state, final List<ReplicatedLogEntry> entries, final long lastIndex,
            final long lastTerm, final long lastAppliedIndex, final long lastAppliedTerm, final TermInfo termInfo,
            final ClusterConfig serverConfig) {
        return new Snapshot(state, entries, lastIndex, lastTerm, lastAppliedIndex, lastAppliedTerm, termInfo,
            serverConfig);
    }

    public static Snapshot ofTermLeader(final State state, final RaftEntryMeta lastIncluded, final TermInfo termInfo,
            final ClusterConfig serverConfig) {
        return new Snapshot(state, List.of(), lastIncluded.index(), lastIncluded.term(),
            lastIncluded.index(), lastIncluded.term(), termInfo, serverConfig);
    }

    public State getState() {
        return state;
    }

    public List<ReplicatedLogEntry> getUnAppliedEntries() {
        return unAppliedEntries;
    }

    public long getLastTerm() {
        return lastTerm;
    }

    public long getLastAppliedIndex() {
        return lastAppliedIndex;
    }

    public long getLastAppliedTerm() {
        return lastAppliedTerm;
    }

    public long getLastIndex() {
        return lastIndex;
    }

    public @NonNull TermInfo termInfo() {
        return termInfo;
    }

    @Deprecated
    public long getElectionTerm() {
        return termInfo.term();
    }

    @Deprecated
    public String getElectionVotedFor() {
        return termInfo.votedFor();
    }

    public ClusterConfig getServerConfiguration() {
        return serverConfig;
    }

    @Override
    public String toString() {
        return "Snapshot [lastIndex=" + lastIndex + ", lastTerm=" + lastTerm + ", lastAppliedIndex=" + lastAppliedIndex
                + ", lastAppliedTerm=" + lastAppliedTerm + ", unAppliedEntries size=" + unAppliedEntries.size()
                + ", state=" + state + ", electionTerm=" + termInfo.term() + ", electionVotedFor="
                + termInfo.votedFor() + ", ServerConfigPayload="  + serverConfig + "]";
    }

    @java.io.Serial
    private Object writeReplace() {
        return new SS(this);
    }
}
