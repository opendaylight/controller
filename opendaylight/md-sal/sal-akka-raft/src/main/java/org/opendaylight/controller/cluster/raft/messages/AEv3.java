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
import org.opendaylight.yangtools.concepts.WritableObjects;

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

        WritableObjects.writeLongs(out, appendEntries.getPrevLogTerm(), appendEntries.getPrevLogIndex());
        WritableObjects.writeLongs(out, appendEntries.getLeaderCommit(), appendEntries.getReplicatedToAllIndex());

        out.writeShort(appendEntries.getPayloadVersion());

        final List<ReplicatedLogEntry> entries = appendEntries.getEntries();
        out.writeInt(entries.size());
        for (ReplicatedLogEntry e: entries) {
            WritableObjects.writeLongs(out, e.getIndex(), e.getTerm());
            out.writeObject(e.getData());
        }

        out.writeObject(appendEntries.getLeaderAddress());
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        final short leaderRaftVersion = in.readShort();
        final long term = in.readLong();
        final String leaderId = (String) in.readObject();

        byte header = WritableObjects.readLongHeader(in);
        final long prevLogTerm = WritableObjects.readFirstLong(in, header);
        final long prevLogIndex = WritableObjects.readSecondLong(in, header);

        header = WritableObjects.readLongHeader(in);
        final long leaderCommit = WritableObjects.readFirstLong(in, header);
        final long replicatedToAllIndex = WritableObjects.readSecondLong(in, header);
        final short payloadVersion = in.readShort();

        final int size = in.readInt();
        List<ReplicatedLogEntry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            header = WritableObjects.readLongHeader(in);
            entries.add(new SimpleReplicatedLogEntry(WritableObjects.readFirstLong(in, header),
                WritableObjects.readSecondLong(in, header), (Payload) in.readObject()));
        }

        final String leaderAddress = (String) in.readObject();

        appendEntries = new AppendEntries(term, leaderId, prevLogIndex, prevLogTerm, entries, leaderCommit,
                replicatedToAllIndex, payloadVersion, RaftVersions.CURRENT_VERSION, leaderRaftVersion,
                leaderAddress);
    }

    private Object readResolve() {
        return appendEntries;
    }
}
