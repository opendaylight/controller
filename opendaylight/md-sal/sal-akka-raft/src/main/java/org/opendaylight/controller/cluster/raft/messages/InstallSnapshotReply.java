/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import static java.util.Objects.requireNonNull;

public final class InstallSnapshotReply extends AbstractRaftRPC {
    /**
     * The kind of a particular reply.
     */
    public enum Kind {
        /**
         * The request was completed successfully.
         */
        SUCCESS,
        /**
         * The request failed.
         */
        FAILURE,
        /**
         * The request was received
         */
        RECEIVED;
    }

    @java.io.Serial
    private static final long serialVersionUID = 642227896390779503L;

    // The followerId - this will be used to figure out which follower is
    // responding
    private final String followerId;
    private final int chunkIndex;
    private final Kind kind;

    public InstallSnapshotReply(final long term, final String followerId, final int chunkIndex, final Kind kind) {
        super(term);
        this.followerId = followerId;
        this.chunkIndex = chunkIndex;
        this.kind = requireNonNull(kind);
    }

    public String getFollowerId() {
        return followerId;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public Kind getKind() {
        return kind;
    }

    @Override
    public String toString() {
        return "InstallSnapshotReply [term=" + getTerm()
                + ", followerId=" + followerId
                + ", chunkIndex=" + chunkIndex
                + ", kind=" + kind + "]";
    }

    @Override
    Object writeReplace() {
        return new IR(this);
    }
}
