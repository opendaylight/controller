/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.messages;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.raft.RaftVersions;
import org.opendaylight.controller.protobuff.messages.cluster.raft.InstallSnapshotMessages;

public final class InstallSnapshot extends AbstractRaftRPC {
    private static final class ExternalizableProxy implements Externalizable {
        private static final long serialVersionUID = 1L;

        // Used when acting as the writeout proxy
        private final InstallSnapshot original;

        // Used when acting as a readin proxy
        private long term;
        private String leaderId;
        private long lastIncludedIndex;
        private long lastIncludedTerm;
        private int chunkIndex;
        private int totalChunks;
        private Optional<Integer> lastChunkHashCode;
        private byte[] data;

        /**
         * Empty constructor to satisfy Externalizable.
         */
        public ExternalizableProxy() {
            original = null;
        }

        ExternalizableProxy(final InstallSnapshot original) {
            this.original = Preconditions.checkNotNull(original);
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeShort(RaftVersions.CURRENT_VERSION);
            out.writeLong(original.getTerm());
            out.writeUTF(original.getLeaderId());
            out.writeLong(original.getLastIncludedIndex());
            out.writeLong(original.getLastIncludedTerm());
            out.writeInt(original.getChunkIndex());
            out.writeInt(original.getTotalChunks());

            final Optional<Integer> maybeHash = original.getLastChunkHashCode();
            if (maybeHash.isPresent()) {
                out.writeByte(1);
                out.writeInt(maybeHash.get());
            } else {
                out.writeByte(0);
            }

            out.writeObject(original.getData());
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            in.readShort(); // raft version - not currently used
            term = in.readLong();
            leaderId = in.readUTF();
            lastIncludedIndex = in.readLong();
            lastIncludedTerm = in.readLong();
            chunkIndex = in.readInt();
            totalChunks = in.readInt();

            lastChunkHashCode = Optional.absent();
            boolean chunkHashCodePresent = in.readByte() == 1;
            if (chunkHashCodePresent) {
                lastChunkHashCode = Optional.of(in.readInt());
            } else {
                lastChunkHashCode = Optional.absent();
            }

            data = (byte[])in.readObject();
        }

        private Object readResolve() {
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

    private Object writeReplace() {
        return new ExternalizableProxy(this);
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
