/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subchannel.generic.spi.proxy;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import org.opendaylight.controller.subchannel.generic.api.data.MessageContext;
import org.opendaylight.controller.subchannel.generic.api.exception.PostTimeoutException;
import org.opendaylight.controller.subchannel.generic.api.exception.RequestTimeoutException;
import org.opendaylight.controller.subchannel.generic.api.exception.ResolveProxyException;
import org.opendaylight.controller.subchannel.generic.api.messages.AbstractDeliverMessage;
import org.opendaylight.controller.subchannel.generic.api.messages.DeserializeMessage;
import org.opendaylight.controller.subchannel.generic.api.messages.DeserializeMessageReply;
import org.opendaylight.controller.subchannel.generic.api.messages.PostDeliverMessage;
import org.opendaylight.controller.subchannel.generic.api.messages.RequestDeliverMessage;
import org.opendaylight.controller.subchannel.generic.api.messages.SerializeMessage;
import org.opendaylight.controller.subchannel.generic.api.messages.SerializeMessageReply;
import org.opendaylight.controller.subchannel.generic.api.messages.SerializerReceiveTimeout;
import org.opendaylight.controller.subchannel.generic.api.procedure.ProcedureCallback;
import org.opendaylight.controller.subchannel.generic.api.procedure.SliceMessageCallback;
import org.opendaylight.controller.subchannel.generic.api.procedure.SliceMessageProcedure;
import org.opendaylight.controller.subchannel.generic.config.SubChannelConfigParams;
import org.opendaylight.controller.subchannel.generic.config.SubChannelConfigParser;
import org.opendaylight.controller.subchannel.generic.jmx.mbeans.subchannel.SubChannelMBeanFactory;
import org.opendaylight.controller.subchannel.generic.jmx.mbeans.subchannel.SubChannelStats;
import org.opendaylight.controller.subchannel.generic.spi.subchannel.AbstractSubChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

/**
 * Created by HanJie on 2017/1/24.
 *
 * @author Han Jie
 */


public abstract class AbstractSubChannelProxy<T,C> implements Serializable,ProcedureCallback<T,C>{
    private final Logger LOG = LoggerFactory.getLogger(getClass());
    private SliceMessageProcedure<T,C> sliceMessageProcedure;
    private T localProxy;
    private T deserializer;
    private Map<T,ProxyContext<T>> clientProxyContexts = new HashMap<>();
    private Map<T,ProxyContext<T>> proxyContexts = new HashMap<>();
    private Map<T,ProxyContext<T>> tempRequestProxyContexts = new HashMap<>();
    private SubChannelConfigParams configParams;
    private Duration serializerTimeout;
    private SubChannelStats<T,C> subChannelMXBean;

    @SuppressWarnings("unchecked")
    protected AbstractSubChannelProxy(T localProxy, AbstractSubChannelBuilder<?> builder) {
        this.localProxy = localProxy;
        this.configParams = SubChannelConfigParser.parse(builder.getConfig());
        this.serializerTimeout = Duration.create(configParams.getSerializerTimeoutInSeconds(), TimeUnit.SECONDS);
        this.sliceMessageProcedure = new SliceMessageProcedure<>(this,this.configParams);
        /*
         * Only the NonActor's proxy registers MXBeans once.
         */
        if(builder.getParentName() == null) {
            this.subChannelMXBean = SubChannelMBeanFactory.getSubChannelMBean(builder.getProxyName(), null);
            this.subChannelMXBean.setSubChannelProxy(this);
        }
    }

    protected abstract void send(T receiver, Object message, T sender);
    protected abstract T resolveRemoteProxy(T receiver) throws ResolveProxyException;
    protected abstract C newTimer(FiniteDuration timeout, Object message);
    protected abstract void stopTimer(C timer);
    protected abstract T newSerializer();
    protected abstract T newSerializer(ProxyLink<T> proxyLink,Duration timeout);
    protected abstract void destroySerializer(T serializer);

