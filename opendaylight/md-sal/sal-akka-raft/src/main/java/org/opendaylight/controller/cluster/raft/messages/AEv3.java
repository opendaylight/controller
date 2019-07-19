/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import static java.util.Objects.requireNonNull;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.cluster.raft.RaftVersions;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;

/**
 * Sodium Externalizable proxy for AppendEntries.
 */
final class AEv3 implements Externalizable {
    private static final long serialVersionUID = 1L;

    private AppendEntries appendEntries;

    @SuppressWarnings("checkstyle:RedundantModifier")
    public AEv3() {
        // For Externalizable
    }

    AEv3(final AppendEntries appendEntries) {
        this.appendEntries = requireNonNull(appendEntries);
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeShort(appendEntries.getLeaderRaftVersion());
        out.writeLong(appendEntries.getTerm());
        out.writeObject(appendEntries.getLeaderId());
        out.writeLong(appendEntries.getPrevLogTerm());
        out.writeLong(appendEntries.getPrevLogIndex());
        out.writeLong(appendEntries.getLeaderCommit());
        out.writeLong(appendEntries.getReplicatedToAllIndex());
        out.writeShort(appendEntries.getPayloadVersion());

        final List<ReplicatedLogEntry> entries = appendEntries.getEntries();
        out.writeInt(entries.size());
        for (ReplicatedLogEntry e: entries) {
            out.writeLong(e.getIndex());
            out.writeLong(e.getTerm());
            out.writeObject(e.getData());
        }

        out.writeObject(appendEntries.getLeaderAddress());
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        short leaderRaftVersion = in.readShort();
        long term = in.readLong();
        String leaderId = (String) in.readObject();
        long prevLogTerm = in.readLong();
        long prevLogIndex = in.readLong();
        long leaderCommit = in.readLong();
        long replicatedToAllIndex = in.readLong();
        short payloadVersion = in.readShort();

        int size = in.readInt();
        List<ReplicatedLogEntry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            entries.add(new SimpleReplicatedLogEntry(in.readLong(), in.readLong(), (Payload) in.readObject()));
        }

        String leaderAddress = (String)in.readObject();

        appendEntries = new AppendEntries(term, leaderId, prevLogIndex, prevLogTerm, entries, leaderCommit,
                replicatedToAllIndex, payloadVersion, RaftVersions.CURRENT_VERSION, leaderRaftVersion,
                leaderAddress);
    }

    private Object readResolve() {
        return appendEntries;
    }
}
