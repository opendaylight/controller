/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.persisted;

import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.cluster.raft.ReplicatedLogEntry;
import org.opendaylight.controller.cluster.raft.protobuff.client.messages.Payload;
import org.slf4j.LoggerFactory;

/**
 * Represents a snapshot of the raft data.
 *
 * @author Thomas Pantelis
 */
public class Snapshot implements Serializable, MigratedSerializable {
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

            snapshot.state.copyTo(toOutputStream(out));
        }

        private OutputStream toOutputStream(final ObjectOutput out) {
            if (out instanceof OutputStream) {
                return (OutputStream) out;
            }

            return new OutputStream() {
                @Override
                public void write(int value) throws IOException {
                    out.write(value);
                }
            };
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

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            copy(in, bos);

            ByteSource state = new ByteSource() {
                @Override
                public InputStream openStream() throws IOException {
                    return new ByteArrayInputStream(bos.toByteArray());
                }
            };

            snapshot = Snapshot.create(state, unAppliedEntries, lastIndex, lastTerm, lastAppliedIndex, lastAppliedTerm,
                    electionTerm, electionVotedFor, serverConfig);
        }

        private static void copy(final ObjectInput from, final OutputStream to) throws IOException {
            if (from instanceof InputStream) {
                ByteStreams.copy((InputStream) from, to);
                return;
            }

            ByteStreams.copy(new InputStream() {
                @Override
                public int read() throws IOException {
                    return from.read();
                }
            }, to);
        }

        private Object readResolve() {
            return snapshot;
        }
    }

    private static final long serialVersionUID = 1L;

    private final ByteSource state;
    private final List<ReplicatedLogEntry> unAppliedEntries;
    private final long lastIndex;
    private final long lastTerm;
    private final long lastAppliedIndex;
    private final long lastAppliedTerm;
    private final long electionTerm;
    private final String electionVotedFor;
    private final ServerConfigurationPayload serverConfig;
    private final transient boolean migrated;

    private Snapshot(ByteSource state, List<ReplicatedLogEntry> unAppliedEntries, long lastIndex, long lastTerm,
            long lastAppliedIndex, long lastAppliedTerm, long electionTerm, String electionVotedFor,
            ServerConfigurationPayload serverConfig, final boolean migrated) {
        this.state = state;
        this.unAppliedEntries = unAppliedEntries;
        this.lastIndex = lastIndex;
        this.lastTerm = lastTerm;
        this.lastAppliedIndex = lastAppliedIndex;
        this.lastAppliedTerm = lastAppliedTerm;
        this.electionTerm = electionTerm;
        this.electionVotedFor = electionVotedFor;
        this.serverConfig = serverConfig;
        this.migrated = migrated;
    }

    public static Snapshot create(ByteSource state, List<ReplicatedLogEntry> entries, long lastIndex, long lastTerm,
            long lastAppliedIndex, long lastAppliedTerm, long electionTerm, String electionVotedFor,
            ServerConfigurationPayload serverConfig) {
        return create(state, entries, lastIndex, lastTerm, lastAppliedIndex, lastAppliedTerm,
                electionTerm, electionVotedFor, serverConfig, false);
    }

    public static Snapshot create(ByteSource state, List<ReplicatedLogEntry> entries, long lastIndex, long lastTerm,
            long lastAppliedIndex, long lastAppliedTerm, long electionTerm, String electionVotedFor,
            ServerConfigurationPayload serverConfig, boolean migrated) {
        return new Snapshot(state, entries, lastIndex, lastTerm, lastAppliedIndex, lastAppliedTerm,
                electionTerm, electionVotedFor, serverConfig, migrated);
    }

    public ByteSource getState() {
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

    @Override
    public Object writeReplace() {
        return new Proxy(this);
    }

    @Override
    public boolean isMigrated() {
        return migrated;
    }

    @Override
    public String toString() {
        long stateSize = -1;
        try {
            stateSize = state.size();
        } catch (IOException e) {
            LoggerFactory.getLogger(Snapshot.class).error("Error getting ByteSource size", e);
        }

        return "Snapshot [lastIndex=" + lastIndex + ", lastTerm=" + lastTerm + ", lastAppliedIndex=" + lastAppliedIndex
                + ", lastAppliedTerm=" + lastAppliedTerm + ", unAppliedEntries size=" + unAppliedEntries.size()
                + ", state size=" + stateSize + ", electionTerm=" + electionTerm + ", electionVotedFor="
                + electionVotedFor + ", ServerConfigPayload="  + serverConfig + "]";
    }
}
