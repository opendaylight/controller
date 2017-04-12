/*
 * Copyright (c) 2016 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.behaviors;

import com.google.common.base.Throwables;
import com.google.common.io.ByteSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encapsulates the leader state and logic for sending snapshot chunks to a follower.
 */
public final class LeaderInstallSnapshotState implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(LeaderInstallSnapshotState.class);

    // The index of the first chunk that is sent when installing a snapshot
    static final int FIRST_CHUNK_INDEX = 1;

    // The index that the follower should respond with if it needs the install snapshot to be reset
    static final int INVALID_CHUNK_INDEX = -1;

    // This would be passed as the hash code of the last chunk when sending the first chunk
    static final int INITIAL_LAST_CHUNK_HASH_CODE = -1;

    private final int snapshotChunkSize;
    private final String logName;
    private ByteSource snapshotBytes;
    private int offset = 0;
    // the next snapshot chunk is sent only if the replyReceivedForOffset matches offset
    private int replyReceivedForOffset = -1;
    // if replyStatus is false, the previous chunk is attempted
    private boolean replyStatus = false;
    private int chunkIndex = FIRST_CHUNK_INDEX;
    private int totalChunks;
    private int lastChunkHashCode = INITIAL_LAST_CHUNK_HASH_CODE;
    private int nextChunkHashCode = INITIAL_LAST_CHUNK_HASH_CODE;
    private long snapshotSize;
    private InputStream snapshotInputStream;

    LeaderInstallSnapshotState(int snapshotChunkSize, String logName) {
        this.snapshotChunkSize = snapshotChunkSize;
        this.logName = logName;
    }

    void setSnapshotBytes(ByteSource snapshotBytes) throws IOException {
        if (this.snapshotBytes != null) {
            return;
        }

        snapshotSize = snapshotBytes.size();
        snapshotInputStream = snapshotBytes.openStream();

        this.snapshotBytes = snapshotBytes;

        totalChunks = (int) (snapshotSize / snapshotChunkSize + (snapshotSize % snapshotChunkSize > 0 ? 1 : 0));

        LOG.debug("{}: Snapshot {} bytes, total chunks to send: {}", logName, snapshotSize, totalChunks);

        replyReceivedForOffset = -1;
        chunkIndex = FIRST_CHUNK_INDEX;
    }

    int incrementOffset() {
        if (replyStatus) {
            // if prev chunk failed, we would want to sent the same chunk again
            offset = offset + snapshotChunkSize;
        }
        return offset;
    }

    int incrementChunkIndex() {
        if (replyStatus) {
            // if prev chunk failed, we would want to sent the same chunk again
            chunkIndex =  chunkIndex + 1;
        }
        return chunkIndex;
    }

    int getChunkIndex() {
        return chunkIndex;
    }

    int getTotalChunks() {
        return totalChunks;
    }

    boolean canSendNextChunk() {
        // we only send a false if a chunk is sent but we have not received a reply yet
        return snapshotBytes != null && (nextChunkHashCode == INITIAL_LAST_CHUNK_HASH_CODE
                || replyReceivedForOffset == offset);
    }

    boolean isLastChunk(int index) {
        return totalChunks == index;
    }

    void markSendStatus(boolean success) {
        if (success) {
            // if the chunk sent was successful
            replyReceivedForOffset = offset;
            replyStatus = true;
            lastChunkHashCode = nextChunkHashCode;
        } else {
            // if the chunk sent was failure
            replyReceivedForOffset = offset;
            replyStatus = false;
        }
    }

    byte[] getNextChunk() throws IOException {
        int start = incrementOffset();
        int size = snapshotChunkSize;
        if (snapshotChunkSize > snapshotSize) {
            size = (int) snapshotSize;
        } else if (start + snapshotChunkSize > snapshotSize) {
            size = (int) (snapshotSize - start);
        }

        byte[] nextChunk = new byte[size];
        int numRead = snapshotInputStream.read(nextChunk);
        if (numRead != size) {
            throw new IOException(String.format(
                    "The # of bytes read from the input stream, %d, does not match the expected # %d", numRead, size));
        }

        nextChunkHashCode = Arrays.hashCode(nextChunk);

        LOG.debug("{}: Next chunk: total length={}, offset={}, size={}, hashCode={}", logName,
                snapshotSize, start, size, nextChunkHashCode);
        return nextChunk;
    }

    /**
     * Reset should be called when the Follower needs to be sent the snapshot from the beginning.
     */
    void reset() {
        closeStream();

        offset = 0;
        replyStatus = false;
        replyReceivedForOffset = offset;
        chunkIndex = FIRST_CHUNK_INDEX;
        lastChunkHashCode = INITIAL_LAST_CHUNK_HASH_CODE;

        try {
            snapshotInputStream = snapshotBytes.openStream();
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void close() {
        closeStream();
        snapshotBytes = null;
    }

    private void closeStream() {
        if (snapshotInputStream != null) {
            try {
                snapshotInputStream.close();
            } catch (IOException e) {
                LOG.warn("{}: Error closing snapshot stream", logName);
            }

            snapshotInputStream = null;
        }
    }

    int getLastChunkHashCode() {
        return lastChunkHashCode;
    }
}
