/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subchannel.impl.akkabased.serializer;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import akka.serialization.Serialization;
import akka.serialization.SerializationExtension;
import akka.serialization.Serializer;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.subchannel.generic.api.messages.AbstractDeliverMessage;
import org.opendaylight.controller.subchannel.generic.api.messages.ClazzBytes;
import org.opendaylight.controller.subchannel.generic.api.messages.DeserializeMessage;
import org.opendaylight.controller.subchannel.generic.api.messages.DeserializeMessageReply;
import org.opendaylight.controller.subchannel.generic.api.messages.PostDeliverMessage;
import org.opendaylight.controller.subchannel.generic.api.messages.RequestDeliverMessage;
import org.opendaylight.controller.subchannel.generic.api.messages.SerializeMessage;
import org.opendaylight.controller.subchannel.generic.api.messages.SerializeMessageReply;
import org.opendaylight.controller.subchannel.generic.api.messages.SerializerReceiveTimeout;
import org.opendaylight.controller.subchannel.generic.spi.proxy.ProxyLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

/**
 * Created by HanJie on 2017/2/17.
 *
 * @author Han Jie
 */
public class SubChannelSerializeActor extends UntypedActor {
    protected final Logger LOG = LoggerFactory.getLogger(getClass());
    private ProxyLink<ActorRef> proxyLink;

    private SubChannelSerializeActor(ProxyLink<ActorRef> proxyLink, Duration receiveTimeout){
        this.proxyLink = proxyLink;
    }

    private SubChannelSerializeActor(){
        this.proxyLink = null;
    }

    private ProxyLink<ActorRef> getProxyLink() {
        return proxyLink;
    }

    @Override
    @SuppressWarnings("unchecked")
    public final void onReceive(Object message) throws Exception {
        if (message instanceof SerializeMessage<?>) {
            handleSerializeMessage(getSender(), (SerializeMessage<ActorRef>) message);
        }else if (message instanceof DeserializeMessage<?>) {
            handleDeserializeMessage(getSender(), (DeserializeMessage<ActorRef>) message);
        }else if (message instanceof ReceiveTimeout) {
            LOG.debug("Got ReceiveTimeout for inactivity - closing serializer, localProxy {} remoteProxy {}"
                    , getProxyLink().getLocalProxy(), getProxyLink().getRemoteProxy());
            close();
        }
    }

    private void close() {
        getContext().parent().tell(new SerializerReceiveTimeout<>(getProxyLink()), getSelf());
    }

    @SuppressWarnings("unchecked")
    private void handleSerializeMessage(ActorRef sender,
                                        SerializeMessage<ActorRef> message) {
        LOG.debug("handleSerializeMessage {} ", message);
        AbstractDeliverMessage<ActorRef,ClazzBytes> abstractSerializedDeliverMessage =
                newSerializedDeliverMessage((AbstractDeliverMessage<ActorRef,Object>)message.getMessage());

        sender.tell(new SerializeMessageReply<ActorRef>(message.getMessageContext(),
                serialize(abstractSerializedDeliverMessage)),getSelf());
    }

    private AbstractDeliverMessage<ActorRef,ClazzBytes> newSerializedDeliverMessage(
            AbstractDeliverMessage<ActorRef,Object> deliverMessage){
        AbstractDeliverMessage<ActorRef,ClazzBytes> serializedDeliverMessage = null;
        ClazzBytes clazzBytes = new ClazzBytes(deliverMessage.getMessage().getClass(),
                serialize(deliverMessage.getMessage()));
        if(deliverMessage instanceof RequestDeliverMessage<?,?>){
            serializedDeliverMessage =
                    new RequestDeliverMessage<ActorRef,ClazzBytes>(
                    deliverMessage.getReceiver(),clazzBytes,
                    ((RequestDeliverMessage<ActorRef,Object>) deliverMessage).getTimeout());
        }
        else if(deliverMessage instanceof PostDeliverMessage<?,?>) {
            serializedDeliverMessage =
                    new PostDeliverMessage<ActorRef,ClazzBytes>(
                    deliverMessage.getReceiver(), clazzBytes,
                    ((PostDeliverMessage<ActorRef,Object>) deliverMessage).getReplyTo());
        }
        Preconditions.checkNotNull(serializedDeliverMessage).setMessageContext(deliverMessage.getMessageContext());
        return serializedDeliverMessage;
    }

