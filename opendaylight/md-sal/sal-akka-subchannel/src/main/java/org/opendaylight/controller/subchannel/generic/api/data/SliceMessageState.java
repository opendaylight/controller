/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subchannel.generic.api.data;

import java.util.Arrays;

import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by HanJie on 2017/1/24.
 *
 * @author Han Jie
 */
public class SliceMessageState {
    private static final Logger LOG = LoggerFactory.getLogger(SliceMessage.class);
    private long messageId;
    private ByteString messageBytes;
    static final int FIRST_CHUNK_INDEX = 1;
    private int chunkIndex=FIRST_CHUNK_INDEX;
    private int totalChunks;
    private int offset = 0;
    private int chunkSize = 0;
    static final int INITIAL_LAST_CHUNK_HASH_CODE = -1;
    private int lastChunkHashCode = INITIAL_LAST_CHUNK_HASH_CODE;
    private int nextChunkHashCode = INITIAL_LAST_CHUNK_HASH_CODE;
    private boolean replyStatus = false;
    private String logName;

    public SliceMessageState(long messageId,ByteString messageBytes,int chunkSize,int totalChunks)
    {
        this.messageId = messageId;
        this.messageBytes = messageBytes;
        this.chunkSize = chunkSize;
        this.totalChunks = totalChunks;
        logName = String.format("Slice message (%d)", messageId);
    }

    protected int getChunkIndex(){
        return chunkIndex;
    }


    protected int getChunkSize(){
        return chunkSize;
    }

    private ByteString getMessageBytes() {
        return messageBytes;
    }

    protected int incrementOffset() {
        offset = offset + chunkSize;
        return offset;
    }

    protected int incrementChunkIndex() {

        chunkIndex =  chunkIndex + 1;
        return chunkIndex;
    }

    protected  void incrementChunk()
    {
        incrementOffset();
        incrementChunkIndex();
    }

    protected byte[] getChunk() {
        int messageLength = getMessageBytes().size();
        int start = offset;
        int size = getChunkSize();
        if (getChunkSize() > messageLength) {
            size = messageLength;
        } else if ((start + getChunkSize()) > messageLength) {
            size = messageLength - start;
        }

        byte[] nextChunk = new byte[size];
        getMessageBytes().copyTo(nextChunk, start, 0, size);
        nextChunkHashCode = Arrays.hashCode(nextChunk);

        LOG.debug("{}: chunkIndex {} totalChunk {} length={}, offset={}, size={}, hashCode={}", logName,
                chunkIndex,totalChunks,messageLength, start, size, nextChunkHashCode);
        return nextChunk;
    }
}
