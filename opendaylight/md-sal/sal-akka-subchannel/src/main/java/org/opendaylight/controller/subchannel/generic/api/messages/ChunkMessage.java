/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subchannel.generic.api.messages;

import java.io.Serializable;

/**
 *
 * @author Han Jie
 */
public class ChunkMessage implements Serializable {
    private long messageId=-1;
    private byte[] chunk;
    private int chunkSize=-1;
    private int chunkIndex=-1;
    private int totalChunks=-1;

    public ChunkMessage(long messageId,byte[] chunk,int chunkSize,
                 int chunkIndex,int totalChunks){
        this.messageId = messageId;
        this.chunk =chunk;
        this.chunkSize = chunkSize;
        this.chunkIndex = chunkIndex;
        this.totalChunks = totalChunks;
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
    public int getChunkSize() {
        return chunkSize;
    }
    public byte[] getData() {
        return chunk;
    }

}
