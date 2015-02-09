/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.messages;

public class InstallSnapshotReply extends AbstractRaftRPC {
    private static final long serialVersionUID = 642227896390779503L;

    // The followerId - this will be used to figure out which follower is
    // responding
    private final String followerId;
    private final int chunkIndex;
    private final boolean success;

    public InstallSnapshotReply(long term, String followerId, int chunkIndex,
        boolean success) {
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
        builder.append("InstallSnapshotReply [term=").append(term).append(", followerId=").append(followerId)
                .append(", chunkIndex=").append(chunkIndex).append(", success=").append(success).append("]");
        return builder.toString();
    }
}
