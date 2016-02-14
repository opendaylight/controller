/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.messages;

import com.google.common.base.Optional;
import com.google.protobuf.ByteString;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.raft.RaftVersions;
import org.opendaylight.controller.protobuff.messages.cluster.raft.InstallSnapshotMessages;
import org.opendaylight.yangtools.concepts.Builder;

public final class InstallSnapshot extends AbstractRaftRPC implements Externalizable {
    private static final class InstallSnapshotBuilder implements Builder<InstallSnapshot> {
        private long term;
        private String leaderId;
        private long lastIncludedIndex;
        private long lastIncludedTerm;
        private int chunkIndex;
        private int totalChunks;
        private Optional<Integer> lastChunkHashCode;
        private byte[] data;

        @Override
        public InstallSnapshot build() {
            return new InstallSnapshot(term, leaderId, lastIncludedIndex, lastIncludedTerm, data, chunkIndex,
                totalChunks, lastChunkHashCode);
        }
    }

    private static final long serialVersionUID = 1L;
    public static final Class<InstallSnapshotMessages.InstallSnapshot> SERIALIZABLE_CLASS = InstallSnapshotMessages.InstallSnapshot.class;

    private final String leaderId;
    private final long lastIncludedIndex;
    private final long lastIncludedTerm;
    private final byte[] data;
    private final int chunkIndex;
    private final int totalChunks;
    private final Optional<Integer> lastChunkHashCode;

    // Used strictly during deserialization
    private final InstallSnapshotBuilder builder;

    /**
     * Empty constructor to satisfy Externalizable.
     *
     * @deprecated Do no use.
     */
    @Deprecated
    public InstallSnapshot() {
        super(0L);
        this.leaderId = null;
        this.lastIncludedIndex = 0;
        this.lastIncludedTerm = 0;
        this.data = null;
        this.chunkIndex = 0;
        this.totalChunks = 0;
        this.lastChunkHashCode = null;
        this.builder = new InstallSnapshotBuilder();
    }

    public InstallSnapshot(final long term, final String leaderId, final long lastIncludedIndex,
        final long lastIncludedTerm, final byte[] data, final int chunkIndex, final int totalChunks, final Optional<Integer> lastChunkHashCode) {
        super(term);
        this.leaderId = leaderId;
        this.lastIncludedIndex = lastIncludedIndex;
        this.lastIncludedTerm = lastIncludedTerm;
        this.data = data;
        this.chunkIndex = chunkIndex;
        this.totalChunks = totalChunks;
        this.lastChunkHashCode = lastChunkHashCode;
        this.builder = null;
    }

    public InstallSnapshot(final long term, final String leaderId, final long lastIncludedIndex,
                           final long lastIncludedTerm, final byte[] data, final int chunkIndex, final int totalChunks) {
        this(term, leaderId, lastIncludedIndex, lastIncludedTerm, data, chunkIndex, totalChunks, Optional.<Integer>absent());
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

    public byte[] getData() {
        return data;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public Optional<Integer> getLastChunkHashCode() {
        return lastChunkHashCode;
    }

    @SuppressWarnings({ "unused", "static-method" })
    private Object readObject(final ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException("ExternalizableProxy should have been used");
    }

    public <T extends Object> Object toSerializable(final short version) {
        if (version >= RaftVersions.BORON_VERSION) {
            return this;
        }

        InstallSnapshotMessages.InstallSnapshot.Builder builder = InstallSnapshotMessages.InstallSnapshot.newBuilder()
                .setTerm(this.getTerm())
                .setLeaderId(this.getLeaderId())
                .setChunkIndex(this.getChunkIndex())
                .setData(ByteString.copyFrom(getData()))
                .setLastIncludedIndex(this.getLastIncludedIndex())
                .setLastIncludedTerm(this.getLastIncludedTerm())
                .setTotalChunks(this.getTotalChunks());

        if (lastChunkHashCode.isPresent()){
            builder.setLastChunkHashCode(lastChunkHashCode.get());
        }
        return builder.build();
    }

    @Override
    public String toString() {
        return "InstallSnapshot [term=" + getTerm() + ", leaderId=" + leaderId + ", lastIncludedIndex="
                + lastIncludedIndex + ", lastIncludedTerm=" + lastIncludedTerm + ", datasize=" + data.length
                + ", Chunk=" + chunkIndex + "/" + totalChunks + ", lastChunkHashCode=" + lastChunkHashCode + "]";
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeShort(RaftVersions.CURRENT_VERSION);
        out.writeLong(getTerm());
        out.writeUTF(leaderId);
        out.writeLong(lastIncludedIndex);
        out.writeLong(lastIncludedTerm);
        out.writeInt(chunkIndex);
        out.writeInt(totalChunks);

        if (lastChunkHashCode.isPresent()) {
            out.writeByte(1);
            out.writeInt(lastChunkHashCode.get());
        } else {
            out.writeByte(0);
        }

        out.writeObject(data);
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        in.readShort(); // raft version - not currently used
        builder.term = in.readLong();
        builder.leaderId = in.readUTF();
        builder.lastIncludedIndex = in.readLong();
        builder.lastIncludedTerm = in.readLong();
        builder.chunkIndex = in.readInt();
        builder.totalChunks = in.readInt();

        if (in.readByte() == 1) {
            builder.lastChunkHashCode = Optional.of(in.readInt());
        } else {
            builder.lastChunkHashCode = Optional.absent();
        }

        builder.data = (byte[])in.readObject();
    }

    private Object readResolve() {
        return builder.build();
    }

    public static InstallSnapshot fromSerializable(final Object o) {
        if(o instanceof InstallSnapshot) {
            return (InstallSnapshot)o;
        } else {
            InstallSnapshotMessages.InstallSnapshot from =
                    (InstallSnapshotMessages.InstallSnapshot) o;

            Optional<Integer> lastChunkHashCode = Optional.absent();
            if(from.hasLastChunkHashCode()){
                lastChunkHashCode = Optional.of(from.getLastChunkHashCode());
            }

            InstallSnapshot installSnapshot = new InstallSnapshot(from.getTerm(),
                    from.getLeaderId(), from.getLastIncludedIndex(),
                    from.getLastIncludedTerm(), from.getData().toByteArray(),
                    from.getChunkIndex(), from.getTotalChunks(), lastChunkHashCode);

            return installSnapshot;
        }
    }

    public static boolean isSerializedType(final Object message) {
        return message instanceof InstallSnapshot || message instanceof InstallSnapshotMessages.InstallSnapshot;
    }
}