    private AbstractDeliverMessage<ActorRef,Object> newDeliverMessage(
            AbstractDeliverMessage<ActorRef,ClazzBytes> serializedDeliverMessage){
        AbstractDeliverMessage<ActorRef,Object> deliverMessage = null;

        ClazzBytes clazzBytes = serializedDeliverMessage.getMessage();
        Object object = deserialize(clazzBytes.getData(),clazzBytes.getClazz());

        if(serializedDeliverMessage instanceof RequestDeliverMessage<?,?>){
            deliverMessage = new RequestDeliverMessage<ActorRef,Object>(
                    serializedDeliverMessage.getReceiver(),
                    object,
                    ((RequestDeliverMessage) serializedDeliverMessage).getTimeout());
        }
        else if(serializedDeliverMessage instanceof PostDeliverMessage<?,?>) {
            deliverMessage = new PostDeliverMessage<ActorRef,Object>(
                    serializedDeliverMessage.getReceiver(),
                    object,
                    ((PostDeliverMessage<ActorRef,ClazzBytes>) serializedDeliverMessage).getReplyTo());
        }
        Preconditions.checkNotNull(deliverMessage).setMessageContext(serializedDeliverMessage.getMessageContext());
        return deliverMessage;
    }

    @SuppressWarnings("unchecked")
    private void handleDeserializeMessage(ActorRef sender,
                                          DeserializeMessage<ActorRef> message) {
        LOG.debug("handleSerializeMessage {} ", message);

        AbstractDeliverMessage<ActorRef,Object> abstractDeliverMessage =
                newDeliverMessage((AbstractDeliverMessage<ActorRef,ClazzBytes>)
                        deserialize(message.getBytes(),AbstractDeliverMessage.class));
        sender.tell(new DeserializeMessageReply(message.getRemoteProxy(),abstractDeliverMessage),getSelf());
    }


    private byte[] serialize(Object message){
        // Get the Serialization Extension
        Serialization serialization = SerializationExtension.get(getContext().system());
        Serializer serializer = serialization.findSerializerFor(message);
        // Turn it into bytes
        byte[] bytes = serializer.toBinary(message);
        return bytes;
    }


    private Object deserialize(byte[] bytes,Class<?> clazz){
        // Get the Serialization Extension

        Serialization serialization = SerializationExtension.get(getContext().system());
        Serializer serializer = serialization.serializerFor(clazz);


        // Turn it into bytes
        Object message = serializer.fromBinary(bytes);
        return message;
    }

    public static Props props() {
        return Props.create(SubChannelSerializeActor.class);
    }

    public static Props props(ProxyLink<ActorRef> proxyLink,Duration receiveTimeout)  {
        return Props.create(new SliceMessageSerializerCreator(proxyLink, receiveTimeout));
    }

    private static class SliceMessageSerializerCreator implements Creator<SubChannelSerializeActor> {

        private static final long serialVersionUID = 1L;

        final Duration receiveTimeout;
        private ProxyLink<ActorRef> proxyLink;

        SliceMessageSerializerCreator(ProxyLink<ActorRef> proxyLink,Duration receiveTimeout) {
            this.receiveTimeout = receiveTimeout;
            this.proxyLink = proxyLink;
        }

        @Override
        public SubChannelSerializeActor create() throws Exception {
            final SubChannelSerializeActor serializer;
            serializer = new SubChannelSerializeActor(proxyLink,receiveTimeout);
            serializer.getContext().setReceiveTimeout(receiveTimeout);
            return serializer;
        }
    }
}
