/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subchannel.generic.spi.proxy;

import java.util.ArrayDeque;
import java.util.Queue;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.subchannel.generic.api.messages.SerializeMessageReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by HanJie on 2017/2/16.
 *
 * @author Han Jie
 */
public class ProxyContext<T> extends ProxyLink<T>{
    private final Logger LOG = LoggerFactory.getLogger(getClass());
    private T seirializer;
    private final Queue<SerializeMessageReply<T>>
            pendingMessages = new ArrayDeque<>();

    public ProxyContext(T localProxy,T remoteProxy,T seirializer){
        super(localProxy,remoteProxy);
        this.seirializer = seirializer;
    }


    public T getSeirializer() {
        return seirializer;
    }


    public SerializeMessageReply<T> fetchQueuedMessage(){
        LOG.debug("fetchQueuedMessage");

        return pendingMessages.poll();
    }

    public void enqueueMessage(SerializeMessageReply<T> message){
        LOG.debug("enqueueMessage {}",message);
        pendingMessages.add(message);
    }


    static public class ProxyContextBuilder<T>{
        private T localProxy;
        private T remoteProxy;
        private T seirializer;

        public ProxyContextBuilder<T> setLocalProxy(T localProxy) {
            this.localProxy = localProxy;
            return this;
        }

        public ProxyContextBuilder<T> setRemoteProxy(T remoteProxy) {
            this.remoteProxy = remoteProxy;
            return this;
        }

        public ProxyContextBuilder<T> setSeirializer(T seirializer) {
            this.seirializer = seirializer;
            return this;
        }

        protected void verify() {
            Preconditions.checkNotNull(localProxy);
            Preconditions.checkNotNull(remoteProxy);
            Preconditions.checkNotNull(seirializer);
        }


        public ProxyContext<T> build() {
            verify();
            return new ProxyContext<T>(localProxy,remoteProxy,seirializer);
        }
    }
}
