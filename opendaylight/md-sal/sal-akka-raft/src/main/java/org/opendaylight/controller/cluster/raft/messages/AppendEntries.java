/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.messages;

import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;

import java.util.List;

/**
 * Invoked by leader to replicate log entries (§5.3); also used as
 * heartbeat (§5.2).
 */
public class AppendEntries extends AbstractRaftRPC {
    // So that follower can redirect clients
    private final String leaderId;

    // Index of log entry immediately preceding new ones
    private final long prevLogIndex;

    // term of prevLogIndex entry
    private final long prevLogTerm;

    // log entries to store (empty for heartbeat;
    // may send more than one for efficiency)
    private final List<ReplicatedLogEntry> entries;

    // leader's commitIndex
    private final long leaderCommit;

    public AppendEntries(long term, String leaderId, long prevLogIndex,
        long prevLogTerm, List<ReplicatedLogEntry> entries, long leaderCommit) {
        super(term);
        this.leaderId = leaderId;
        this.prevLogIndex = prevLogIndex;
        this.prevLogTerm = prevLogTerm;
        this.entries = entries;
        this.leaderCommit = leaderCommit;
    }

    public String getLeaderId() {
        return leaderId;
    }

    public long getPrevLogIndex() {
        return prevLogIndex;
    }

    public long getPrevLogTerm() {
        return prevLogTerm;
    }

    public List<ReplicatedLogEntry> getEntries() {
        return entries;
    }

    public long getLeaderCommit() {
        return leaderCommit;
    }

    @Override public String toString() {
        return "AppendEntries{" +
            "leaderId='" + leaderId + '\'' +
            ", prevLogIndex=" + prevLogIndex +
            ", prevLogTerm=" + prevLogTerm +
            ", entries=" + entries +
            ", leaderCommit=" + leaderCommit +
            '}';
    }
}
