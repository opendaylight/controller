/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
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
import org.opendaylight.controller.cluster.raft.RaftVersions;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;

/**
 * Argon serialization proxy for {@link AppendEntries}.
 */
final class AE implements Externalizable {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private AppendEntries appendEntries;

    // checkstyle flags the public modifier as redundant which really doesn't make sense since it clearly isn't
    // redundant. It is explicitly needed for Java serialization to be able to create instances via reflection.
    @SuppressWarnings("checkstyle:RedundantModifier")
    public AE() {
    }

    AE(final AppendEntries appendEntries) {
        this.appendEntries = appendEntries;
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

        final var entries = appendEntries.getEntries();
        out.writeInt(entries.size());
        for (var e : entries) {
            out.writeLong(e.getIndex());
            out.writeLong(e.getTerm());
            out.writeObject(e.getData());
        }

        out.writeObject(appendEntries.getLeaderAddress().orElse(null));
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
        var entries = new ArrayList<ReplicatedLogEntry>(size);
        for (int i = 0; i < size; i++) {
            entries.add(new SimpleReplicatedLogEntry(in.readLong(), in.readLong(), (Payload) in.readObject()));
        }

        String leaderAddress = (String)in.readObject();

        appendEntries = new AppendEntries(term, leaderId, prevLogIndex, prevLogTerm, entries, leaderCommit,
                replicatedToAllIndex, payloadVersion, RaftVersions.CURRENT_VERSION, leaderRaftVersion,
                leaderAddress);
    }

    @java.io.Serial
    private Object readResolve() {
        return appendEntries;
    }
}
