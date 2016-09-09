/*
 * Copyright (c) 2016 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.behaviors;

import com.google.protobuf.ByteString;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encapsulates the leader state and logic for sending snapshot chunks to a follower.
 */
public class LeaderInstallSnapshotState {
    private static final Logger LOG = LoggerFactory.getLogger(LeaderInstallSnapshotState.class);

    // The index of the first chunk that is sent when installing a snapshot
    static final int FIRST_CHUNK_INDEX = 1;

    // The index that the follower should respond with if it needs the install snapshot to be reset
    static final int INVALID_CHUNK_INDEX = -1;

    // This would be passed as the hash code of the last chunk when sending the first chunk
    static final int INITIAL_LAST_CHUNK_HASH_CODE = -1;

    private int snapshotChunkSize;
    private final ByteString snapshotBytes;
    private final String logName;
    private int offset = 0;
    // the next snapshot chunk is sent only if the replyReceivedForOffset matches offset
    private int replyReceivedForOffset;
    // if replyStatus is false, the previous chunk is attempted
    private boolean replyStatus = false;
    private int chunkIndex;
    private final int totalChunks;
    private int lastChunkHashCode = INITIAL_LAST_CHUNK_HASH_CODE;
    private int nextChunkHashCode = INITIAL_LAST_CHUNK_HASH_CODE;

    /**
     * Constructor.
     *
     * @param snapshotBytes
     * @param snapshotChunkSize
     * @param logName
     */
    public LeaderInstallSnapshotState(ByteString snapshotBytes, int snapshotChunkSize, String logName) {
        this.snapshotChunkSize = snapshotChunkSize;
        this.snapshotBytes = snapshotBytes;
        this.logName = logName;
        int size = snapshotBytes.size();
        totalChunks = (size / snapshotChunkSize) +
                (size % snapshotChunkSize > 0 ? 1 : 0);

        LOG.debug("{}: Snapshot {} bytes, total chunks to send: {}", logName, size, totalChunks);

        replyReceivedForOffset = -1;
        chunkIndex = FIRST_CHUNK_INDEX;
    }

    ByteString getSnapshotBytes() {
        return snapshotBytes;
    }

    int incrementOffset() {
        if(replyStatus) {
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
        return replyReceivedForOffset == offset;
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

    byte[] getNextChunk() {
        int snapshotLength = getSnapshotBytes().size();
        int start = incrementOffset();
        int size = snapshotChunkSize;
        if (snapshotChunkSize > snapshotLength) {
            size = snapshotLength;
        } else if ((start + snapshotChunkSize) > snapshotLength) {
            size = snapshotLength - start;
        }

        byte[] nextChunk = new byte[size];
        getSnapshotBytes().copyTo(nextChunk, start, 0, size);
        nextChunkHashCode = Arrays.hashCode(nextChunk);

        LOG.debug("{}: Next chunk: total length={}, offset={}, size={}, hashCode={}", logName,
                snapshotLength, start, size, nextChunkHashCode);
        return nextChunk;
    }

    /**
     * reset should be called when the Follower needs to be sent the snapshot from the beginning
     */
    void reset(){
        offset = 0;
        replyStatus = false;
        replyReceivedForOffset = offset;
        chunkIndex = FIRST_CHUNK_INDEX;
        lastChunkHashCode = INITIAL_LAST_CHUNK_HASH_CODE;
    }

    int getLastChunkHashCode() {
        return lastChunkHashCode;
    }
}
