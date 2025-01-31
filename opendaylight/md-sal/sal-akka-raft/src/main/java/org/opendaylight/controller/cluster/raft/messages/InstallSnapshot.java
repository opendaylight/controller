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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.OptionalInt;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.controller.cluster.raft.RaftVersions;
import org.opendaylight.controller.cluster.raft.persisted.ClusterConfig;

/**
 * Message sent from a leader to install a snapshot chunk on a follower.
 */
public final class InstallSnapshot extends RaftRPC {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    private final String leaderId;
    private final long lastIncludedIndex;
    private final long lastIncludedTerm;
    private final byte[] data;
    private final int chunkIndex;
    private final int totalChunks;
    @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "Handled via writeReplace()")
    private final OptionalInt lastChunkHashCode;
    private final @Nullable ClusterConfig serverConfig;
    private final short recipientRaftVersion;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = """
        Stores a reference to an externally mutable byte[] object but this is OK since this class is merely a DTO and \
        does not process byte[] internally. Also it would be inefficient to create a copy as the byte[] could be \
        large.""")
    public InstallSnapshot(final long term, final String leaderId, final long lastIncludedIndex,
            final long lastIncludedTerm, final byte[] data, final int chunkIndex, final int totalChunks,
            final OptionalInt lastChunkHashCode, final @Nullable ClusterConfig serverConfig,
            final short recipientRaftVersion) {
        super(term);
        this.leaderId = leaderId;
        this.lastIncludedIndex = lastIncludedIndex;
        this.lastIncludedTerm = lastIncludedTerm;
        this.data = data;
        this.chunkIndex = chunkIndex;
        this.totalChunks = totalChunks;
        this.lastChunkHashCode = lastChunkHashCode;
        this.serverConfig = serverConfig;
        this.recipientRaftVersion = recipientRaftVersion;
    }

    @VisibleForTesting
    public InstallSnapshot(final long term, final String leaderId, final long lastIncludedIndex,
                           final long lastIncludedTerm, final byte[] data, final int chunkIndex,
                           final int totalChunks) {
        this(term, leaderId, lastIncludedIndex, lastIncludedTerm, data, chunkIndex, totalChunks, OptionalInt.empty(),
            null, RaftVersions.CURRENT_VERSION);
    }

    public String getLeaderId() {
        return leaderId;
    }

    public long getLastIncludedIndex() {
        return lastIncludedIndex;
    }

    public long getLastIncludedTerm() {
        return lastIncludedTerm;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = """
        Exposes a mutable object stored in a field but this is OK since this class is merely a DTO and does not \
        process the byte[] internally. Also it would be inefficient to create a return copy as the byte[] could be \
        large.""")
    public byte[] getData() {
        return data;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public OptionalInt getLastChunkHashCode() {
        return lastChunkHashCode;
    }

    public @Nullable ClusterConfig serverConfig() {
        return serverConfig;
    }

    @Override
    ToStringHelper addToStringAttributes(final ToStringHelper helper) {
        return super.addToStringAttributes(helper)
            .add("leaderId", leaderId)
            .add("lastIncludedIndex", lastIncludedIndex)
            .add("lastIncludedTerm", lastIncludedTerm)
            .add("datasize", data.length)
            .add("chunk", chunkIndex + "/" + totalChunks)
            .add("lastChunkHashCode", lastChunkHashCode)
            .add("serverConfig", serverConfig);
    }

    @Override
    Object writeReplace() {
        return recipientRaftVersion <= RaftVersions.FLUORINE_VERSION ? new Proxy(this) : new IS(this);
    }

    private static class Proxy implements Externalizable {
        @java.io.Serial
        private static final long serialVersionUID = 1L;

        private InstallSnapshot installSnapshot;

        // checkstyle flags the public modifier as redundant which really doesn't make sense since it clearly isn't
        // redundant. It is explicitly needed for Java serialization to be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
        }

        Proxy(final InstallSnapshot installSnapshot) {
            this.installSnapshot = installSnapshot;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeLong(installSnapshot.getTerm());
            out.writeObject(installSnapshot.leaderId);
            out.writeLong(installSnapshot.lastIncludedIndex);
            out.writeLong(installSnapshot.lastIncludedTerm);
            out.writeInt(installSnapshot.chunkIndex);
            out.writeInt(installSnapshot.totalChunks);

            out.writeByte(installSnapshot.lastChunkHashCode.isPresent() ? 1 : 0);
            if (installSnapshot.lastChunkHashCode.isPresent()) {
                out.writeInt(installSnapshot.lastChunkHashCode.orElseThrow());
            }

            out.writeByte(installSnapshot.serverConfig != null ? 1 : 0);
            if (installSnapshot.serverConfig != null) {
                out.writeObject(installSnapshot.serverConfig);
            }

            out.writeObject(installSnapshot.data);
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            long term = in.readLong();
            String leaderId = (String) in.readObject();
            long lastIncludedIndex = in.readLong();
            long lastIncludedTerm = in.readLong();
            int chunkIndex = in.readInt();
            int totalChunks = in.readInt();

            final var lastChunkHashCode = in.readByte() == 1 ? OptionalInt.of(in.readInt()) : OptionalInt.empty();
            final var serverConfig = in.readByte() == 1 ? requireNonNull((ClusterConfig) in.readObject()) : null;
            final var data = (byte[]) in.readObject();

            installSnapshot = new InstallSnapshot(term, leaderId, lastIncludedIndex, lastIncludedTerm, data,
                    chunkIndex, totalChunks, lastChunkHashCode, serverConfig, RaftVersions.CURRENT_VERSION);
        }

        @java.io.Serial
        private Object readResolve() {
            return installSnapshot;
        }
    }
}
