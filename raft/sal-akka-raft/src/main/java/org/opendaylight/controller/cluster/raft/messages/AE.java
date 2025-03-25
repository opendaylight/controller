/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableList;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.raft.RaftVersions;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.persisted.SimpleReplicatedLogEntry;
import org.opendaylight.yangtools.concepts.WritableObjects;

/**
 * Argon serialization proxy for {@link AppendEntries}.
 */
final class AE implements Externalizable {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private AppendEntries appendEntries;

    @SuppressWarnings("checkstyle:RedundantModifier")
    public AE() {
        // For Externalizable
    }

    AE(final AppendEntries appendEntries) {
        this.appendEntries = requireNonNull(appendEntries);
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeShort(appendEntries.getLeaderRaftVersion());
        WritableObjects.writeLong(out, appendEntries.getTerm());
        out.writeObject(appendEntries.getLeaderId());

        WritableObjects.writeLongs(out, appendEntries.getPrevLogTerm(), appendEntries.getPrevLogIndex());
        WritableObjects.writeLongs(out, appendEntries.getLeaderCommit(), appendEntries.getReplicatedToAllIndex());

        out.writeShort(appendEntries.getPayloadVersion());

        final var entries = appendEntries.getEntries();
        out.writeInt(entries.size());
        for (var e : entries) {
            WritableObjects.writeLongs(out, e.index(), e.term());
            out.writeObject(e.getData());
        }

        out.writeObject(appendEntries.leaderAddress());
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        short leaderRaftVersion = in.readShort();
        long term = WritableObjects.readLong(in);
        String leaderId = (String) in.readObject();

        byte hdr = WritableObjects.readLongHeader(in);
        long prevLogTerm = WritableObjects.readFirstLong(in, hdr);
        long prevLogIndex = WritableObjects.readSecondLong(in, hdr);

        hdr = WritableObjects.readLongHeader(in);
        long leaderCommit = WritableObjects.readFirstLong(in, hdr);
        long replicatedToAllIndex = WritableObjects.readSecondLong(in, hdr);
        short payloadVersion = in.readShort();

        int size = in.readInt();
        var entries = ImmutableList.<ReplicatedLogEntry>builderWithExpectedSize(size);
        for (int i = 0; i < size; i++) {
            hdr = WritableObjects.readLongHeader(in);
            entries.add(new SimpleReplicatedLogEntry(WritableObjects.readFirstLong(in, hdr),
                WritableObjects.readSecondLong(in, hdr), (Payload) in.readObject()));
        }

        String leaderAddress = (String)in.readObject();

        appendEntries = new AppendEntries(term, leaderId, prevLogIndex, prevLogTerm, entries.build(), leaderCommit,
                replicatedToAllIndex, payloadVersion, RaftVersions.CURRENT_VERSION, leaderRaftVersion,
                leaderAddress);
    }

    @java.io.Serial
    private Object readResolve() {
        return verifyNotNull(appendEntries);
    }
}
