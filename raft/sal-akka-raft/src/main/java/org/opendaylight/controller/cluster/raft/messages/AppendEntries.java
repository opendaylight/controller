/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.List;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.RaftVersions;
import org.opendaylight.controller.cluster.raft.spi.DefaultLogEntry;
import org.opendaylight.controller.cluster.raft.spi.LogEntry;
import org.opendaylight.controller.cluster.raft.spi.StateMachineCommand;

/**
 * Invoked by leader to replicate log entries (§5.3); also used as heartbeat (§5.2).
 */
public final class AppendEntries extends RaftRPC {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    // FIXME: split out to RaftRPC.FromLeader for sharing with InstallSnapshot
    // So that follower can redirect clients
    private final @NonNull String leaderId;

    // Index of log entry immediately preceding new ones
    private final long prevLogIndex;

    // term of prevLogIndex entry
    private final long prevLogTerm;

    // log entries to store (empty for heart beat - may send more than one for efficiency)
    @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "Handled via serialization proxy")
    private final @NonNull List<@NonNull LogEntry> entries;

    // leader's commitIndex
    private final long leaderCommit;

    // index which has been replicated successfully to all followers, -1 if none
    private final long replicatedToAllIndex;

    private final short payloadVersion;

    private final short recipientRaftVersion;

    private final short leaderRaftVersion;

    private final String leaderAddress;

    AppendEntries(final long term, final @NonNull String leaderId, final long prevLogIndex,
            final long prevLogTerm, final @NonNull List<@NonNull LogEntry> entries, final long leaderCommit,
            final long replicatedToAllIndex, final short payloadVersion, final short recipientRaftVersion,
            final short leaderRaftVersion, final @Nullable String leaderAddress) {
        super(term);
        this.leaderId = requireNonNull(leaderId);
        this.prevLogIndex = prevLogIndex;
        this.prevLogTerm = prevLogTerm;
        this.entries = requireNonNull(entries);
        this.leaderCommit = leaderCommit;
        this.replicatedToAllIndex = replicatedToAllIndex;
        this.payloadVersion = payloadVersion;
        this.recipientRaftVersion = recipientRaftVersion;
        this.leaderRaftVersion = leaderRaftVersion;
        this.leaderAddress = leaderAddress;
    }

    public AppendEntries(final long term, final @NonNull String leaderId, final long prevLogIndex,
            final long prevLogTerm, final @NonNull List<? extends @NonNull LogEntry> entries, final long leaderCommit,
            final long replicatedToAllIndex, final short payloadVersion, final short recipientRaftVersion,
            final @Nullable String leaderAddress) {
        this(term, leaderId, prevLogIndex, prevLogTerm, List.copyOf(entries), leaderCommit, replicatedToAllIndex,
            payloadVersion, recipientRaftVersion, RaftVersions.CURRENT_VERSION, leaderAddress);
    }

    @VisibleForTesting
    public AppendEntries(final long term, final @NonNull String leaderId, final long prevLogIndex,
            final long prevLogTerm, final @NonNull List<? extends @NonNull LogEntry> entries, final long leaderCommit,
            final long replicatedToAllIndex, final short payloadVersion) {
        this(term, leaderId, prevLogIndex, prevLogTerm, entries, leaderCommit, replicatedToAllIndex, payloadVersion,
                RaftVersions.CURRENT_VERSION, null);
    }

    public @NonNull String getLeaderId() {
        return leaderId;
    }

    public long getPrevLogIndex() {
        return prevLogIndex;
    }

    public long getPrevLogTerm() {
        return prevLogTerm;
    }

    public @NonNull List<? extends @NonNull LogEntry> getEntries() {
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

    public @Nullable String leaderAddress() {
        return leaderAddress;
    }

    public short getLeaderRaftVersion() {
        return leaderRaftVersion;
    }

    @Override
    ToStringHelper addToStringAttributes(final ToStringHelper helper) {
        return super.addToStringAttributes(helper)
            .add("leaderId", leaderId)
            .add("prevLogIndex", prevLogIndex)
            .add("prevLogTerm", prevLogTerm)
            .add("leaderCommit", leaderCommit)
            .add("replicatedToAllIndex", replicatedToAllIndex)
            .add("payloadVersion", payloadVersion)
            .add("recipientRaftVersion", recipientRaftVersion)
            .add("leaderRaftVersion", leaderRaftVersion)
            .add("leaderAddress", leaderAddress)
            .add("entries=", entries);
    }

    @Override
    Object writeReplace() {
        return recipientRaftVersion <= RaftVersions.FLUORINE_VERSION ? new ProxyV2(this) : new AE(this);
    }

    /**
     * Fluorine version that adds the leader address.
     */
    private static class ProxyV2 implements Externalizable {
        @java.io.Serial
        private static final long serialVersionUID = 1L;

        private AppendEntries appendEntries;

        // checkstyle flags the public modifier as redundant which really doesn't make sense since it clearly isn't
        // redundant. It is explicitly needed for Java serialization to be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public ProxyV2() {
        }

        ProxyV2(final AppendEntries appendEntries) {
            this.appendEntries = appendEntries;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeShort(appendEntries.leaderRaftVersion);
            out.writeLong(appendEntries.getTerm());
            out.writeObject(appendEntries.leaderId);
            out.writeLong(appendEntries.prevLogTerm);
            out.writeLong(appendEntries.prevLogIndex);
            out.writeLong(appendEntries.leaderCommit);
            out.writeLong(appendEntries.replicatedToAllIndex);
            out.writeShort(appendEntries.payloadVersion);

            out.writeInt(appendEntries.entries.size());
            for (var entry : appendEntries.entries) {
                out.writeLong(entry.index());
                out.writeLong(entry.term());
                out.writeObject(entry.command().toSerialForm());
            }

            out.writeObject(appendEntries.leaderAddress);
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
            var entries = ImmutableList.<LogEntry>builderWithExpectedSize(size);
            for (int i = 0; i < size; i++) {
                entries.add(new DefaultLogEntry(in.readLong(), in.readLong(), (StateMachineCommand) in.readObject()));
            }

            String leaderAddress = (String)in.readObject();

            appendEntries = new AppendEntries(term, leaderId, prevLogIndex, prevLogTerm, entries.build(), leaderCommit,
                    replicatedToAllIndex, payloadVersion, RaftVersions.CURRENT_VERSION, leaderRaftVersion,
                    leaderAddress);
        }

        @java.io.Serial
        private Object readResolve() {
            return appendEntries;
        }
    }
}
