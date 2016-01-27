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
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.raft.RaftVersions;
import org.opendaylight.controller.protobuff.messages.cluster.raft.InstallSnapshotMessages;

public class InstallSnapshot extends AbstractRaftRPC implements Externalizable {
    private static final long serialVersionUID = 1L;
    public static final Class<InstallSnapshotMessages.InstallSnapshot> SERIALIZABLE_CLASS = InstallSnapshotMessages.InstallSnapshot.class;

    private String leaderId;
    private long lastIncludedIndex;
    private long lastIncludedTerm;
    private byte[] data;
    private int chunkIndex;
    private int totalChunks;
    private Optional<Integer> lastChunkHashCode;

    /**
     * Empty constructor to satisfy Externalizable.
     */
    public InstallSnapshot() {
    }

    public InstallSnapshot(long term, String leaderId, long lastIncludedIndex,
        long lastIncludedTerm, byte[] data, int chunkIndex, int totalChunks, Optional<Integer> lastChunkHashCode) {
        super(term);
        this.leaderId = leaderId;
        this.lastIncludedIndex = lastIncludedIndex;
        this.lastIncludedTerm = lastIncludedTerm;
        this.data = data;
        this.chunkIndex = chunkIndex;
        this.totalChunks = totalChunks;
        this.lastChunkHashCode = lastChunkHashCode;
    }

    public InstallSnapshot(long term, String leaderId, long lastIncludedIndex,
                           long lastIncludedTerm, byte[] data, int chunkIndex, int totalChunks) {
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

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeShort(RaftVersions.CURRENT_VERSION);
        out.writeLong(getTerm());
        out.writeUTF(leaderId);
        out.writeLong(lastIncludedIndex);
        out.writeLong(lastIncludedTerm);
        out.writeInt(chunkIndex);
        out.writeInt(totalChunks);

        out.writeByte(lastChunkHashCode.isPresent() ? 1 : 0);
        if(lastChunkHashCode.isPresent()) {
            out.writeInt(lastChunkHashCode.get().intValue());
        }

        out.writeObject(data);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        in.readShort(); // raft version - not currently used
        setTerm(in.readLong());
        leaderId = in.readUTF();
        lastIncludedIndex = in.readLong();
        lastIncludedTerm = in.readLong();
        chunkIndex = in.readInt();
        totalChunks = in.readInt();

        lastChunkHashCode = Optional.absent();
        boolean chunkHashCodePresent = in.readByte() == 1;
        if(chunkHashCodePresent) {
            lastChunkHashCode = Optional.of(in.readInt());
        }

        data = (byte[])in.readObject();
    }

    public <T extends Object> Object toSerializable(short version) {
        if(version >= RaftVersions.BORON_VERSION) {
            return this;
        } else {
            InstallSnapshotMessages.InstallSnapshot.Builder builder = InstallSnapshotMessages.InstallSnapshot.newBuilder()
                    .setTerm(this.getTerm())
                    .setLeaderId(this.getLeaderId())
                    .setChunkIndex(this.getChunkIndex())
                    .setData(ByteString.copyFrom(getData()))
                    .setLastIncludedIndex(this.getLastIncludedIndex())
                    .setLastIncludedTerm(this.getLastIncludedTerm())
                    .setTotalChunks(this.getTotalChunks());

            if(lastChunkHashCode.isPresent()){
                builder.setLastChunkHashCode(lastChunkHashCode.get());
            }
            return builder.build();
        }
    }

    @Override
    public String toString() {
        return "InstallSnapshot [term=" + getTerm() + ", leaderId=" + leaderId + ", lastIncludedIndex="
                + lastIncludedIndex + ", lastIncludedTerm=" + lastIncludedTerm + ", datasize=" + data.length
                + ", Chunk=" + chunkIndex + "/" + totalChunks + ", lastChunkHashCode=" + lastChunkHashCode + "]";
    }

    public static InstallSnapshot fromSerializable (Object o) {
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

    public static boolean isSerializedType(Object message) {
        return message instanceof InstallSnapshot || message instanceof InstallSnapshotMessages.InstallSnapshot;
    }
}
