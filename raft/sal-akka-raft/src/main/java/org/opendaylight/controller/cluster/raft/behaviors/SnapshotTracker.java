/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.behaviors;

import static java.util.Objects.requireNonNull;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.OptionalInt;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.cluster.raft.RaftActorContext;
import org.opendaylight.raft.spi.FileBackedOutputStream;
import org.opendaylight.raft.spi.InputStreamProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class that maintains state for a snapshot that is being installed in chunks on a Follower.
 */
class SnapshotTracker implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(SnapshotTracker.class);

    private final int totalChunks;
    private final String leaderId;
    private final BufferedOutputStream bufferedStream;
    private final FileBackedOutputStream fileBackedStream;
    private final @NonNull String logName;

    private int lastChunkIndex = LeaderInstallSnapshotState.FIRST_CHUNK_INDEX - 1;
    private boolean sealed = false;
    private int lastChunkHashCode = LeaderInstallSnapshotState.INITIAL_LAST_CHUNK_HASH_CODE;
    private long count;

    SnapshotTracker(final String logName, final int totalChunks, final String leaderId,
            final RaftActorContext context) {
        this.logName = requireNonNull(logName);
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
        LOG.debug("{}: addChunk: chunkIndex={}, lastChunkIndex={}, collectedChunks.size={}, lastChunkHashCode={}",
            logName, chunkIndex, lastChunkIndex, count, lastChunkHashCode);

        if (sealed) {
            throw new InvalidChunkException("Invalid chunk received with chunkIndex " + chunkIndex
                    + " all chunks already received");
        }

        if (lastChunkIndex + 1 != chunkIndex) {
            throw new InvalidChunkException("Expected chunkIndex " + (lastChunkIndex + 1) + " got " + chunkIndex);
        }

        if (maybeLastChunkHashCode.isPresent()) {
            final var actualChunkHashCode = maybeLastChunkHashCode.orElseThrow();
            if (actualChunkHashCode != lastChunkHashCode) {
                throw new InvalidChunkException("The hash code of the recorded last chunk does not match the sender's "
                    + "hash code, expected " + lastChunkHashCode + " was " + actualChunkHashCode);
            }
        }

        bufferedStream.write(chunk);

        count += chunk.length;
        sealed = chunkIndex == totalChunks;
        lastChunkIndex = chunkIndex;
        lastChunkHashCode = Arrays.hashCode(chunk);
        return sealed;
    }

    // FIXME: InputStreamProvider
    @NonNull InputStreamProvider getSnapshotBytes() throws IOException {
        if (!sealed) {
            throw new IllegalStateException("lastChunk not received yet");
        }

        bufferedStream.close();
        return fileBackedStream.asByteSource()::openStream;
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
