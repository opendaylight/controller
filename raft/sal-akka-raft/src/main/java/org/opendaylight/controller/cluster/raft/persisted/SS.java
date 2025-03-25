/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableList;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.messages.Payload;
import org.opendaylight.controller.cluster.raft.persisted.Snapshot.State;
import org.opendaylight.raft.api.TermInfo;
import org.opendaylight.yangtools.concepts.WritableObjects;

/**
 * Externalizable proxy for {@link Snapshot}.
 */
final class SS implements Externalizable {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private Snapshot snapshot;

    @SuppressWarnings("checkstyle:RedundantModifier")
    public SS() {
        // For Externalizable
    }

    SS(final Snapshot snapshot) {
        this.snapshot = requireNonNull(snapshot);
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        WritableObjects.writeLongs(out, snapshot.getLastIndex(), snapshot.getLastTerm());
        WritableObjects.writeLongs(out, snapshot.getLastAppliedIndex(), snapshot.getLastAppliedTerm());

        final var termInfo = snapshot.termInfo();
        WritableObjects.writeLong(out, termInfo.term());
        out.writeObject(termInfo.votedFor());

        out.writeObject(snapshot.getServerConfiguration());

        final var unAppliedEntries = snapshot.getUnAppliedEntries();
        out.writeInt(unAppliedEntries.size());
        for (var e : unAppliedEntries) {
            WritableObjects.writeLongs(out, e.index(), e.term());
            out.writeObject(e.getData());
        }

        out.writeObject(snapshot.getState());
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        byte hdr = WritableObjects.readLongHeader(in);
        long lastIndex = WritableObjects.readFirstLong(in, hdr);
        long lastTerm = WritableObjects.readSecondLong(in, hdr);

        hdr = WritableObjects.readLongHeader(in);
        long lastAppliedIndex = WritableObjects.readFirstLong(in, hdr);
        long lastAppliedTerm = WritableObjects.readSecondLong(in, hdr);
        long electionTerm = WritableObjects.readLong(in);
        String electionVotedFor = (String) in.readObject();
        ClusterConfig serverConfig = (ClusterConfig) in.readObject();

        int size = in.readInt();
        var unAppliedEntries = ImmutableList.<ReplicatedLogEntry>builderWithExpectedSize(size);
        for (int i = 0; i < size; i++) {
            hdr = WritableObjects.readLongHeader(in);
            unAppliedEntries.add(new SimpleReplicatedLogEntry(
                WritableObjects.readFirstLong(in, hdr), WritableObjects.readSecondLong(in, hdr),
                (Payload) in.readObject()));
        }

        State state = (State) in.readObject();

        snapshot = Snapshot.create(state, unAppliedEntries.build(), lastIndex, lastTerm, lastAppliedIndex,
            lastAppliedTerm, new TermInfo(electionTerm, electionVotedFor), serverConfig);
    }

    @java.io.Serial
    private Object readResolve() {
        return verifyNotNull(snapshot);
    }
}
