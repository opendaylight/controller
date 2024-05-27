/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import java.io.Serializable;
import java.util.List;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;

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
    public interface State extends Serializable {
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
    private final long electionTerm;
    private final String electionVotedFor;
    private final ServerConfigurationPayload serverConfig;

    private Snapshot(final State state, final List<ReplicatedLogEntry> unAppliedEntries, final long lastIndex,
            final long lastTerm, final long lastAppliedIndex, final long lastAppliedTerm, final long electionTerm,
            final String electionVotedFor, final ServerConfigurationPayload serverConfig) {
        this.state = state;
        this.unAppliedEntries = unAppliedEntries;
        this.lastIndex = lastIndex;
        this.lastTerm = lastTerm;
        this.lastAppliedIndex = lastAppliedIndex;
        this.lastAppliedTerm = lastAppliedTerm;
        this.electionTerm = electionTerm;
        this.electionVotedFor = electionVotedFor;
        this.serverConfig = serverConfig;
    }

    public static Snapshot create(final State state, final List<ReplicatedLogEntry> entries, final long lastIndex,
            final long lastTerm, final long lastAppliedIndex, final long lastAppliedTerm, final long electionTerm,
            final String electionVotedFor, final ServerConfigurationPayload serverConfig) {
        return new Snapshot(state, entries, lastIndex, lastTerm, lastAppliedIndex, lastAppliedTerm,
                electionTerm, electionVotedFor, serverConfig);
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

    public long getElectionTerm() {
        return electionTerm;
    }

    public String getElectionVotedFor() {
        return electionVotedFor;
    }

    public ServerConfigurationPayload getServerConfiguration() {
        return serverConfig;
    }

    @Override
    public String toString() {
        return "Snapshot [lastIndex=" + lastIndex + ", lastTerm=" + lastTerm + ", lastAppliedIndex=" + lastAppliedIndex
                + ", lastAppliedTerm=" + lastAppliedTerm + ", unAppliedEntries size=" + unAppliedEntries.size()
                + ", state=" + state + ", electionTerm=" + electionTerm + ", electionVotedFor="
                + electionVotedFor + ", ServerConfigPayload="  + serverConfig + "]";
    }

    @java.io.Serial
    private Object writeReplace() {
        return new SS(this);
    }
}
