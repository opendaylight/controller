/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.messages;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects.ToStringHelper;

/**
 * The response to an {@link InstallSnapshot} request. According to Figure 13 of
 * <a href="https://raft.github.io/raft.pdf">the RAFT paper</a>, a receiver implementation should:
 * <ol>
 *   <li>Reply immediately if {@code term < currentTerm}</li>
 *   <li>Create new snapshot file if first chunk ({@code offset} is {@code 0})</li>
 *   <li>Write data into snapshot file at given {@code offset}</li>
 *   <li>Reply and wait for more data chunks if {@code done} is {@code false}</li>
 *   <li>Save snapshot file, discard any existing or partial snapshot with a smaller {@code index}</li>
 *   <li>If existing log entry has same {@code index} and {@code term} as snapshot’s last included entry, retain log
 *       entries following it and reply</li>
 *   <li>Discard the entire log</li>
 *   <li>Reset state machine using snapshot contents (and load snapshot’s cluster configuration)</li>
 * </ol>
 *
 * <p>Our transfer tracking differs significantly: we are applying a sequence of variable-sized
 * {@link InstallSnapshot#getData()}, each of which has a {@link InstallSnapshot#getChunkIndex()} within
 * {@link InstallSnapshot#getTotalChunks()}. Chunks are transmitted in sequence.
 */
public final class InstallSnapshotReply extends RaftRPC {
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

    // The followerId - this will be used to figure out which follower is responding
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
    ToStringHelper addToStringAttributes(final ToStringHelper helper) {
        return super.addToStringAttributes(helper)
            .add("followerId", followerId)
            .add("chunkIndex", chunkIndex)
            .add("kind", kind);
    }

    @Override
    Object writeReplace() {
        return new IR(this);
    }
}
