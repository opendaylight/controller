/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.messaging;

import static org.opendaylight.controller.cluster.messaging.AbstractSliceMessage.INVALID_CHUNK_INDEX;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.serialization.Serialization;
import akka.serialization.SerializationExtension;
import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import org.opendaylight.controller.cluster.messaging.client.ReceiverClient;
import org.opendaylight.controller.cluster.messaging.client.SenderClient;
import org.opendaylight.controller.cluster.messaging.messages.ChunkMessage;
import org.opendaylight.controller.cluster.messaging.messages.ChunkMessageReply;
import org.opendaylight.controller.cluster.messaging.messages.SerializedMessage;
import org.opendaylight.controller.cluster.messaging.messages.SliceMessageTimeOut;
import org.opendaylight.controller.cluster.messaging.messages.SliceMessageTrackerTimeOut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.duration.FiniteDuration;

/**
 * Created by HanJie on 2017/1/24.
 *
 * @author Han Jie
 */
public class SubChannel {
    private static final Logger LOG = LoggerFactory.getLogger(SubChannel.class);
    private Map<ActorRef,SenderClient> senderCatalog = new HashMap<>();
    private Map<ActorRef,ReceiverClient<ActorRef>> receiverCatalog = new HashMap<>();
    private static final AtomicLongFieldUpdater<SubChannel> MESSAGE_COUNTER_UPDATER =
            AtomicLongFieldUpdater.newUpdater(SubChannel.class, "messageId");
    private ActorContext actorContext;
    private SliceMessageCallback<Object> callback;
    // Used via MESSAGE_COUNTER_UPDATER
    @SuppressWarnings("unused")
    private volatile long messageId = 1;
    public static final int DEFAULT_CHUNK_SIZE = 2 * 1024 * 1024; //2MB
    public static final long DEFAULT_MESSAGE_TIMEOUT = 5; //seconds
    public static final long DEFAULT_MESSAGE_MAX_RETRIES = 5; //seconds
    private static final FiniteDuration DURATION = new FiniteDuration(DEFAULT_MESSAGE_TIMEOUT, TimeUnit.SECONDS);

    public SubChannel(ActorContext actorContext) {
        this.actorContext = actorContext;
    }

    public SubChannel(ActorContext actorContext,SliceMessageCallback<Object> callback) {
        this(actorContext);
        this.callback = callback;
    }

    private long getAndIncrementSliceMessageId() {
        return  MESSAGE_COUNTER_UPDATER.getAndIncrement(this);
    }

    private ActorRef getSelf() {
        return actorContext.self();
    }

    public void request(ActorSelection receiver, Object message) {
        LOG.debug("request  receiver {} message {} ", receiver,message);
        receiver.tell(message,getSelf());
    }

    public void post(ActorSelection receiver, Object message) throws Exception {
        LOG.debug("post receiver {} message {} ", receiver,message);
        try {
            ActorRef ref = Await.result(receiver.resolveOne(DURATION), DURATION);
            post(ref,message);
        } catch (TimeoutException e) {
            LOG.error("post receiver exception {} ", e.getMessage());
        }
    }

    public void post(ActorRef receiver, Object message) {
        LOG.debug("post receiver {} message {} ", receiver,message);
        SerializedMessage serializedMessage = new SerializedMessage(message.getClass(),serialize(message));
        post(receiver,serialize(serializedMessage));
    }

    private void post(ActorRef receiver, byte[] data) {
        LOG.debug("post receiver {}", receiver);
        ReceiverClient<ActorRef> receiverClient = receiverCatalog.get(receiver);

        if (receiverClient == null) {
            receiverClient = new ReceiverClient<>(receiver);
            receiverCatalog.put(receiver,receiverClient);
        }

        SliceMessage<ActorRef> sliceMessage = new SliceMessage<>(actorContext,receiverClient,
                getAndIncrementSliceMessageId(), data, DEFAULT_CHUNK_SIZE,
                DEFAULT_MESSAGE_TIMEOUT);

        receiverClient.enqueueMessage(sliceMessage);
        doPost(receiverClient);
    }

    private void doPost(ReceiverClient<ActorRef> receiverClient) {
        SliceMessage<ActorRef> sliceMessage;
        if (receiverClient.getCurrentMessage() == null) {
            if (!receiverClient.isQueueEmpty()) {
                LOG.debug("doPost {} pollMessage", receiverClient);
                sliceMessage = receiverClient.pollMessage();
                directPost(receiverClient.getClientId(),sliceMessage);
            } else {
                removeReceiverClient(receiverClient);
            }
        }
    }

    private void directPost(ActorRef receiver, SliceMessage<ActorRef> sliceMessage) {
        Preconditions.checkNotNull(receiver,"receiver should not be null!");
        Preconditions.checkNotNull(sliceMessage,"current sliceMessage should not be null!");
        ChunkMessage chunkMessage = sliceMessage.getChunkMessage();
        receiver.tell(chunkMessage, getSelf());
        sliceMessage.incrementCurChunkRetries();
        LOG.debug("directPost ChunkMessage message {} chunk index {} try {} to receiver {}",
                sliceMessage.getMessageId(),sliceMessage.getCurrentChunkIndex(),
                sliceMessage.getChunkRetries(),receiver);
        sliceMessage.scheduleOnceTimeOut(
                new SliceMessageTimeOut<>(receiver,sliceMessage.getMessageId(),sliceMessage.getCurrentChunkIndex()));
    }

