/*
 * Copyright (c) 2023 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.messages.handlers;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Optional;
import java.util.OptionalInt;
import org.opendaylight.controller.cluster.raft.RaftMessageHandler;
import org.opendaylight.controller.cluster.raft.RaftVersions;
import org.opendaylight.controller.cluster.raft.messages.InstallSnapshot;
import org.opendaylight.controller.cluster.raft.messages.RaftRPC;
import org.opendaylight.controller.cluster.raft.persisted.ServerConfigurationPayload;

public class InstallSnapshotHandler implements RaftMessageHandler {

    private static final int LAST_CHUNK_HASHCODE = 0x10;
    private static final int SERVER_CONFIG       = 0x20;
    @Override
    public void writeTo(final DataOutput out, final RaftRPC message) throws IOException {
        final InstallSnapshot installSnapshot = (InstallSnapshot) message;

        out.writeLong(installSnapshot.getTerm());
        out.writeUTF(installSnapshot.getLeaderId());
        out.writeLong(installSnapshot.getLastIncludedIndex());
        out.writeLong(installSnapshot.getLastIncludedTerm());
        out.writeInt(installSnapshot.getChunkIndex());
        out.writeInt(installSnapshot.getTotalChunks());

        int flags = 0;
        final var lastChunkHashCode = installSnapshot.getLastChunkHashCode();
        if (lastChunkHashCode.isPresent()) {
            flags |= LAST_CHUNK_HASHCODE;
        }
        final Optional<ServerConfigurationPayload> serverConfig = installSnapshot.getServerConfig();
        if (serverConfig.isPresent()) {
            flags |= SERVER_CONFIG;
        }
        out.writeInt(flags);

        if (lastChunkHashCode.isPresent()) {
            out.writeInt(lastChunkHashCode.getAsInt());
        }
        if (serverConfig.isPresent()) {
            final ObjectOutputStream payloadOut = new ObjectOutputStream((OutputStream) out);
            payloadOut.writeObject(serverConfig);
        }

        out.write(installSnapshot.getData().length);
        out.write(installSnapshot.getData());
    }

    @Override
    public RaftRPC readFrom(final DataInput in) throws IOException, ClassNotFoundException {
        final long term = in.readLong();
        final String leaderId = in.readUTF();
        final long lastIncludedIndex = in.readLong();
        final long lastIncludedTerm = in.readLong();
        final int chunkIndex = in.readInt();
        final int totalChunks = in.readInt();

        final int flags = in.readInt();
        final OptionalInt lastChunkHashCode = getFlag(flags, LAST_CHUNK_HASHCODE)
                ? OptionalInt.of(in.readInt())
                : OptionalInt.empty();

        Optional<ServerConfigurationPayload> serverConfig;
        if (getFlag(flags, SERVER_CONFIG)) {
            final ObjectInputStream payloadIn = new ObjectInputStream((InputStream) in);
            final Object payload = payloadIn.readObject();
            if (!(payload instanceof ServerConfigurationPayload)) {
                throw new IllegalArgumentException("Deserialization error: serverConfig not instance of "
                        + "ServerConfigurationPayload");
            }
            serverConfig = Optional.of((ServerConfigurationPayload)payload);
        } else {
            serverConfig = Optional.empty();
        }
        final int dataLength = in.readInt();
        final byte[] data = new byte[dataLength];
        in.readFully(data);

        return new InstallSnapshot(term, leaderId, lastIncludedIndex, lastIncludedTerm, data,
                chunkIndex, totalChunks, lastChunkHashCode, serverConfig, RaftVersions.CURRENT_VERSION);
    }

    private boolean getFlag(final int flags, final int bit) {
        return (flags & bit) != 0;
    }
}