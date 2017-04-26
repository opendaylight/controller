/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.messaging;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.UntypedActor;
import java.io.Serializable;
import javax.annotation.Nullable;
import org.opendaylight.controller.cluster.messaging.messages.RequestMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by HanJie on 2017/4/18.
 *
 * @author Han Jie
 */
public class SubChannelProxyActor extends UntypedActor {
    protected static final Logger LOG = LoggerFactory.getLogger(SubChannelProxyActor.class);
    private ActorRef sender;
    private SubChannel subChannel;

    protected SubChannelProxyActor(Builder builder) {
        LOG.debug("Actor created {}", getSelf());

    }

    @SuppressWarnings("unchecked")
    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof RequestMessage<?,?>) {
            hanldeRequestMessage((RequestMessage<ActorSelection,Object>)message);
        } else {
            if (subChannel != null) {
                subChannel.handleMessage(getSender(),  message);
            }
        }
    }

    private void hanldeRequestMessage(RequestMessage<ActorSelection,Object> message) {
        this.sender = getSender();
        LOG.debug("hanldeRequestMessage message {} sender {} ", message, sender);

        /**
         * Allocate a subChannel for each request in accord with ask.
         */
        if (this.subChannel == null) {
            this.subChannel = new SubChannel(getContext(), new SliceMessageCallback<Object>() {
                @Override
                public void onReceive(@Nullable Object message) {
                    sender.tell(message, getSelf());
                    getSelf().tell(PoisonPill.getInstance(),getSelf());
                }
            });

            subChannel.request(message.getReceiver(), message.getMessage());
        } else {
            LOG.error("Allocate a subchannel for each request in accord with ask, message {} sender {} ",
                    message, sender);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public abstract static class AbstractBuilder<S extends SubChannelProxyActor,T extends AbstractBuilder<S,T>>
            implements Serializable {
        private static final long serialVersionUID = -5810014743433599100L;
        private final Class<S> proxyClass;

        protected T self() {
            return (T) this;
        }

        protected AbstractBuilder(final Class<S> proxyClass) {
            this.proxyClass = proxyClass;
        }

        public Props props() {
            return Props.create(proxyClass, this);
        }

    }

    public static class Builder extends AbstractBuilder<SubChannelProxyActor,Builder> {
        private static final long serialVersionUID = -8815480704770933104L;

        protected Builder() {
            super(SubChannelProxyActor.class);
        }
    }
}

