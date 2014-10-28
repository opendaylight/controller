/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.messages;

import com.google.common.collect.Lists;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.ReplicatedLogImplEntry;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.List;

/**
 * Invoked by leader to replicate log entries (§5.3); also used as
 * heartbeat (§5.2).
 */
public class AppendEntries extends AbstractRaftRPC implements Externalizable {
    private static final long serialVersionUID = 1L;

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AppendEntries.class);

    // So that follower can redirect clients
    private transient String leaderId;

    // Index of log entry immediately preceding new ones
    private transient long prevLogIndex;

    // term of prevLogIndex entry
    private transient long prevLogTerm;

    // log entries to store (empty for heartbeat;
    // may send more than one for efficiency)
    private transient List<ReplicatedLogEntry> entries;

    // leader's commitIndex
    private transient long leaderCommit;

    public AppendEntries() {
        super(-1);
    }

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

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeLong(getTerm());
        out.writeLong(getPrevLogTerm());
        out.writeLong(getPrevLogIndex());
        out.writeLong(getLeaderCommit());
        out.writeUTF(getLeaderId());

        out.writeInt(entries.size());
        for (ReplicatedLogEntry entry : entries) {
            out.writeLong(entry.getIndex());
            out.writeLong(entry.getTerm());
            out.writeObject(entry.getData());
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        setTerm(in.readLong());
        prevLogTerm = in.readLong();
        prevLogIndex = in.readLong();
        leaderCommit = in.readLong();
        leaderId = in.readUTF();

        int size = in.readInt();
        entries = Lists.newArrayListWithCapacity(size);
        for(int i = 0; i < size; i++) {
            entries.add(new ReplicatedLogImplEntry(in.readLong(), in.readLong(),
                    (Payload) in.readObject()));
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb =
            new StringBuilder("AppendEntries{");
        sb.append("term=").append(getTerm());
        sb.append("leaderId='").append(leaderId).append('\'');
        sb.append(", prevLogIndex=").append(prevLogIndex);
        sb.append(", prevLogTerm=").append(prevLogTerm);
        sb.append(", entries=").append(entries);
        sb.append(", leaderCommit=").append(leaderCommit);
        sb.append('}');
        return sb.toString();
    }
}
