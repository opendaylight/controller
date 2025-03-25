/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import com.google.common.base.MoreObjects.ToStringHelper;

public final class InstallSnapshotReply extends RaftRPC {
    @java.io.Serial
    private static final long serialVersionUID = 642227896390779503L;

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
    ToStringHelper addToStringAttributes(final ToStringHelper helper) {
        return super.addToStringAttributes(helper)
            .add("followerId", followerId)
            .add("chunkIndex", chunkIndex)
            .add("success", success);
    }

    @Override
    Object writeReplace() {
        return new IR(this);
    }
}
