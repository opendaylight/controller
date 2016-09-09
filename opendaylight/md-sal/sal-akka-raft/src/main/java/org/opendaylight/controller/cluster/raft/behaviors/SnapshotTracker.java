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
import com.google.protobuf.ByteString;
import java.util.Arrays;
import org.slf4j.Logger;

/**
 * SnapshotTracker does house keeping for a snapshot that is being installed in chunks on the Follower
 */
public class SnapshotTracker {
    private final Logger LOG;
    private final int totalChunks;
    private final String leaderId;
    private ByteString collectedChunks = ByteString.EMPTY;
    private int lastChunkIndex = LeaderInstallSnapshotState.FIRST_CHUNK_INDEX - 1;
    private boolean sealed = false;
    private int lastChunkHashCode = LeaderInstallSnapshotState.INITIAL_LAST_CHUNK_HASH_CODE;

    SnapshotTracker(Logger LOG, int totalChunks, String leaderId) {
        this.LOG = LOG;
        this.totalChunks = totalChunks;
        this.leaderId = Preconditions.checkNotNull(leaderId);
    }

    /**
     * Adds a chunk to the tracker
     *
     * @param chunkIndex
     * @param chunk
     * @return true when the lastChunk is received
     * @throws InvalidChunkException
     */
    boolean addChunk(int chunkIndex, byte[] chunk, Optional<Integer> lastChunkHashCode) throws InvalidChunkException{
        LOG.debug("addChunk: chunkIndex={}, lastChunkIndex={}, collectedChunks.size={}, lastChunkHashCode={}",
                chunkIndex, lastChunkIndex, collectedChunks.size(), this.lastChunkHashCode);

        if(sealed){
            throw new InvalidChunkException("Invalid chunk received with chunkIndex " + chunkIndex + " all chunks already received");
        }

        if(lastChunkIndex + 1 != chunkIndex){
            throw new InvalidChunkException("Expected chunkIndex " + (lastChunkIndex + 1) + " got " + chunkIndex);
        }

        if(lastChunkHashCode.isPresent()){
            if(lastChunkHashCode.get() != this.lastChunkHashCode){
                throw new InvalidChunkException("The hash code of the recorded last chunk does not match " +
                        "the senders hash code, expected " + this.lastChunkHashCode + " was " + lastChunkHashCode.get());
            }
        }

        sealed = chunkIndex == totalChunks;
        lastChunkIndex = chunkIndex;
        collectedChunks = collectedChunks.concat(ByteString.copyFrom(chunk));
        this.lastChunkHashCode = Arrays.hashCode(chunk);
        return sealed;
    }

    byte[] getSnapshot(){
        if(!sealed) {
            throw new IllegalStateException("lastChunk not received yet");
        }

        return collectedChunks.toByteArray();
    }

    ByteString getCollectedChunks(){
        return collectedChunks;
    }

    String getLeaderId() {
        return leaderId;
    }

    public static class InvalidChunkException extends Exception {
        private static final long serialVersionUID = 1L;

        InvalidChunkException(String message){
            super(message);
        }
    }

}
