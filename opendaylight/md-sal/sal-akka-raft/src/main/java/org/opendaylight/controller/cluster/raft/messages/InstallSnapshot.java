/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import com.google.common.annotations.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Optional;
import java.util.OptionalInt;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.raft.RaftVersions;
import org.opendaylight.controller.cluster.raft.persisted.ServerConfigurationPayload;
import org.opendaylight.yangtools.concepts.WritableObjects;

/**
 * Message sent from a leader to install a snapshot chunk on a follower.
 */
public final class InstallSnapshot extends AbstractRaftRPC {
    @java.io.Serial
    private static final long serialVersionUID = 1L;
    // Flags
    private static final int LAST_CHUNK_HASHCODE = 0x10;
    private static final int SERVER_CONFIG       = 0x20;

    private final String leaderId;
    private final long lastIncludedIndex;
    private final long lastIncludedTerm;
    private final byte[] data;
    private final int chunkIndex;
    private final int totalChunks;
    @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "Handled via writeReplace()")
    private final OptionalInt lastChunkHashCode;
    @SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "Handled via writeReplace()")
    private final Optional<ServerConfigurationPayload> serverConfig;
    private final short recipientRaftVersion;

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Stores a reference to an externally mutable byte[] "
            + "object but this is OK since this class is merely a DTO and does not process byte[] internally. "
            + "Also it would be inefficient to create a copy as the byte[] could be large.")
    public InstallSnapshot(final long term, final String leaderId, final long lastIncludedIndex,
            final long lastIncludedTerm, final byte[] data, final int chunkIndex, final int totalChunks,
            final OptionalInt lastChunkHashCode, final Optional<ServerConfigurationPayload> serverConfig,
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
            Optional.empty(), RaftVersions.CURRENT_VERSION);
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

    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Exposes a mutable object stored in a field but "
            + "this is OK since this class is merely a DTO and does not process the byte[] internally. "
            + "Also it would be inefficient to create a return copy as the byte[] could be large.")
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

    public Optional<ServerConfigurationPayload> getServerConfig() {
        return serverConfig;
    }

    @Override
    public String toString() {
        return "InstallSnapshot [term=" + getTerm() + ", leaderId=" + leaderId + ", lastIncludedIndex="
                + lastIncludedIndex + ", lastIncludedTerm=" + lastIncludedTerm + ", datasize=" + data.length
                + ", Chunk=" + chunkIndex + "/" + totalChunks + ", lastChunkHashCode=" + lastChunkHashCode
                + ", serverConfig=" + serverConfig.orElse(null) + "]";
    }

    @Override
    Object writeReplace() {
        return recipientRaftVersion <= RaftVersions.FLUORINE_VERSION ? new Proxy(this) : new IS(this);
    }

    @Override
    public void writeTo(DataOutput out) throws IOException {
        int flags = 0;
        final var lastChunkHashCode = getLastChunkHashCode();
        if (lastChunkHashCode.isPresent()) {
            flags |= LAST_CHUNK_HASHCODE;
        }
        final var serverConfig = getServerConfig();
        if (serverConfig.isPresent()) {
            flags |= SERVER_CONFIG;
        }

        WritableObjects.writeLong(out, getTerm(), flags);
        out.writeUTF(getLeaderId());
        WritableObjects.writeLongs(out, getLastIncludedIndex(), getLastIncludedTerm());
        out.writeInt(getChunkIndex());
        out.writeInt(getTotalChunks());

        if (lastChunkHashCode.isPresent()) {
            out.writeInt(lastChunkHashCode.getAsInt());
        }
        if (serverConfig.isPresent()) {
            serverConfig.get().writeTo(out);;
        }

        out.write(getData().length);
        out.write(getData());
    }

    public static @NonNull InstallSnapshot readFrom(final DataInput in) throws IOException {
        byte hdr = WritableObjects.readLongHeader(in);
        final int flags = WritableObjects.longHeaderFlags(hdr);

        long term = WritableObjects.readLongBody(in, hdr);
        String leaderId = in.readUTF();

        hdr = WritableObjects.readLongHeader(in);
        long lastIncludedIndex = WritableObjects.readFirstLong(in, hdr);
        long lastIncludedTerm = WritableObjects.readSecondLong(in, hdr);
        int chunkIndex = in.readInt();
        int totalChunks = in.readInt();

        OptionalInt lastChunkHashCode = getFlag(flags, LAST_CHUNK_HASHCODE) ? OptionalInt.of(in.readInt())
                : OptionalInt.empty();
        Optional<ServerConfigurationPayload> serverConfig = getFlag(flags, SERVER_CONFIG)
                ? Optional.of((ServerConfigurationPayload.readFrom(in))) : Optional.empty();

        int dataLength = in.readInt();
        byte[] data = new byte[dataLength];
        in.readFully(data);

        return new InstallSnapshot(term, leaderId, lastIncludedIndex, lastIncludedTerm, data,
                chunkIndex, totalChunks, lastChunkHashCode, serverConfig, RaftVersions.CURRENT_VERSION);
    }

    private static boolean getFlag(final int flags, final int bit) {
        return (flags & bit) != 0;
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
                out.writeInt(installSnapshot.lastChunkHashCode.getAsInt());
            }

            out.writeByte(installSnapshot.serverConfig.isPresent() ? 1 : 0);
            if (installSnapshot.serverConfig.isPresent()) {
                out.writeObject(installSnapshot.serverConfig.get());
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

            OptionalInt lastChunkHashCode = in.readByte() == 1 ? OptionalInt.of(in.readInt()) : OptionalInt.empty();
            Optional<ServerConfigurationPayload> serverConfig = in.readByte() == 1
                    ? Optional.of((ServerConfigurationPayload)in.readObject()) : Optional.empty();

            byte[] data = (byte[])in.readObject();

            installSnapshot = new InstallSnapshot(term, leaderId, lastIncludedIndex, lastIncludedTerm, data,
                    chunkIndex, totalChunks, lastChunkHashCode, serverConfig, RaftVersions.CURRENT_VERSION);
        }

        @java.io.Serial
        private Object readResolve() {
            return installSnapshot;
        }
    }
}
