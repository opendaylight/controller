/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import static java.util.Objects.requireNonNull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.List;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.spi.LogEntry;
import org.opendaylight.controller.cluster.raft.spi.RaftSnapshot;
import org.opendaylight.controller.cluster.raft.spi.StateSnapshot;
import org.opendaylight.raft.api.EntryInfo;
import org.opendaylight.raft.api.EntryMeta;
import org.opendaylight.raft.api.TermInfo;

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
    public interface State extends StateSnapshot, Serializable {
        // Nothing else
    }

    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final @Nullable State state;
    @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "Handled through serialization proxy")
    private final @NonNull List<@NonNull LogEntry> unAppliedEntries;
    private final long lastIndex;
    private final long lastTerm;
    private final long lastAppliedIndex;
    private final long lastAppliedTerm;
    @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "Handled through serialization proxy")
    private final @NonNull TermInfo termInfo;
    private final @Nullable VotingConfig votingConfig;

    @NonNullByDefault
    private Snapshot(final @Nullable State state, final List<LogEntry> unAppliedEntries,
            final long lastIndex, final long lastTerm, final long lastAppliedIndex, final long lastAppliedTerm,
            final TermInfo termInfo, final @Nullable VotingConfig votingConfig) {
        this.state = state;
        this.unAppliedEntries = requireNonNull(unAppliedEntries);
        this.lastIndex = lastIndex;
        this.lastTerm = lastTerm;
        this.lastAppliedIndex = lastAppliedIndex;
        this.lastAppliedTerm = lastAppliedTerm;
        this.termInfo = requireNonNull(termInfo);
        this.votingConfig = votingConfig;
    }

    @NonNullByDefault
    public static Snapshot create(final @Nullable State state, final List<LogEntry> entries,
            final long lastIndex, final long lastTerm, final long lastAppliedIndex, final long lastAppliedTerm,
            final TermInfo termInfo, final @Nullable VotingConfig serverConfig) {
        return new Snapshot(state, entries, lastIndex, lastTerm, lastAppliedIndex, lastAppliedTerm, termInfo,
            serverConfig);
    }

    @NonNullByDefault
    public static Snapshot ofRaft(final TermInfo termInfo, final RaftSnapshot raftSnapshot,
            final EntryMeta lastIncluded, final @Nullable State state) {
        final var unapplied = raftSnapshot.unappliedEntries();
        return new Snapshot(state, unapplied,
            unapplied.isEmpty() ? lastIncluded.index() : unapplied.getLast().index(),
            unapplied.isEmpty() ? lastIncluded.term() : unapplied.getLast().term(),
            lastIncluded.index(), lastIncluded.term(), termInfo, raftSnapshot.votingConfig());
    }

    @NonNullByDefault
    public static Snapshot ofTermLeader(final @Nullable State state, final EntryMeta lastIncluded,
            final TermInfo termInfo, final @Nullable VotingConfig serverConfig) {
        return new Snapshot(state, List.of(), lastIncluded.index(), lastIncluded.term(), lastIncluded.index(),
            lastIncluded.term(), termInfo, serverConfig);
    }

    public @Nullable State state() {
        return state;
    }

    @NonNullByDefault
    public List<LogEntry> getUnAppliedEntries() {
        return unAppliedEntries;
    }

    public @NonNull EntryInfo last() {
        return EntryInfo.of(lastIndex, lastTerm);
    }

    public long getLastIndex() {
        return lastIndex;
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

    public @NonNull EntryInfo lastApplied() {
        return EntryInfo.of(lastAppliedIndex, lastAppliedTerm);
    }

    public @NonNull TermInfo termInfo() {
        return termInfo;
    }

    public @Nullable VotingConfig votingConfig() {
        return votingConfig;
    }

    @Override
    public String toString() {
        return "Snapshot [lastIndex=" + lastIndex + ", lastTerm=" + lastTerm + ", lastAppliedIndex=" + lastAppliedIndex
                + ", lastAppliedTerm=" + lastAppliedTerm + ", unAppliedEntries size=" + unAppliedEntries.size()
                + ", state=" + state + ", electionTerm=" + termInfo.term() + ", electionVotedFor="
                + termInfo.votedFor() + ", ServerConfigPayload=" + votingConfig + "]";
    }

    @java.io.Serial
    private Object writeReplace() {
        return new SS(this);
    }
}
