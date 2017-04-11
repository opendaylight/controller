/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subchannel.generic.api.procedure;

import java.io.Serializable;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.subchannel.generic.api.data.SliceMessage;
import org.opendaylight.controller.subchannel.generic.api.data.SliceMessageTracker;
import org.opendaylight.controller.subchannel.generic.api.messages.AbstractSliceMessageTimeOut;
import org.opendaylight.controller.subchannel.generic.api.messages.ChunkMessage;
import org.opendaylight.controller.subchannel.generic.api.messages.ChunkMessageReply;
import org.opendaylight.controller.subchannel.generic.api.messages.FinishSliceMessage;
import org.opendaylight.controller.subchannel.generic.api.messages.FinishSliceMessageReply;
import org.opendaylight.controller.subchannel.generic.api.messages.SliceMessageTimeOut;
import org.opendaylight.controller.subchannel.generic.api.messages.SliceMessageTrackerTimeOut;
import org.opendaylight.controller.subchannel.generic.api.procedure.client.ClientCatalog;
import org.opendaylight.controller.subchannel.generic.api.procedure.client.ClientCatalogImpl;
import org.opendaylight.controller.subchannel.generic.api.procedure.client.ReceiverClient;
import org.opendaylight.controller.subchannel.generic.api.procedure.client.SenderClient;
import org.opendaylight.controller.subchannel.generic.config.ProcedureConfigParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by HanJie on 2017/1/24.
 *
 * @author Han Jie
 */
public class SliceMessageProcedure<T,C> implements Serializable {
    private final Logger LOG = LoggerFactory.getLogger(getClass());
    private ClientCatalog<SenderClient<T,C>,T> senderCatalog;
    private ClientCatalog<ReceiverClient<T,C>,T> receiverCatalog;
    private ProcedureCallback<T,C> callback;
    private ProcedureConfigParams config;

    public SliceMessageProcedure(ProcedureCallback<T,C> callback, ProcedureConfigParams configParams)
    {
        Preconditions.checkNotNull(configParams);
        this.callback = callback;
        this.senderCatalog = new ClientCatalogImpl<>();
        this.receiverCatalog = new ClientCatalogImpl<>();
        this.config = configParams;
    }

    public void startSliceMessage(T sender, T receiver, byte[] data,
                                                       SliceMessageCallback<Boolean> sliceMessageCallback) {
        LOG.debug("startSliceMessage sender {} receiver {}", sender,receiver);
        ReceiverClient<T,C> receiverClient =  receiverCatalog.getClient(receiver);
        if(receiverClient == null) {
            receiverClient = receiverCatalog.addClient(new ReceiverClient<>(receiver));
        }

        SliceMessage<T, C> sliceMessage = new SliceMessage.SliceMessageBuilder<T, C>()
                        .setMessageId(receiverClient.createSliceMessageId())
                        .setCallback(callback)
                        .setClient(receiverClient)
                        .setReceiver(receiver)
                        .setSender(sender)
                        .setTimeoutInSenconds(config.getMessageTimeoutInSeconds())
                        .setChunkSize(config.getChunkSize())
                        .setData(data)
                        .setSliceMessageCallback(sliceMessageCallback)
                        .build();

        receiverClient.enqueueSliceMessage(sliceMessage);

        ChunkMessage chunkMessage = sliceMessage.getChunkMessage();

        callback.sendCall(receiver, chunkMessage);

        sliceMessage.scheduleOnceTimeOut(chunkMessage);
    }