    @SuppressWarnings("unchecked")
    public boolean handleMessage(ActorRef sender,Object message) {
        if (message instanceof ChunkMessage) {
            handleChunkMessage(sender,(ChunkMessage) message);
        } else if (message instanceof ChunkMessageReply) {
            handleChunkMessageReply(sender,(ChunkMessageReply) message);
        } else if (message instanceof SliceMessageTimeOut) {
            handleSliceMessageTimeOut((SliceMessageTimeOut<ActorRef>)message);
        } else if (message instanceof SliceMessageTrackerTimeOut) {
            handleSliceMessageTrackerTimeOut((SliceMessageTrackerTimeOut<ActorRef>)message);
        } else {
            return false;
        }

        return true;
    }


    private void handleChunkMessage(ActorRef sender, ChunkMessage message) {
        LOG.debug("Received ChunkMessage id {} chunk index {} total {} sender {} self {}", message.getMessageId(),
                message.getChunkIndex(),message.getTotalChunks(),sender, getSelf());

        SenderClient senderClient = senderCatalog.get(sender);
        if (senderClient == null) {
            senderClient = new SenderClient(sender);
            senderCatalog.put(sender,senderClient);
        }

        SliceMessageTracker sliceMessageTracker = senderClient.getCurrentMessage();

        if (sliceMessageTracker == null) {
            sliceMessageTracker =  new SliceMessageTracker(actorContext,
                                        senderClient, message.getMessageId(),
                                        DEFAULT_MESSAGE_TIMEOUT,message.getTotalChunks());
            senderClient.setCurrentMessage(sliceMessageTracker);
        }

        Preconditions.checkState(message.getMessageId() == sliceMessageTracker.getMessageId());

        final ChunkMessageReply reply = new ChunkMessageReply(message.getMessageId(), message.getChunkIndex(),true);

        try {
            if (sliceMessageTracker.addChunkMessage(message)) {
                removeSenderClient(senderClient);
                /**
                 *  Here reply first in case that the actor stopped by onComplete of request at SubChannelProxy and
                 *  also try best not to be suspended by derialization.
                 */
                sender.tell(reply, getSelf());
                LOG.debug("Send last ChunkMessageReply message id {} chunk index {} success {} to {}",
                        reply.getMessageId(),reply.getChunkIndex(),reply.isSuccess(),sender);

                //TODO: If do serialize/deserialize operation should best be specified by user
                SerializedMessage serializedMessage =
                        (SerializedMessage) deserialize(sliceMessageTracker.getData(), SerializedMessage.class);
                Object message1 = deserialize(serializedMessage.getData(), serializedMessage.getClazz());

                if (callback == null) {
                    getSelf().tell(message1, sender);
                } else {
                    callback.onReceive(message1);
                }
            } else {
                sliceMessageTracker.scheduleOnceTimeOut(
                        new SliceMessageTrackerTimeOut<>(sender,reply.getMessageId(),reply.getChunkIndex()));
                LOG.debug("Send ChunkMessageReply message {} chunk index {} success {} to {}",
                        reply.getMessageId(),reply.getChunkIndex(),reply.isSuccess(),sender);
                sender.tell(reply, getSelf());
            }

        } catch (SliceMessageTracker.InvalidChunkException ignored) {
            LOG.debug("InvalidChunkException ChunkMessage id {} chunk index {}, "
                            + "send ChunkMessageReply id {} chunk index {} success {} to {}",
                    message.getMessageId(),message.getChunkIndex(),message.getMessageId(),-1,false,sender);

            sender.tell(new ChunkMessageReply(message.getMessageId(),-1, false), getSelf());
            removeSenderClient(senderClient);
        } catch (IllegalStateException e) {
            LOG.debug("Exception in handleChunkMessage,"
                            + "send ChunkMessageReply id {} chunk index {} success {} to {}",
                    message.getMessageId(),message.getChunkIndex(),false,sender);

            //send reply with success as false. The chunk will be sent again on failure
            sender.tell(new ChunkMessageReply(message.getMessageId(),message.getChunkIndex(), false), getSelf());
        }
    }

