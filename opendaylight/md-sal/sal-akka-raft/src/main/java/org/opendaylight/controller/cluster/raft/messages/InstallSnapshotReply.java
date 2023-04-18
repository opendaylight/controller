/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.concepts.WritableObjects;

public final class InstallSnapshotReply extends AbstractRaftRPC {
    private static final long serialVersionUID = 642227896390779503L;
    // Flags
    private static final int SUCCESS = 0x10;

    // The followerId - this will be used to figure out which follower is
    // responding
    private final String followerId;
    private final int chunkIndex;
    private final boolean success;

    public InstallSnapshotReply(final long term, final String followerId, final int chunkIndex, final boolean success) {
        super(term);
        this.followerId = followerId;
        this.chunkIndex = chunkIndex;
        this.success = success;
    }

    public String getFollowerId() {
        return followerId;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public boolean isSuccess() {
        return success;
    }

    @Override
    public String toString() {
        return "InstallSnapshotReply [term=" + getTerm()
                + ", followerId=" + followerId
                + ", chunkIndex=" + chunkIndex
                + ", success=" + success + "]";
    }

    @Override
    Object writeReplace() {
        return new IR(this);
    }

    @Override
    public void writeTo(DataOutput out) throws IOException {
        WritableObjects.writeLong(out, getTerm(), isSuccess() ? SUCCESS : 0);
        out.writeUTF(getFollowerId());
        out.writeInt(getChunkIndex());
    }

    public static @NonNull InstallSnapshotReply readFrom(final DataInput in) throws IOException {
        final byte hdr = WritableObjects.readLongHeader(in);
        final int flags = WritableObjects.longHeaderFlags(hdr);

        long term = WritableObjects.readLongBody(in, hdr);
        String followerId = in.readUTF();
        int chunkIndex = in.readInt();

        return new InstallSnapshotReply(term, followerId, chunkIndex, (flags & SUCCESS) != 0);
    }

    @Deprecated(since = "7.0.0", forRemoval = true)
    private static class Proxy implements Externalizable {
        @java.io.Serial
        private static final long serialVersionUID = 1L;

        private InstallSnapshotReply installSnapshotReply;

        // checkstyle flags the public modifier as redundant which really doesn't make sense since it clearly isn't
        // redundant. It is explicitly needed for Java serialization to be able to create instances via reflection.
        @SuppressWarnings("checkstyle:RedundantModifier")
        public Proxy() {
        }

        Proxy(final InstallSnapshotReply installSnapshotReply) {
            this.installSnapshotReply = installSnapshotReply;
        }

        @Override
        public void writeExternal(final ObjectOutput out) throws IOException {
            out.writeLong(installSnapshotReply.getTerm());
            out.writeObject(installSnapshotReply.followerId);
            out.writeInt(installSnapshotReply.chunkIndex);
            out.writeBoolean(installSnapshotReply.success);
        }

        @Override
        public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
            long term = in.readLong();
            String followerId = (String) in.readObject();
            int chunkIndex = in.readInt();
            boolean success = in.readBoolean();

            installSnapshotReply = new InstallSnapshotReply(term, followerId, chunkIndex, success);
        }

        @java.io.Serial
        private Object readResolve() {
            return installSnapshotReply;
        }
    }
}
