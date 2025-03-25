/*
 * Copyright (c) 2022 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import static com.google.common.base.Verify.verifyNotNull;
import static java.util.Objects.requireNonNull;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.OptionalInt;
import org.opendaylight.controller.cluster.raft.RaftVersions;
import org.opendaylight.controller.cluster.raft.persisted.ClusterConfig;
import org.opendaylight.yangtools.concepts.WritableObjects;

/**
 * Serialization proxy for {@link InstallSnapshot}.
 */
final class IS implements Externalizable {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    // Flags
    private static final int LAST_CHUNK_HASHCODE = 0x10;
    private static final int SERVER_CONFIG       = 0x20;

    private InstallSnapshot installSnapshot;

    @SuppressWarnings("checkstyle:RedundantModifier")
    public IS() {
        // For Externalizable
    }

    IS(final InstallSnapshot installSnapshot) {
        this.installSnapshot = requireNonNull(installSnapshot);
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        int flags = 0;
        final var lastChunkHashCode = installSnapshot.getLastChunkHashCode();
        if (lastChunkHashCode.isPresent()) {
            flags |= LAST_CHUNK_HASHCODE;
        }
        final var serverConfig = installSnapshot.serverConfig();
        if (serverConfig != null) {
            flags |= SERVER_CONFIG;
        }

        WritableObjects.writeLong(out, installSnapshot.getTerm(), flags);
        out.writeObject(installSnapshot.getLeaderId());
        WritableObjects.writeLongs(out, installSnapshot.getLastIncludedIndex(), installSnapshot.getLastIncludedTerm());
        out.writeInt(installSnapshot.getChunkIndex());
        out.writeInt(installSnapshot.getTotalChunks());

        if (lastChunkHashCode.isPresent()) {
            out.writeInt(lastChunkHashCode.orElseThrow());
        }
        if (serverConfig != null) {
            out.writeObject(serverConfig);
        }

        out.writeObject(installSnapshot.getData());
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        byte hdr = WritableObjects.readLongHeader(in);
        final int flags = WritableObjects.longHeaderFlags(hdr);

        long term = WritableObjects.readLongBody(in, hdr);
        String leaderId = (String) in.readObject();

        hdr = WritableObjects.readLongHeader(in);
        long lastIncludedIndex = WritableObjects.readFirstLong(in, hdr);
        long lastIncludedTerm = WritableObjects.readSecondLong(in, hdr);
        int chunkIndex = in.readInt();
        int totalChunks = in.readInt();

        OptionalInt lastChunkHashCode = getFlag(flags, LAST_CHUNK_HASHCODE) ? OptionalInt.of(in.readInt())
            : OptionalInt.empty();
        ClusterConfig serverConfig = getFlag(flags, SERVER_CONFIG)
                ? requireNonNull((ClusterConfig) in.readObject()) : null;

        byte[] data = (byte[])in.readObject();

        installSnapshot = new InstallSnapshot(term, leaderId, lastIncludedIndex, lastIncludedTerm, data,
                chunkIndex, totalChunks, lastChunkHashCode, serverConfig, RaftVersions.CURRENT_VERSION);
    }

    @java.io.Serial
    private Object readResolve() {
        return verifyNotNull(installSnapshot);
    }

    private static boolean getFlag(final int flags, final int bit) {
        return (flags & bit) != 0;
    }
}