    public Set<T> getReceivers() {
        return clientProxyContexts.keySet();
    }

    public Collection<ProxyContext<T>> getProxyContexts() {
        return proxyContexts.values();
    }

    public Set<T> getTempReceivers() {
        return tempRequestProxyContexts.keySet();
    }

    @VisibleForTesting
    public SubChannelConfigParams getConfigParams() {
        return configParams;
    }


    @Override
    public void sendCall(T receiver, Object message) {
        send(receiver, message, getSelf());
    }

    @SuppressWarnings("unchecked")
    @Override
    public void receiveCall(T sender, T receiver, byte[] bytes) {
        LOG.debug("receiveCall bytes");
        /*
         * Here input remote proxy as sender to finally create proxy context after receiving the serialize reply.
         * @see handleDeserializeMessageReply
         */
        send(deserializer, new DeserializeMessage(sender,bytes),getSelf());
    }

    @Override
    public C newTimerCall(FiniteDuration timeout, Object message) {
        return newTimer(timeout,message);
    }

    @Override
    public void stopTimerCall(C timer) {
        stopTimer(timer);
    }


    public void start(){
        this.deserializer = newSerializer();
    }
    @SuppressWarnings("unchecked")
    public boolean handleMessage(T sender, Object message){
        if (message instanceof AbstractDeliverMessage<?,?>) {
            handleDeliverMessage(sender, (AbstractDeliverMessage<T,Object>) message);
            return true;
        }else if (message instanceof SerializeMessageReply<?>) {
            handleSerializeMessageReply(sender, (SerializeMessageReply<T>) message);
            return true;
        }else if (message instanceof DeserializeMessageReply<?>) {
            handleDeserializeMessageReply(sender, (DeserializeMessageReply<T>) message);
            return true;
        }else if (message instanceof SerializerReceiveTimeout<?>) {
            handleSerializerReceiveTimeout(sender, (SerializerReceiveTimeout<T>) message);
            return true;
        }
        else {
            return sliceMessageProcedure.handleMessage(sender,getSelf(), message);
        }

    }

    private ProxyContext<T> cacheClientProxyContext(T remoteClient, ProxyContext<T> proxyContext){
        LOG.debug("cacheClientProxyContext remoteClient {} proxyContext {}",remoteClient,proxyContext);
        return clientProxyContexts.put(remoteClient, proxyContext);
    }

    private ProxyContext<T> cacheProxyContext(ProxyContext<T> proxyContext){
        LOG.debug("cacheProxyContext proxyContext {}",proxyContext);
        return proxyContexts.put(proxyContext.getRemoteProxy(), proxyContext);
    }

    private ProxyContext<T> getCachedProxyContext(T remoteProxy){
        return proxyContexts.get(remoteProxy);
    }

    private ProxyContext<T> getCachedClientProxyContext(T remoteClient){
        return clientProxyContexts.get(remoteClient);
    }

    private ProxyContext<T> removeCachedProxyContext(T remoteProxy){
        LOG.debug("removeCachedProxyContext remoteProxy {}",remoteProxy);
        return proxyContexts.remove(remoteProxy);
    }

    private ProxyContext<T> removeCachedClientProxyContext(T remoteClient){
        LOG.debug("removeCachedClientProxyContext remoteClient {}",remoteClient);
        return clientProxyContexts.remove(remoteClient);
    }

    private ProxyContext<T> findTempRequestProxyContext(T remoteClient){
        LOG.debug("findTempRequestProxyContext remoteClient {}",remoteClient);
        return tempRequestProxyContexts.get(remoteClient);
    }

    private ProxyContext<T> removeTempRequestProxyContext(T remoteClient){
        LOG.debug("removeTempRequestProxyContext remoteClient {}",remoteClient);
        ProxyContext<T> proxyContext = tempRequestProxyContexts.remove(remoteClient);
        return proxyContext;
    }

