/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.messages;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.cluster.raft.RaftVersions;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;

/**
 * Invoked by leader to replicate log entries (§5.3); also used as
 * heartbeat (§5.2).
 */
public class AppendEntries extends AbstractRaftRPC {
    private static final long serialVersionUID = 1L;

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AppendEntries.class);

    // So that follower can redirect clients
    private final String leaderId;

    // Index of log entry immediately preceding new ones
    private final long prevLogIndex;

    // term of prevLogIndex entry
    private final long prevLogTerm;

    // log entries to store (empty for heartbeat;
    // may send more than one for efficiency)
    private transient List<ReplicatedLogEntry> entries;

    // leader's commitIndex
    private final long leaderCommit;

    // index which has been replicated successfully to all followers, -1 if none
    private final long replicatedToAllIndex;

    private final short payloadVersion;

    public AppendEntries(long term, String leaderId, long prevLogIndex, long prevLogTerm,
            List<ReplicatedLogEntry> entries, long leaderCommit, long replicatedToAllIndex, short payloadVersion) {
        super(term);
        this.leaderId = leaderId;
        this.prevLogIndex = prevLogIndex;
        this.prevLogTerm = prevLogTerm;
        this.entries = entries;
        this.leaderCommit = leaderCommit;
        this.replicatedToAllIndex = replicatedToAllIndex;
        this.payloadVersion = payloadVersion;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeShort(RaftVersions.CURRENT_VERSION);
        out.defaultWriteObject();

        out.writeInt(entries.size());
        for(ReplicatedLogEntry e: entries) {
            out.writeObject(e);
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.readShort(); // raft version

        in.defaultReadObject();

        int size = in.readInt();
        entries = new ArrayList<>(size);
        for(int i = 0; i < size; i++) {
            entries.add((ReplicatedLogEntry) in.readObject());
        }
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

    public long getReplicatedToAllIndex() {
        return replicatedToAllIndex;
    }

    public short getPayloadVersion() {
        return payloadVersion;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("AppendEntries [leaderId=").append(leaderId).append(", prevLogIndex=").append(prevLogIndex)
                .append(", prevLogTerm=").append(prevLogTerm).append(", leaderCommit=").append(leaderCommit)
                .append(", replicatedToAllIndex=").append(replicatedToAllIndex).append(", payloadVersion=")
                .append(payloadVersion).append(", entries=").append(entries).append("]");
        return builder.toString();
    }
}
