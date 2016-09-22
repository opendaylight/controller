/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.messages;

import com.google.common.base.Optional;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.opendaylight.controller.cluster.raft.persisted.ServerConfigurationPayload;

public class InstallSnapshot extends AbstractRaftRPC {
    private static final long serialVersionUID = 1L;

    private final String leaderId;
    private final long lastIncludedIndex;
    private final long lastIncludedTerm;
    private final byte[] data;
    private final int chunkIndex;
    private final int totalChunks;
    private final Optional<Integer> lastChunkHashCode;
    private final Optional<ServerConfigurationPayload> serverConfig;

    public InstallSnapshot(long term, String leaderId, long lastIncludedIndex, long lastIncludedTerm, byte[] data,
            int chunkIndex, int totalChunks, Optional<Integer> lastChunkHashCode, Optional<ServerConfigurationPayload> serverConfig) {
        super(term);
        this.leaderId = leaderId;
        this.lastIncludedIndex = lastIncludedIndex;
        this.lastIncludedTerm = lastIncludedTerm;
        this.data = data;
        this.chunkIndex = chunkIndex;
        this.totalChunks = totalChunks;
        this.lastChunkHashCode = lastChunkHashCode;
        this.serverConfig = serverConfig;
    }

    public InstallSnapshot(long term, String leaderId, long lastIncludedIndex,
                           long lastIncludedTerm, byte[] data, int chunkIndex, int totalChunks) {
        this(term, leaderId, lastIncludedIndex, lastIncludedTerm, data, chunkIndex, totalChunks,
                Optional.<Integer>absent(), Optional.<ServerConfigurationPayload>absent());
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

    public Optional<ServerConfigurationPayload> getServerConfig() {
        return serverConfig;
    }


    public <T> Object toSerializable(short version) {
        return this;
    }

    @Override
    public String toString() {
        return "InstallSnapshot [term=" + getTerm() + ", leaderId=" + leaderId + ", lastIncludedIndex="
                + lastIncludedIndex + ", lastIncludedTerm=" + lastIncludedTerm + ", datasize=" + data.length
                + ", Chunk=" + chunkIndex + "/" + totalChunks + ", lastChunkHashCode=" + lastChunkHashCode
                + ", serverConfig=" + serverConfig.orNull() + "]";
    }

    private Object writeReplace() {
        return new Proxy(this);
    }

    private static class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;

        private InstallSnapshot installSnapshot;

        public Proxy() {
        }

        Proxy(InstallSnapshot installSnapshot) {
            this.installSnapshot = installSnapshot;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeLong(installSnapshot.getTerm());
            out.writeObject(installSnapshot.leaderId);
            out.writeLong(installSnapshot.lastIncludedIndex);
            out.writeLong(installSnapshot.lastIncludedTerm);
            out.writeInt(installSnapshot.chunkIndex);
            out.writeInt(installSnapshot.totalChunks);

            out.writeByte(installSnapshot.lastChunkHashCode.isPresent() ? 1 : 0);
            if(installSnapshot.lastChunkHashCode.isPresent()) {
                out.writeInt(installSnapshot.lastChunkHashCode.get().intValue());
            }

            out.writeByte(installSnapshot.serverConfig.isPresent() ? 1 : 0);
            if(installSnapshot.serverConfig.isPresent()) {
                out.writeObject(installSnapshot.serverConfig.get());
            }

            out.writeObject(installSnapshot.data);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            long term = in.readLong();
            String leaderId = (String) in.readObject();
            long lastIncludedIndex = in.readLong();
            long lastIncludedTerm = in.readLong();
            int chunkIndex = in.readInt();
            int totalChunks = in.readInt();

            Optional<Integer> lastChunkHashCode = Optional.absent();
            boolean chunkHashCodePresent = in.readByte() == 1;
            if(chunkHashCodePresent) {
                lastChunkHashCode = Optional.of(in.readInt());
            }

            Optional<ServerConfigurationPayload> serverConfig = Optional.absent();
            boolean serverConfigPresent = in.readByte() == 1;
            if(serverConfigPresent) {
                serverConfig = Optional.of((ServerConfigurationPayload)in.readObject());
            }

            byte[] data = (byte[])in.readObject();

            installSnapshot = new InstallSnapshot(term, leaderId, lastIncludedIndex, lastIncludedTerm, data,
                    chunkIndex, totalChunks, lastChunkHashCode, serverConfig);
        }

        private Object readResolve() {
            return installSnapshot;
        }
    }
}
