/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import static java.util.Objects.requireNonNull;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Optional;
import java.util.OptionalInt;
import org.opendaylight.controller.cluster.raft.persisted.ServerConfigurationPayload;

/**
 * Sodium Externalizable proxy for InstallSnapshot.
 */
final class ISv2 implements Externalizable {
    private static final long serialVersionUID = 1L;

    private InstallSnapshot installSnapshot;

    @SuppressWarnings("checkstyle:RedundantModifier")
    public ISv2() {
        // For Externalizable
    }

    ISv2(final InstallSnapshot installSnapshot) {
        this.installSnapshot = requireNonNull(installSnapshot);
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeLong(installSnapshot.getTerm());
        out.writeObject(installSnapshot.getLeaderId());
        out.writeLong(installSnapshot.getLastIncludedIndex());
        out.writeLong(installSnapshot.getLastIncludedTerm());
        out.writeInt(installSnapshot.getChunkIndex());
        out.writeInt(installSnapshot.getTotalChunks());

        final OptionalInt lastChunkHashCode = installSnapshot.getLastChunkHashCode();
        out.writeByte(lastChunkHashCode.isPresent() ? 1 : 0);
        if (lastChunkHashCode.isPresent()) {
            out.writeInt(lastChunkHashCode.getAsInt());
        }

        final Optional<ServerConfigurationPayload> serverConfig = installSnapshot.getServerConfig();
        out.writeByte(serverConfig.isPresent() ? 1 : 0);
        if (serverConfig.isPresent()) {
            out.writeObject(serverConfig.get());
        }

        out.writeObject(installSnapshot.getData());
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
                chunkIndex, totalChunks, lastChunkHashCode, serverConfig);
    }

    private Object readResolve() {
        return installSnapshot;
    }
}