    public boolean isBusy(T receiver){
        Preconditions.checkNotNull(receiver);
        ReceiverClient<T,C> receiverClient = receiverCatalog.getClient(receiver);

        if(receiverClient !=null && receiverClient.isSending()) {
            return true;
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    public boolean handleMessage(T sender,T receiver,Object message)
    {
        if(message instanceof ChunkMessage) {
            handleChunkMessage(sender,receiver,(ChunkMessage) message);
        }
        else if(message instanceof ChunkMessageReply) {
            handleChunkMessageReply(sender,receiver,(ChunkMessageReply) message);
        }
        else if(message instanceof FinishSliceMessage<?>) {
            handleFinishSliceMessage(sender,receiver,(FinishSliceMessage<T>)message);
        }
        else if(message instanceof FinishSliceMessageReply) {
            handleFinishSliceMessageReply(sender,receiver,(FinishSliceMessageReply) message);
        }
        else if(message instanceof SliceMessageTimeOut<?>) {
            handleSliceMessageTimeOut((AbstractSliceMessageTimeOut<T>)message);
        }
        else if(message instanceof SliceMessageTrackerTimeOut<?>) {
            handleSliceMessageTrackerTimeOut((AbstractSliceMessageTimeOut<T>)message);
        }
        else{
            return false;
        }


        return true;
    }


    private void handleChunkMessage(T sender,T receiver,ChunkMessage message){
        LOG.debug("Received ChunkMessage id {} chunkIndex {} total {} sender {} receiver {}", message.getMessageId(),
                message.getChunkIndex(),message.getTotalChunks(),sender, receiver);
        Object lastMessage;
        SenderClient<T,C> senderClient = senderCatalog.getClient(sender);
        if(senderClient == null) {
            senderClient = senderCatalog.addClient(new SenderClient<>(sender));
        }

        SliceMessageTracker<T,C> sliceMessageTracker = senderClient.getSliceMessageTracker(message.getMessageId());
        if(sliceMessageTracker==null){
            sliceMessageTracker =  new SliceMessageTracker.SliceMessageTrackerBuilder<T,C>()
                        .setMessageId(message.getMessageId())
                        .setReceiver(receiver)
                        .setSender(sender)
                        .setClient(senderClient)
                        .setTimeoutInSenconds(config.getMessageTimeoutInSeconds())
                        .setCallback(callback)
                        .build();
            senderClient.enqueueSliceMessageTracker(sliceMessageTracker);
        }

        sliceMessageTracker.addChunkMessage(message);


        lastMessage = new ChunkMessageReply(message.getMessageId(),message.getChunkIndex(),
                message.getTotalChunks());
        callback.sendCall(sender,lastMessage);
        sliceMessageTracker.scheduleOnceTimeOut(lastMessage);
    }

    private void handleChunkMessageReply(T sender,T receiver, ChunkMessageReply message)
    {
        LOG.debug("Received ChunkMessageReply id {} chunkindex {} total {} sender {} receiver {}", message.getMessageId(),
                message.getChunkIndex(),message.getTotalChunks(),sender, receiver);
        Object lastMessage;
        ReceiverClient<T,C> receiverClient = receiverCatalog.getClient(sender);
        Preconditions.checkNotNull(receiverClient);

        SliceMessage<T,C> sliceMessage = receiverClient.getSliceMessage(message.getMessageId());
        Preconditions.checkNotNull(sliceMessage);

        if(message.getChunkIndex()!= sliceMessage.getCurrentChunkIndex())
        {
            LOG.error("Received ChunkMessageReply index {} != CurrentChunkIndex {}",
                    message.getChunkIndex(),sliceMessage.getCurrentChunkIndex());
            return;
        }

        if(!sliceMessage.incrementChunk()) {
            lastMessage = new FinishSliceMessage<T>(sliceMessage.getMessageId(),sliceMessage.getReceiver(),
                    sliceMessage.getSender());
            callback.sendCall(sender, lastMessage);
        }
        else {
            lastMessage = sliceMessage.getChunkMessage();
            callback.sendCall(sender, lastMessage);
        }
        sliceMessage.scheduleOnceTimeOut(lastMessage);
    }

    private void handleFinishSliceMessage(T sender,T receiver,FinishSliceMessage<T> finishMessage)
    {
        LOG.debug("Received FinishSliceMessage id {} sender {} receiver {}", finishMessage.getMessageId(),
                finishMessage.getReceiver(),finishMessage.getSender(),sender, receiver);
        SenderClient<T,C> senderClient = senderCatalog.removeClient(sender);
        if(senderClient == null) {
            LOG.error("Received FinishSliceMessage {} but senderClient is null...", finishMessage);
            return;
        }
        callback.sendCall(sender,new FinishSliceMessageReply<T>(finishMessage.getMessageId()));
        callback.receiveCall(sender,receiver,senderClient.finish(finishMessage));
    }

    private void handleFinishSliceMessageReply(T sender,T receiver,FinishSliceMessageReply message) {
        LOG.debug("Received handleFinishSliceMessageReply id {} sender {} receiver {} ",
                message.getMessageId(),sender,receiver);

        ReceiverClient<T,C> receiverClient = receiverCatalog.removeClient(sender);

        if(receiverClient ==null){
            LOG.error("Received handleFinishSliceMessageReply {} but receiverClient is null...", message);
            return;
        }

        receiverClient.finish(message.getMessageId());
    }


    private void handleSliceMessageTimeOut(AbstractSliceMessageTimeOut<T> message) {
        LOG.warn("handleSliceMessageTimeOut id {} ", message.getMessageId());
        ReceiverClient<T,C> client = receiverCatalog.removeClient(message.getClientIdentify());
        Preconditions.checkNotNull(client);
        client.handleSliceMessageTimeOut(message);
    }

    private void handleSliceMessageTrackerTimeOut(AbstractSliceMessageTimeOut<T> message) {
        LOG.warn("handleSliceMessageTrackerTimeOut id {} ", message.getMessageId());
        SenderClient<T,C> client = senderCatalog.removeClient(message.getClientIdentify());
        Preconditions.checkNotNull(client);
        SliceMessageTracker<T,C> sliceMessageTracker =
                client.getSliceMessageTracker(message.getMessageId());
        client.handleSliceMessageTimeOut(message);
    }




}


