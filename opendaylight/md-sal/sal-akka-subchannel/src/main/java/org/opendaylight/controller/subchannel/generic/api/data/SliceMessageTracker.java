/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subchannel.generic.api.data;

import java.util.Arrays;
import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;
import org.opendaylight.controller.subchannel.generic.api.messages.AbstractSliceMessageTimeOut;
import org.opendaylight.controller.subchannel.generic.api.messages.ChunkMessage;
import org.opendaylight.controller.subchannel.generic.api.messages.SliceMessageTrackerTimeOut;
import org.opendaylight.controller.subchannel.generic.api.procedure.ProcedureCallback;
import org.opendaylight.controller.subchannel.generic.api.procedure.client.AbstractClient;
import org.opendaylight.controller.subchannel.generic.api.procedure.client.ClientIdentify;
import org.opendaylight.controller.subchannel.generic.spi.data.AbstractSliceMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by HanJie on 2017/1/24.
 *
 * @author Han Jie
 */
public class SliceMessageTracker<T,C> extends AbstractSliceMessage<T,C> {
    private final Logger LOG = LoggerFactory.getLogger(getClass());
    private ByteString collectedChunks = ByteString.EMPTY;
    private int lastChunkHashCode = SliceMessageState.INITIAL_LAST_CHUNK_HASH_CODE;

    public SliceMessageTracker(long messageId, @Nullable T receiver, @Nullable T replyTo,AbstractClient<T> client,
                               long timeoutInSenconds,ProcedureCallback<T,C> callback){
        super(messageId, receiver, replyTo,client,timeoutInSenconds,callback);
    }

    public void addChunkMessage(ChunkMessage chunkMessage){
        LOG.debug("addChunkMessage messageId {} chunkIndex {} chunkSize {} totalChunks {}",
                chunkMessage.getMessageId(),chunkMessage.getChunkIndex(),
                chunkMessage.getChunkSize(),chunkMessage.getTotalChunks());
        collectedChunks = collectedChunks.concat(ByteString.copyFrom(chunkMessage.getData()));
        this.lastChunkHashCode = Arrays.hashCode(chunkMessage.getData());
    }

    public byte[] finish(){
        return getData();
    }

    @Override
    protected AbstractSliceMessageTimeOut<T> newSliceMessageTimeOut(ClientIdentify<T> clientId, long messageId){
        return new SliceMessageTrackerTimeOut<>(clientId,messageId);
    }

    private byte[] getData(){
        return collectedChunks.toByteArray();
    }

    static public class SliceMessageTrackerBuilder<T, C> extends AbstractSliceMessageBuilder<T,C,SliceMessageTrackerBuilder<T, C>> {

        @Override
        protected void verify() {
            Preconditions.checkNotNull(getMessageId());
            Preconditions.checkNotNull(getCallback());
            Preconditions.checkNotNull(getClient());
            Preconditions.checkNotNull(getTimeoutInSenconds());
        }

        public SliceMessageTracker<T, C> build() {
            verify();
            return new SliceMessageTracker<T, C>(getMessageId(), getReceiver(), getSender(),
                    getClient(),getTimeoutInSenconds(),
                    getCallback());
        }
    }

}
