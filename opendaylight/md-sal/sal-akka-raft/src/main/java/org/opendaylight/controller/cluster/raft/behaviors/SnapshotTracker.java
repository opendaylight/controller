/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.cluster.raft.behaviors;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteSource;
import com.google.common.io.CountingOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import org.slf4j.Logger;

/**
 * Helper class that maintains state for a snapshot that is being installed in chunks on a Follower.
 */
class SnapshotTracker implements AutoCloseable {
    private final Logger log;
    private final int totalChunks;
    private final String leaderId;
    private final CountingOutputStream countingStream;
    private final ByteArrayOutputStream backingStream;
    private int lastChunkIndex = LeaderInstallSnapshotState.FIRST_CHUNK_INDEX - 1;
    private boolean sealed = false;
    private int lastChunkHashCode = LeaderInstallSnapshotState.INITIAL_LAST_CHUNK_HASH_CODE;

    SnapshotTracker(Logger log, int totalChunks, String leaderId) {
        this.log = log;
        this.totalChunks = totalChunks;
        this.leaderId = Preconditions.checkNotNull(leaderId);

        backingStream = new ByteArrayOutputStream();
        countingStream = new CountingOutputStream(backingStream);
    }

    /**
     * Adds a chunk to the tracker.
     *
     * @param chunkIndex the index of the chunk
     * @param chunk the chunk data
     * @param lastChunkHashCode the optional hash code for the chunk
     * @return true if this is the last chunk is received
     * @throws InvalidChunkException if the chunk index is invalid or out of order
     */
    boolean addChunk(int chunkIndex, byte[] chunk, Optional<Integer> maybeLastChunkHashCode)
            throws InvalidChunkException, IOException {
        log.debug("addChunk: chunkIndex={}, lastChunkIndex={}, collectedChunks.size={}, lastChunkHashCode={}",
                chunkIndex, lastChunkIndex, countingStream.getCount(), this.lastChunkHashCode);

        if (sealed) {
            throw new InvalidChunkException("Invalid chunk received with chunkIndex " + chunkIndex
                    + " all chunks already received");
        }

        if (lastChunkIndex + 1 != chunkIndex) {
            throw new InvalidChunkException("Expected chunkIndex " + (lastChunkIndex + 1) + " got " + chunkIndex);
        }

        if (maybeLastChunkHashCode.isPresent() && maybeLastChunkHashCode.get() != this.lastChunkHashCode) {
            throw new InvalidChunkException("The hash code of the recorded last chunk does not match "
                    + "the senders hash code, expected " + this.lastChunkHashCode + " was "
                    + maybeLastChunkHashCode.get());
        }

        countingStream.write(chunk);

        sealed = chunkIndex == totalChunks;
        lastChunkIndex = chunkIndex;
        this.lastChunkHashCode = Arrays.hashCode(chunk);
        return sealed;
    }

    ByteSource getSnapshotBytes() {
        if (!sealed) {
            throw new IllegalStateException("lastChunk not received yet");
        }

        return ByteSource.wrap(backingStream.toByteArray());
    }

    String getLeaderId() {
        return leaderId;
    }

    @Override
    public void close() {
        try {
            countingStream.close();
        } catch (IOException e) {
            log.warn("Error closing snapshot stream");
        }
    }

    public static class InvalidChunkException extends IOException {
        private static final long serialVersionUID = 1L;

        InvalidChunkException(String message) {
            super(message);
        }
    }
}
