/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.messages;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.ReplicatedLogImplEntry;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;

/**
 * Invoked by leader to replicate log entries (§5.3); also used as
 * heartbeat (§5.2).
 */
public class AppendEntries extends AbstractRaftRPC {
    private static final long serialVersionUID = 1L;

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

    private Object writeReplace() {
        return new Proxy(this);
    }

    private static class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;

        private AppendEntries appendEntries;

        public Proxy() {
        }

        Proxy(AppendEntries appendEntries) {
            this.appendEntries = appendEntries;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeLong(appendEntries.getTerm());
            out.writeObject(appendEntries.leaderId);
            out.writeLong(appendEntries.prevLogTerm);
            out.writeLong(appendEntries.prevLogIndex);
            out.writeLong(appendEntries.leaderCommit);
            out.writeLong(appendEntries.replicatedToAllIndex);
            out.writeShort(appendEntries.payloadVersion);

            out.writeInt(appendEntries.entries.size());
            for(ReplicatedLogEntry e: appendEntries.entries) {
                out.writeLong(e.getIndex());
                out.writeLong(e.getTerm());
                out.writeObject(e.getData());
            }
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            long term = in.readLong();
            String leaderId = (String) in.readObject();
            long prevLogTerm = in.readLong();
            long prevLogIndex = in.readLong();
            long leaderCommit = in.readLong();
            long replicatedToAllIndex = in.readLong();
            short payloadVersion = in.readShort();

            int size = in.readInt();
            List<ReplicatedLogEntry> entries = new ArrayList<>(size);
            for(int i = 0; i < size; i++) {
                entries.add(new ReplicatedLogImplEntry(in.readLong(), in.readLong(), (Payload) in.readObject()));
            }

            appendEntries = new AppendEntries(term, leaderId, prevLogIndex, prevLogTerm, entries, leaderCommit,
                    replicatedToAllIndex, payloadVersion);
        }

        private Object readResolve() {
            return appendEntries;
        }
    }
}
