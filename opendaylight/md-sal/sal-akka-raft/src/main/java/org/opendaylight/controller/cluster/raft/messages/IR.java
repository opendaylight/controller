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
import org.opendaylight.yangtools.concepts.WritableObjects;

/**
 * Serialization proxy for {@link InstallSnapshotReply}.
 */
final class IR implements Externalizable {
    @java.io.Serial
    private static final long serialVersionUID = 1L;

    // Flags
    private static final int SUCCESS = 0x10;

    private InstallSnapshotReply installSnapshotReply;

    @SuppressWarnings("checkstyle:RedundantModifier")
    public IR() {
        // For Externalizable
    }

    IR(final InstallSnapshotReply installSnapshotReply) {
        this.installSnapshotReply = requireNonNull(installSnapshotReply);
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        WritableObjects.writeLong(out, installSnapshotReply.getTerm(), installSnapshotReply.isSuccess() ? SUCCESS : 0);
        out.writeObject(installSnapshotReply.getFollowerId());
        out.writeInt(installSnapshotReply.getChunkIndex());
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        final byte hdr = WritableObjects.readLongHeader(in);
        final int flags = WritableObjects.longHeaderFlags(hdr);

        long term = WritableObjects.readLongBody(in, hdr);
        String followerId = (String) in.readObject();
        int chunkIndex = in.readInt();

        installSnapshotReply = new InstallSnapshotReply(term, followerId, chunkIndex, (flags & SUCCESS) != 0);
    }

    @java.io.Serial
    private Object readResolve() {
        return verifyNotNull(installSnapshotReply);
    }
}
