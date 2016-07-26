/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.messages;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class InstallSnapshotReply extends AbstractRaftRPC {
    private static final long serialVersionUID = 642227896390779503L;

    // The followerId - this will be used to figure out which follower is
    // responding
    private final String followerId;
    private final int chunkIndex;
    private final boolean success;

    public InstallSnapshotReply(long term, String followerId, int chunkIndex, boolean success) {
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
        StringBuilder builder = new StringBuilder();
        builder.append("InstallSnapshotReply [term=").append(getTerm()).append(", followerId=").append(followerId)
                .append(", chunkIndex=").append(chunkIndex).append(", success=").append(success).append("]");
        return builder.toString();
    }

    private Object writeReplace() {
        return new Proxy(this);
    }

    private static class Proxy implements Externalizable {
        private static final long serialVersionUID = 1L;

        private InstallSnapshotReply installSnapshotReply;

        public Proxy() {
        }

        Proxy(InstallSnapshotReply installSnapshotReply) {
            this.installSnapshotReply = installSnapshotReply;
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeLong(installSnapshotReply.getTerm());
            out.writeObject(installSnapshotReply.followerId);
            out.writeInt(installSnapshotReply.chunkIndex);
            out.writeBoolean(installSnapshotReply.success);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            long term = in.readLong();
            String followerId = (String) in.readObject();
            int chunkIndex = in.readInt();
            boolean success = in.readBoolean();

            installSnapshotReply = new InstallSnapshotReply(term, followerId, chunkIndex, success);
        }

        private Object readResolve() {
            return installSnapshotReply;
        }
    }
}
