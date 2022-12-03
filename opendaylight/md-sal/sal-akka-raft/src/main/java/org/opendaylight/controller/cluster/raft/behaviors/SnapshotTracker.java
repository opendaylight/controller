/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.behaviors;

import static java.util.Objects.requireNonNull;

import com.google.common.io.ByteSource;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.OptionalInt;
import org.opendaylight.controller.cluster.io.FileBackedOutputStream;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.slf4j.Logger;

/**
 * Helper class that maintains state for a snapshot that is being installed in chunks on a Follower.
 */
class SnapshotTracker implements AutoCloseable {
    private final Logger log;
    private final int totalChunks;
    private final String leaderId;
    private final BufferedOutputStream bufferedStream;
    private final FileBackedOutputStream fileBackedStream;
    private int lastChunkIndex = LeaderInstallSnapshotState.FIRST_CHUNK_INDEX - 1;
    private boolean sealed = false;
    private int lastChunkHashCode = LeaderInstallSnapshotState.INITIAL_LAST_CHUNK_HASH_CODE;
    private long count;

    SnapshotTracker(final Logger log, final int totalChunks, final String leaderId, final RaftActorContext context) {
        this.log = log;
        this.totalChunks = totalChunks;
        this.leaderId = requireNonNull(leaderId);
        fileBackedStream = context.getFileBackedOutputStreamFactory().newInstance();
        bufferedStream = new BufferedOutputStream(fileBackedStream);
    }

    /**
     * Adds a chunk to the tracker.
     *
     * @param chunkIndex the index of the chunk
     * @param chunk the chunk data
     * @param lastChunkHashCode the optional hash code for the chunk
     * @return true if this is the last chunk is received
     * @throws InvalidChunkException if the chunk index is invalid or out of order
     * @throws IOException if there is a problem writing to the stream
     */
    boolean addChunk(final int chunkIndex, final byte[] chunk, final OptionalInt maybeLastChunkHashCode)
            throws IOException {
        log.debug("addChunk: chunkIndex={}, lastChunkIndex={}, collectedChunks.size={}, lastChunkHashCode={}",
                chunkIndex, lastChunkIndex, count, lastChunkHashCode);

        if (sealed) {
            throw new InvalidChunkException("Invalid chunk received with chunkIndex " + chunkIndex
                    + " all chunks already received");
        }

        if (lastChunkIndex + 1 != chunkIndex) {
            throw new InvalidChunkException("Expected chunkIndex " + (lastChunkIndex + 1) + " got " + chunkIndex);
        }

        if (maybeLastChunkHashCode.isPresent() && maybeLastChunkHashCode.getAsInt() != lastChunkHashCode) {
            throw new InvalidChunkException("The hash code of the recorded last chunk does not match "
                    + "the senders hash code, expected " + lastChunkHashCode + " was "
                    + maybeLastChunkHashCode.getAsInt());
        }

        bufferedStream.write(chunk);

        count += chunk.length;
        sealed = chunkIndex == totalChunks;
        lastChunkIndex = chunkIndex;
        lastChunkHashCode = Arrays.hashCode(chunk);
        return sealed;
    }

    ByteSource getSnapshotBytes() throws IOException {
        if (!sealed) {
            throw new IllegalStateException("lastChunk not received yet");
        }

        bufferedStream.close();
        return fileBackedStream.asByteSource();
    }

    String getLeaderId() {
        return leaderId;
    }

    @Override
    public void close() {
        fileBackedStream.cleanup();
    }

    public static class InvalidChunkException extends IOException {
        private static final long serialVersionUID = 1L;

        InvalidChunkException(final String message) {
            super(message);
        }
    }
}