    private ProxyContext<T> cacheTempRequestProxyContext(T remoteClient, ProxyContext<T> proxyContext){
        LOG.debug("cacheTempRequestProxyContext remoteClient {}",remoteClient);
        return tempRequestProxyContexts.put(remoteClient,proxyContext);
    }

    private ProxyContext<T> createProxyContext(T remoteProxy){
        ProxyContext<T> proxyContext;
        proxyContext = new ProxyContext.ProxyContextBuilder<T>()
                .setLocalProxy(getSelf())
                .setRemoteProxy(remoteProxy)
                .setSeirializer(newSerializer(new ProxyLink<T>(getSelf(),remoteProxy),serializerTimeout))
                .build();
        cacheProxyContext(proxyContext);
        return proxyContext;
    }

    private ProxyContext<T> createContexts(T client){
        T remoteProxy = resolveRemoteProxy(client);
        ProxyContext<T> proxyContext = createProxyContext(remoteProxy);
        cacheClientProxyContext(client, proxyContext);
        return proxyContext;
    }

    private void handleDeliverMessage(T sender, AbstractDeliverMessage<T,Object> abstractDeliverMessage) {
        try {
            /**
             *  If it is the reply of request,then there MUST be cached client with proxy.
             */
            ProxyContext<T> proxyContext;
            proxyContext = findTempRequestProxyContext(abstractDeliverMessage.getReceiver());
            if (proxyContext == null) {
                /**
                 *  Otherwise it is normal post, and then resolve the remote proxy.
                 */
                proxyContext = getCachedClientProxyContext(abstractDeliverMessage.getReceiver());
                if (proxyContext == null) {
                    //on sender side ,otherwise proxyContext exists.
                    proxyContext = createContexts(abstractDeliverMessage.getReceiver());
                }
            }

            MessageContext<T> messageContext = new MessageContext<T>(abstractDeliverMessage.getReceiver(),
                    proxyContext.getRemoteProxy(),
                    sender, proxyContext.getLocalProxy(),
                    ((abstractDeliverMessage instanceof RequestDeliverMessage)?
                            RequestDeliverMessage.class:PostDeliverMessage.class));
            abstractDeliverMessage.setMessageContext(messageContext);

            executeAsyncSerialize(proxyContext, abstractDeliverMessage);

        }catch (Exception e){
            LOG.error("handleDeliverMessage error sender {} message {}",sender,abstractDeliverMessage);
            send(sender,e,getSelf());
        }
    }

    private void executeAsyncSerialize(ProxyContext<T> proxyContext,
                                       AbstractDeliverMessage<T,Object> message){
        LOG.debug("executeAsyncSerialize {} {}",proxyContext,message);
        send(proxyContext.getSeirializer(),new SerializeMessage<T>(message.getMessageContext(),message),getSelf());
    }



    private void handleSerializeMessageReply(T sender, SerializeMessageReply<T> reply) {
        LOG.debug("handleSerializeMessageReply {} {}",sender,reply);
        final MessageContext<T> messageContext = reply.getMessageContext();
        ProxyContext<T> proxyContext = findTempRequestProxyContext(messageContext.getReceiver());
        if(proxyContext!=null)
        {
            removeTempRequestProxyContext(messageContext.getReceiver());
            //Fixme:Under request mode, there would be limit proxyLinks,so do not release serializer.
        }else {
            proxyContext = getCachedClientProxyContext(messageContext.getReceiver());
            if(proxyContext == null){
                /*
                 *  Proxy receives some delivermessages before receiveTimeout message from serializer,
                 *  after receiveTimeout processed , the proxyContext would be released,
                 *  and then received serializeMessageReply from serializer,so it should createContexts again.
                 */
                proxyContext = createContexts(messageContext.getReceiver());
            }
        }

        if(sliceMessageProcedure.isBusy(proxyContext.getRemoteProxy())) {
            proxyContext.enqueueMessage(reply);
        }
        else {

            // Not using Futures.allAsList here to avoid its internal overhead.
            ProxyContext<T> finalProxyContext = proxyContext;
            SliceMessageCallback<Boolean> sliceMessageCallback = new SliceMessageCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    if (result == null || !result) {
                        LOG.error("Error doStartSliceMessage");
                    } else {
                        LOG.debug("doStartSliceMessage success");
                        SerializeMessageReply<T> message1 = finalProxyContext.fetchQueuedMessage();
                        if(message1!=null) {
                            doStartSliceMessage(finalProxyContext, message1, this);
                        }
                    }
                }

                @Override
                public void onFailure(Throwable failure) {
                    LOG.error("Error doStartSliceMessage",failure);
                    if(failure instanceof PostTimeoutException &&
                            messageContext.getClazz() == RequestDeliverMessage.class){
                            send(messageContext.getSender(), new RequestTimeoutException("RequestTimeoutException"), getSelf());
                    }
                    else {
                        send(messageContext.getSender(), failure, getSelf());
                    }
                    SerializeMessageReply<T> message1 = finalProxyContext.fetchQueuedMessage();
                    if(message1!=null) {
                        doStartSliceMessage(finalProxyContext, message1, this);
                    }
                }
            };

