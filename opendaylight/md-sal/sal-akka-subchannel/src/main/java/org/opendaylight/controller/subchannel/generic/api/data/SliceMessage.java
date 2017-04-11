/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subchannel.generic.api.data;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;
import org.opendaylight.controller.subchannel.generic.api.messages.AbstractSliceMessageTimeOut;
import org.opendaylight.controller.subchannel.generic.api.messages.ChunkMessage;
import org.opendaylight.controller.subchannel.generic.api.messages.SliceMessageTimeOut;
import org.opendaylight.controller.subchannel.generic.api.procedure.ProcedureCallback;
import org.opendaylight.controller.subchannel.generic.api.procedure.SliceMessageCallback;
import org.opendaylight.controller.subchannel.generic.api.procedure.client.AbstractClient;
import org.opendaylight.controller.subchannel.generic.api.procedure.client.ClientIdentify;
import org.opendaylight.controller.subchannel.generic.spi.data.AbstractSliceMessage;

/**
 * Created by HanJie on 2017/1/23.
 *
 * @author Han Jie
 */
public class SliceMessage<T,C> extends AbstractSliceMessage<T,C> {
    private byte[] data;
    private int chunkSize;
    private int totalChunks;
    private SliceMessageState sliceMessageState;
    private SliceMessageCallback<Boolean> sliceMessageCallback;

    private SliceMessage(long messageId, byte[] data, int chunkSize, T receiver, T sender, AbstractClient<T> client,
                         long timeoutInSenconds, ProcedureCallback<T, C> callback, SliceMessageCallback<Boolean> sliceMessageCallback) {
        super(messageId, receiver, sender,client,timeoutInSenconds,callback);
        this.data = data;
        this.sliceMessageCallback = sliceMessageCallback;
        ByteString messageBytes = ByteString.copyFrom(getData());
        this.chunkSize = chunkSize;
        totalChunks = (messageBytes.size() / chunkSize) + (messageBytes.size() % chunkSize > 0 ? 1 : 0);
        sliceMessageState = new SliceMessageState(messageId, messageBytes, chunkSize, totalChunks);

    }

    private byte[] getData() {
        return this.data;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void finish(Throwable failure) {
        Preconditions.checkNotNull(sliceMessageCallback);
        sliceMessageCallback.onFailure(failure);
    }

    public void finish() {
        Preconditions.checkNotNull(sliceMessageCallback);
        sliceMessageCallback.onSuccess(true);
    }

    @Override
    protected AbstractSliceMessageTimeOut<T> newSliceMessageTimeOut(ClientIdentify<T> clientId, long messageId){
        return new SliceMessageTimeOut<T>(clientId,messageId);
    }

    public int getCurrentChunkIndex() {
        return sliceMessageState.getChunkIndex();
    }

    public boolean incrementChunk() {
        if (sliceMessageState.getChunkIndex() == getTotalChunks()) {
            return false;
        }

        sliceMessageState.incrementChunk();
        return true;
    }

    public ChunkMessage getChunkMessage() {
        byte[] chunk = sliceMessageState.getChunk();
        return new ChunkMessage(getMessageId(), chunk, getChunkSize(), sliceMessageState.getChunkIndex(), getTotalChunks());
    }

    static public class SliceMessageBuilder<T, C> extends AbstractSliceMessageBuilder<T,C,SliceMessageBuilder<T,C>> {
        private byte[] data;
        private int chunkSize;
        private SliceMessageCallback<Boolean> sliceMessageCallback;

        public byte[] getData() {
            return data;
        }

        public SliceMessageBuilder<T, C> setData(byte[] data) {
            this.data = data;

            return this;
        }

        public SliceMessageCallback<Boolean> getSliceMessageCallback() {
            return sliceMessageCallback;
        }

        public int getChunkSize() {
            return chunkSize;
        }

        public SliceMessageBuilder<T, C> setChunkSize(int chunkSize) {
            this.chunkSize = chunkSize;
            return this;
        }

        public SliceMessageBuilder<T, C> setSliceMessageCallback(SliceMessageCallback<Boolean> sliceMessageCallback) {
            this.sliceMessageCallback = sliceMessageCallback;
            return this;
        }

        @Override
        protected void verify() {
            super.verify();
            Preconditions.checkNotNull(data);
            Preconditions.checkNotNull(chunkSize);
            Preconditions.checkNotNull(sliceMessageCallback);
        }

        public SliceMessage<T, C> build() {
            verify();
            return new SliceMessage<>(getMessageId(), getData(),
                    getChunkSize(), getReceiver(), getSender(), getClient(),getTimeoutInSenconds(),
                    getCallback(), getSliceMessageCallback());
        }
    }

}
