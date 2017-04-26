/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.messaging;

import akka.actor.ActorContext;
import com.google.common.base.Optional;
import com.google.protobuf.ByteString;
import java.util.Arrays;
import org.opendaylight.controller.cluster.messaging.client.ReceiverClient;
import org.opendaylight.controller.cluster.messaging.messages.ChunkMessage;
import org.opendaylight.controller.cluster.messaging.messages.SliceMessageTimeOut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by HanJie on 2017/1/23.
 *
 * @author Han Jie
 */
public class SliceMessage<T> extends AbstractSliceMessage<SliceMessageTimeOut<T>> {
    private static final Logger LOG = LoggerFactory.getLogger(SliceMessage.class);
    private ByteString messageBytes;
    private int chunkSize;
    private int curChunkHashCode = INITIAL_LAST_CHUNK_HASH_CODE;
    private int curChunkIndex = FIRST_CHUNK_INDEX;
    private int curChunkRetries = 0;
    private int curOffset = 0;
    private String logName;

    public SliceMessage(ActorContext actorContext,
                        ReceiverClient<T> client, long messageId, byte[] data, int chunkSize,
                        long timeoutInSenconds) {
        super(actorContext,messageId, timeoutInSenconds);
        this.messageBytes = ByteString.copyFrom(data);
        setChunkSize(chunkSize);
        int size = messageBytes.size();
        setTotalChunks((size / chunkSize) + (size % chunkSize > 0 ? 1 : 0));
        this.logName = String.format("Slice message (%d)", messageId);

        LOG.debug("{}: {} bytes, total chunks to send: {}", logName, size, totalChunks);
    }

    private int incrementOffset() {
        curOffset = curOffset + getChunkSize();
        return curOffset;
    }

    private int incrementChunkIndex() {
        curChunkIndex =  curChunkIndex + 1;
        return curChunkIndex;
    }

    boolean incrementChunk() {
        LOG.debug("increment chunk, current message {} chunk index {} total chunks {}",
                getMessageId(),getCurrentChunkIndex(),getTotalChunks());
        if (getCurrentChunkIndex() == getTotalChunks()) {
            return false;
        }

        incrementOffset();
        incrementChunkIndex();
        setLastChunkHashCode(curChunkHashCode);
        curChunkRetries = 0;
        return true;
    }

    int getCurrentChunkIndex() {
        return curChunkIndex;
    }

    public int getChunkRetries() {
        return curChunkRetries;
    }



    public int incrementCurChunkRetries() {
        return curChunkRetries ++;
    }

    private int getChunkSize() {
        return chunkSize;
    }

    private void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    private ByteString getMessageBytes() {
        return messageBytes;
    }

    private int getOffset() {
        return curOffset;
    }

    protected byte[] getChunk() {
        int messageLength = getMessageBytes().size();
        int start = getOffset();
        int size = getChunkSize();
        if (getChunkSize() > messageLength) {
            size = messageLength;
        } else if ((start + getChunkSize()) > messageLength) {
            size = messageLength - start;
        }

        byte[] chunk = new byte[size];
        getMessageBytes().copyTo(chunk, start, 0, size);
        curChunkHashCode = Arrays.hashCode(chunk);

        LOG.debug("{}: chunk index {} total chunk {} length={}, current offset={}, size={}, hashCode={}", logName,
                curChunkIndex,totalChunks,messageLength, start, size, curChunkHashCode);
        return chunk;
    }

    ChunkMessage getChunkMessage() {
        byte[] chunk = getChunk();
        return new ChunkMessage(getMessageId(), chunk,
                getCurrentChunkIndex(), getTotalChunks(), Optional.of(getLastChunkHashCode()));
    }

    void reset() {
        LOG.debug("Slicemessage reset, current message {} chunk index {}", getMessageId(),getCurrentChunkIndex());
        curOffset = 0;
        curChunkIndex = FIRST_CHUNK_INDEX;
        curChunkHashCode = INITIAL_LAST_CHUNK_HASH_CODE;
        curChunkRetries = 0;
        setLastChunkHashCode(INITIAL_LAST_CHUNK_HASH_CODE);
    }
}