            doStartSliceMessage(proxyContext,reply,sliceMessageCallback);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleDeserializeMessageReply(T sender, DeserializeMessageReply<T> reply) {
        LOG.debug("handleDeserializeMessageReply sender {} reply {}",sender,reply);
        Object message = reply.getMessage();
        Preconditions.checkState(message instanceof AbstractDeliverMessage<?,?>);
        AbstractDeliverMessage<T,Object> deliverMessage = (AbstractDeliverMessage<T,Object>)message;

        ProxyContext<T> proxyContext = getCachedProxyContext(reply.getRemoteProxy());
        if(proxyContext == null) {
            proxyContext = createProxyContext(reply.getRemoteProxy());
        }

        if(deliverMessage instanceof RequestDeliverMessage<?,?>)
        {
            cacheTempRequestProxyContext(
                    deliverMessage.getMessageContext().getSender(),
                    proxyContext);
        }
        else if(deliverMessage instanceof PostDeliverMessage<?,?>){
            cacheClientProxyContext(((PostDeliverMessage<T,T>)deliverMessage).getReplyTo(),proxyContext);
        }

        finishDeliverMessager(deliverMessage);
    }

    private void doStartSliceMessage(ProxyContext<T> proxyContext,
                                                  SerializeMessageReply<T> message,
                                                  SliceMessageCallback<Boolean> sliceMessageCallback){
        sliceMessageProcedure.startSliceMessage(
                proxyContext.getLocalProxy(),
                proxyContext.getRemoteProxy(),
                message.getBytes(),sliceMessageCallback);
    }




    public T getSelf(){
        return localProxy;
    }

    private void finishDeliverMessager(AbstractDeliverMessage<T,Object> message) {
        if(message instanceof RequestDeliverMessage<?,?>){
            send(message.getReceiver(), message.getMessage(),message.getMessageContext().getSender());
        }
        else if(message instanceof PostDeliverMessage<?,?>){
            send(message.getReceiver(), message.getMessage(),
                    ((PostDeliverMessage<T,Object>) message).getReplyTo());
        }
    }

    private void handleSerializerReceiveTimeout(T sender, SerializerReceiveTimeout<T> message) {
        if(sliceMessageProcedure.isBusy(message.getProxyLink().getRemoteProxy()))
        {
            LOG.debug("RemoteProxy {} is busy now,wait for next timeout to release",
                    message.getProxyLink().getRemoteProxy());
            return;
        }

        for(Map.Entry<T,ProxyContext<T>> clientProxyContext : clientProxyContexts.entrySet()) {
            if(clientProxyContext.getValue().equals(message.getProxyLink())){
                removeCachedClientProxyContext(clientProxyContext.getKey());
                break;
            }
        }

        ProxyContext<T> proxyContext = removeCachedProxyContext(message.getProxyLink().getRemoteProxy());
        destroySerializer(proxyContext.getSeirializer());
    }
}
