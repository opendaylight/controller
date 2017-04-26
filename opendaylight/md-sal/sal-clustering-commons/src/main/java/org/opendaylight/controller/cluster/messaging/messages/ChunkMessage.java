/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.messaging.messages;

import com.google.common.base.Optional;
import java.io.Serializable;

/**
 * Created by HanJie on 2017/1/24.
 *
 * @author Han Jie
 */
public class ChunkMessage implements Serializable {
    private static final long serialVersionUID = -3285749137066941997L;
    private long messageId = -1;
    private byte[] chunk;
    private int chunkIndex = -1;
    private int totalChunks = -1;
    Optional<Integer> lastChunkHashCode;

    public ChunkMessage(long messageId, byte[] chunk,int chunkIndex,
                        int totalChunks, Optional<Integer> lastChunkHashCode) {
        this.messageId = messageId;
        this.chunk = chunk.clone();
        this.chunkIndex = chunkIndex;
        this.totalChunks = totalChunks;
        this.lastChunkHashCode = lastChunkHashCode;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public long getMessageId() {
        return messageId;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public byte[] getChunk() {
        return chunk.clone();
    }

    public Optional<Integer> getLastChunkHashCode() {
        return lastChunkHashCode;
    }
}
