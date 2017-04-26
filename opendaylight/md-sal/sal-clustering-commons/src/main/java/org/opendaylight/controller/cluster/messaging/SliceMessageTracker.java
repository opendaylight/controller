/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.messaging;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import com.google.common.base.Optional;
import com.google.protobuf.ByteString;
import java.util.Arrays;
import org.opendaylight.controller.cluster.messaging.client.SenderClient;
import org.opendaylight.controller.cluster.messaging.messages.ChunkMessage;
import org.opendaylight.controller.cluster.messaging.messages.SliceMessageTrackerTimeOut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by HanJie on 2017/1/24.
 *
 * @author Han Jie
 */
public class SliceMessageTracker
        extends AbstractSliceMessage<SliceMessageTrackerTimeOut<ActorRef>> {
    private static final Logger LOG = LoggerFactory.getLogger(SliceMessageTracker.class);
    private ByteString collectedChunks = ByteString.EMPTY;
    private int lastChunkIndex = FIRST_CHUNK_INDEX - 1;

    public SliceMessageTracker(ActorContext actorContext,
                               SenderClient client, long messageId,
                               long timeoutInSenconds, int totalChunks) {
        super(actorContext, messageId, timeoutInSenconds);
        setTotalChunks(totalChunks);
    }

    boolean addChunkMessage(ChunkMessage chunkMessage) throws InvalidChunkException {
        LOG.debug("addChunkMessage message id {} chunk index {} total chunks {}",
                chunkMessage.getMessageId(),chunkMessage.getChunkIndex(),chunkMessage.getTotalChunks());

        if (getLastChunkIndex() + 1 != chunkMessage.getChunkIndex()) {
            LOG.error("addChunkMessage chunk index {} , lastChunkIndex {}",
                    chunkMessage.getChunkIndex(),getLastChunkIndex());
            throw new InvalidChunkException("Expected ChunkIndex " + (getLastChunkIndex() + 1)
                    + " got " + chunkMessage.getChunkIndex());
        }

        Optional<Integer> lastChunkHashCode = chunkMessage.getLastChunkHashCode();

        if (lastChunkHashCode.isPresent()) {
            if (lastChunkHashCode.get() != getLastChunkHashCode()) {
                LOG.error("addChunkMessage lastChunkHashCode {} , local lastChunkHashCode {}",
                        chunkMessage.getLastChunkHashCode(),getLastChunkHashCode());
                throw new InvalidChunkException("The hash code of the recorded last chunk does not match "
                        + "the senders hash code, expected " + getLastChunkHashCode()
                        + " was " + lastChunkHashCode.get());
            }
        }

        lastChunkIndex = chunkMessage.getChunkIndex();
        collectedChunks = collectedChunks.concat(ByteString.copyFrom(chunkMessage.getChunk()));
        setLastChunkHashCode(Arrays.hashCode(chunkMessage.getChunk()));

        return getTotalChunks() == chunkMessage.getChunkIndex();
    }

    public int getLastChunkIndex() {
        return lastChunkIndex;
    }

    public byte[] getData() {
        return collectedChunks.toByteArray();
    }

    public static class InvalidChunkException extends Exception {
        private static final long serialVersionUID = 1L;

        InvalidChunkException(String message) {
            super(message);
        }
    }
}
