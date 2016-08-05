/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft;

import java.io.Serializable;
import java.util.List;
import org.opendaylight.controller.cluster.raft.persisted.ServerConfigurationPayload;

public class Snapshot implements Serializable {
    private static final long serialVersionUID = -8298574936724056236L;

    private final byte[] state;
    private final List<ReplicatedLogEntry> unAppliedEntries;
    private final long lastIndex;
    private final long lastTerm;
    private final long lastAppliedIndex;
    private final long lastAppliedTerm;
    private final long electionTerm;
    private final String electionVotedFor;
    private final ServerConfigurationPayload serverConfig;

    private Snapshot(byte[] state, List<ReplicatedLogEntry> unAppliedEntries, long lastIndex, long lastTerm,
            long lastAppliedIndex, long lastAppliedTerm, long electionTerm, String electionVotedFor,
            ServerConfigurationPayload serverConfig) {
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

    public static Snapshot create(byte[] state, List<ReplicatedLogEntry> entries, long lastIndex, long lastTerm,
            long lastAppliedIndex, long lastAppliedTerm) {
        return new Snapshot(state, entries, lastIndex, lastTerm, lastAppliedIndex, lastAppliedTerm, -1, null, null);
    }

    public static Snapshot create(byte[] state, List<ReplicatedLogEntry> entries, long lastIndex, long lastTerm,
            long lastAppliedIndex, long lastAppliedTerm, long electionTerm, String electionVotedFor) {
        return new Snapshot(state, entries, lastIndex, lastTerm, lastAppliedIndex, lastAppliedTerm,
                electionTerm, electionVotedFor, null);
    }

    public static Snapshot create(byte[] state, List<ReplicatedLogEntry> entries, long lastIndex, long lastTerm,
            long lastAppliedIndex, long lastAppliedTerm, long electionTerm, String electionVotedFor,
            ServerConfigurationPayload serverConfig) {
        return new Snapshot(state, entries, lastIndex, lastTerm, lastAppliedIndex, lastAppliedTerm,
                electionTerm, electionVotedFor, serverConfig);
    }

    public byte[] getState() {
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
        return this.lastIndex;
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
                + ", state size=" + state.length + ", electionTerm=" + electionTerm + ", electionVotedFor=" + electionVotedFor
                + ", ServerConfigPayload="  + serverConfig + "]";
    }
}
