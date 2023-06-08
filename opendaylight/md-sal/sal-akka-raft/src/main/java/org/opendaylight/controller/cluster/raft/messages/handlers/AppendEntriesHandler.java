/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages.handlers;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.opendaylight.controller.cluster.raft.RaftMessageHandler;
import org.opendaylight.controller.cluster.raft.RaftVersions;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.messages.AppendEntries;
import org.opendaylight.controller.cluster.raft.messages.RaftRPC;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;

public class AppendEntriesHandler implements RaftMessageHandler {
    @Override
    public void writeTo(final DataOutput out, final RaftRPC message) throws IOException {
        final AppendEntries appendEntries = (AppendEntries) message;
        out.writeShort(appendEntries.getLeaderRaftVersion());
        out.writeLong(appendEntries.getTerm());
        out.writeUTF(appendEntries.getLeaderId());

        out.writeLong(appendEntries.getPrevLogTerm());
        out.writeLong(appendEntries.getPrevLogIndex());
        out.writeLong(appendEntries.getLeaderCommit());
        out.writeLong(appendEntries.getReplicatedToAllIndex());

        out.writeShort(appendEntries.getPayloadVersion());

        final List<ReplicatedLogEntry> entries = appendEntries.getEntries();
        out.writeInt(entries.size());
        for (ReplicatedLogEntry entry : entries) {
            entry.writeTo(out);
        }

        final Optional<String> leaderAddress = appendEntries.getLeaderAddress();
        out.writeBoolean(leaderAddress.isPresent());
        if (leaderAddress.isPresent()) {
            out.writeUTF(leaderAddress.get());
        }
    }

    @Override
    public RaftRPC readFrom(final DataInput in) throws IOException, ClassNotFoundException {
        final short leaderRaftVersion = in.readShort();
        final long term = in.readLong();
        final String leaderId = in.readUTF();
        final long prevLogTerm = in.readLong();
        final long prevLogIndex = in.readLong();
        final long leaderCommit = in.readLong();
        final long replicatedToAllIndex = in.readLong();
        final short payloadVersion = in.readShort();

        final int size = in.readInt();
        final List<ReplicatedLogEntry> entries = new ArrayList<ReplicatedLogEntry>(size);
        for (int i = 0; i < size; i++) {
            entries.add(SimpleReplicatedLogEntry.readFrom(in));
        }

        final boolean leaderAddressPresent = in.readBoolean();
        String leaderAddress = null;
        if (leaderAddressPresent) {
            leaderAddress = in.readUTF();
        }

        return new AppendEntries(term, leaderId, prevLogIndex, prevLogTerm, entries, leaderCommit,
                replicatedToAllIndex, payloadVersion, RaftVersions.CURRENT_VERSION, leaderRaftVersion,
                leaderAddress);
    }
}
