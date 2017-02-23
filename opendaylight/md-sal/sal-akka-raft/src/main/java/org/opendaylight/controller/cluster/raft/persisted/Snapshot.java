/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;

/**
 * Represents a snapshot of the raft data.
 *
 * @author Thomas Pantelis
 */
public class Snapshot implements Serializable {

    /**
     * Implementations of this interface are used as the state payload for a snapshot.
     *
     * @author Thomas Pantelis
     */
    public interface State extends Serializable {
    }

    private static final class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;

        private Snapshot snapshot;

        // checkstyle flags the public modifier as redundant which really doesn't make sense since it clearly isn't
        // redundant. It is explicitly needed for Java serialization to be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
            // For Externalizable
        }

        Proxy(final Snapshot snapshot) {
            this.snapshot = snapshot;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeLong(snapshot.lastIndex);
            out.writeLong(snapshot.lastTerm);
            out.writeLong(snapshot.lastAppliedIndex);
            out.writeLong(snapshot.lastAppliedTerm);
            out.writeLong(snapshot.electionTerm);
            out.writeObject(snapshot.electionVotedFor);
            out.writeObject(snapshot.serverConfig);

            out.writeInt(snapshot.unAppliedEntries.size());
            for (ReplicatedLogEntry e: snapshot.unAppliedEntries) {
                out.writeLong(e.getIndex());
                out.writeLong(e.getTerm());
                out.writeObject(e.getData());
            }

            out.writeObject(snapshot.state);
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            long lastIndex = in.readLong();
            long lastTerm = in.readLong();
            long lastAppliedIndex = in.readLong();
            long lastAppliedTerm = in.readLong();
            long electionTerm = in.readLong();
            String electionVotedFor = (String) in.readObject();
            ServerConfigurationPayload serverConfig = (ServerConfigurationPayload) in.readObject();

            int size = in.readInt();
            List<ReplicatedLogEntry> unAppliedEntries = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                unAppliedEntries.add(new SimpleReplicatedLogEntry(in.readLong(), in.readLong(),
                        (Payload) in.readObject()));
            }

            State state = (State) in.readObject();

            snapshot = Snapshot.create(state, unAppliedEntries, lastIndex, lastTerm, lastAppliedIndex, lastAppliedTerm,
                    electionTerm, electionVotedFor, serverConfig);
        }

        private Object readResolve() {
            return snapshot;
        }
    }

    private static final long serialVersionUID = 1L;

    private final State state;
    private final List<ReplicatedLogEntry> unAppliedEntries;
    private final long lastIndex;
    private final long lastTerm;
    private final long lastAppliedIndex;
    private final long lastAppliedTerm;
    private final long electionTerm;
    private final String electionVotedFor;
    private final ServerConfigurationPayload serverConfig;

    private Snapshot(State state, List<ReplicatedLogEntry> unAppliedEntries, long lastIndex, long lastTerm,
            long lastAppliedIndex, long lastAppliedTerm, long electionTerm, String electionVotedFor,
            ServerConfigurationPayload serverConfig) {
        this.state = state;
        this.unAppliedEntries = unAppliedEntries;
        this.lastIndex = lastIndex;
        this.lastTerm = lastTerm;
        this.lastAppliedIndex = lastAppliedIndex;
        this.lastAppliedTerm = lastAppliedTerm;
        this.electionTerm = electionTerm;
        this.electionVotedFor = electionVotedFor;
        this.serverConfig = serverConfig;
    }

    public static Snapshot create(State state, List<ReplicatedLogEntry> entries, long lastIndex, long lastTerm,
            long lastAppliedIndex, long lastAppliedTerm, long electionTerm, String electionVotedFor,
            ServerConfigurationPayload serverConfig) {
        return new Snapshot(state, entries, lastIndex, lastTerm, lastAppliedIndex, lastAppliedTerm,
                electionTerm, electionVotedFor, serverConfig);
    }

    public State getState() {
        return state;
    }

    public List<ReplicatedLogEntry> getUnAppliedEntries() {
        return unAppliedEntries;
    }

    public long getLastTerm() {
        return lastTerm;
    }

    public long getLastAppliedIndex() {
        return lastAppliedIndex;
    }

    public long getLastAppliedTerm() {
        return lastAppliedTerm;
    }

    public long getLastIndex() {
        return this.lastIndex;
    }

    public long getElectionTerm() {
        return electionTerm;
    }

    public String getElectionVotedFor() {
        return electionVotedFor;
    }

    public ServerConfigurationPayload getServerConfiguration() {
        return serverConfig;
    }

    private Object writeReplace() {
        return new Proxy(this);
    }

    @Override
    public String toString() {
        return "Snapshot [lastIndex=" + lastIndex + ", lastTerm=" + lastTerm + ", lastAppliedIndex=" + lastAppliedIndex
                + ", lastAppliedTerm=" + lastAppliedTerm + ", unAppliedEntries size=" + unAppliedEntries.size()
                + ", state=" + state + ", electionTerm=" + electionTerm + ", electionVotedFor="
                + electionVotedFor + ", ServerConfigPayload="  + serverConfig + "]";
    }
}