    private void handleChunkMessageReply(ActorRef sender,ChunkMessageReply message) {
        LOG.debug("Received ChunkMessageReply id {} chunk index {} success {} sender {} self {}",
                message.getMessageId(),message.getChunkIndex(), message.isSuccess(),sender, getSelf());

        ReceiverClient<ActorRef> receiverClient = receiverCatalog.get(sender);
        if (receiverClient == null) {
            /**
             * The last ChunkMessage will trigger remote's deserialization that may take longger time than timeout,
             * so that it may send the last chunkmessage more then once which would make a reply of
             * InvalidChunkException at the end, do nothing but just return here.
             */
            LOG.warn("Receiver client not exist!");
            return;
        }

        SliceMessage<ActorRef> sliceMessage = receiverClient.getCurrentMessage();
        Preconditions.checkState(message.getMessageId() == sliceMessage.getMessageId());
        Preconditions.checkNotNull(sliceMessage);

        if (message.getChunkIndex() != sliceMessage.getCurrentChunkIndex()) {
            LOG.error("Received ChunkMessageReply chunk index {} does not match expected chunkIndex {}, "
                            + "wait for timeout to retry.",
                    message.getChunkIndex(),sliceMessage.getCurrentChunkIndex());

            if (message.getChunkIndex() == INVALID_CHUNK_INDEX) {
                //  resume from the beginning
                sliceMessage.reset();
                directPost(sender,sliceMessage);
            }
        } else {
            if (message.isSuccess()) {
                if (!sliceMessage.incrementChunk()) {
                    receiverClient.done();
                    doPost(receiverClient);
                } else {
                    directPost(sender,sliceMessage);
                }
            } else {
                LOG.info("ChunkMessageReply received sending chunk index {} failed , "
                                + "wait for timeout to retry.",
                        message.getChunkIndex());
            }
        }
    }

    private void handleSliceMessageTimeOut(SliceMessageTimeOut<ActorRef> message) {
        LOG.warn("Slicemessage timeout message {} chunk index {} client id {}.",
                message.getMessageId(),message.getChunkIndex(),message.getClientId());

        ReceiverClient<ActorRef> client = receiverCatalog.get(message.getClientId());
        if (client == null) {
            LOG.debug("Outdated timeout message, receiver client not exist {}",message.getClientId());
            return;
        }
        Preconditions.checkNotNull(client.getCurrentMessage());
        if (message.getMessageId() != client.getCurrentMessage().getMessageId()
                || message.getChunkIndex() != client.getCurrentMessage().getCurrentChunkIndex()) {
            LOG.debug("Outdated timeout message {} chunk index {}, current message {} chunk index {}",
                    message.getMessageId(),message.getChunkIndex(),
                    client.getCurrentMessage().getMessageId(),client.getCurrentMessage().getCurrentChunkIndex());
            return;
        }

        Preconditions.checkState(client.getCurrentMessage().getChunkRetries() <= DEFAULT_MESSAGE_MAX_RETRIES + 1);
        if (client.getCurrentMessage().getChunkRetries() == DEFAULT_MESSAGE_MAX_RETRIES + 1) {
            LOG.debug("Max retry times {},message {} chunk index {}",
                    client.getCurrentMessage().getChunkRetries() - 1,client.getCurrentMessage().getMessageId(),
                    client.getCurrentMessage().getCurrentChunkIndex());
            removeReceiverClient(client);
        } else {
            directPost(client.getClientId(), client.getCurrentMessage());
        }
    }

    private void handleSliceMessageTrackerTimeOut(SliceMessageTrackerTimeOut<ActorRef> message) {
        LOG.warn("Slicemessage tracker timeout message {} chunk index {} client id {}.",
                message.getMessageId(),message.getChunkIndex(),message.getClientId());
        SenderClient client = senderCatalog.get(message.getClientId());
        if (client == null) {
            LOG.debug("Outdated timeout message, sender client not exist {}",message.getClientId());
            return;
        }
        Preconditions.checkNotNull(client.getCurrentMessage());
        if (message.getMessageId() != client.getCurrentMessage().getMessageId()
                || message.getChunkIndex() != client.getCurrentMessage().getLastChunkIndex()) {
            LOG.debug("Outdated timeout message {} chunk index {}, current message {} chunk index {}",
                    message.getMessageId(),message.getChunkIndex(),
                    client.getCurrentMessage().getMessageId(),client.getCurrentMessage().getLastChunkIndex());
            return;
        }

        removeSenderClient(client);
    }

    private SenderClient removeSenderClient(SenderClient senderClient) {
        LOG.debug("Remove sender client {}",senderClient);
        senderClient.done();
        return senderCatalog.remove(senderClient.getClientId());
    }

    private ReceiverClient<ActorRef> removeReceiverClient(ReceiverClient<ActorRef> receiverClient) {
        LOG.debug("Remove receiver client {}",receiverClient);
        receiverClient.done();
        return receiverCatalog.remove(receiverClient.getClientId());
    }

    public byte[] serialize(Object message) {
        LOG.debug("Serialize {}",message.getClass());
        Serialization serialization = SerializationExtension.get(actorContext.system());
        akka.serialization.Serializer serializer = serialization.findSerializerFor(message);
        return serializer.toBinary(message);
    }


    private Object deserialize(byte[] bytes,Class<?> clazz) {
        LOG.debug("Deserialize {}",clazz);
        Serialization serialization = SerializationExtension.get(actorContext.system());
        akka.serialization.Serializer serializer = serialization.serializerFor(clazz);
        return serializer.fromBinary(bytes);
    }
}


