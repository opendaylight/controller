/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subchannel.impl.akkabased.proxy;


import akka.actor.Props;
import akka.actor.UntypedActor;
import com.google.common.annotations.VisibleForTesting;
import org.opendaylight.controller.subchannel.generic.spi.subchannel.AbstractSubChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by HanJie on 2017/1/24.
 *
 * @author Han Jie
 */
public class SubChannelProxyActor extends UntypedActor {
    protected final Logger LOG = LoggerFactory.getLogger(getClass());
    private AkkaBasedSubChannelProxy subChannelProxy;
    private SubChannelProxyActor.AbstractBuilder<?> builder;
    protected SubChannelProxyActor(SubChannelProxyActor.AbstractBuilder<?> builder) {
        LOG.debug("Actor created {}", getSelf());
        this.builder = builder;
    }

    @Override
    public void preStart() {
        this.subChannelProxy = AkkaBasedSubChannelProxyFactory.createInstance(getContext(), builder);
        subChannelProxy.start();
    }

    @Override
    public void postRestart(Throwable reason){
        preStart();
    }

    @Override
    public final void onReceive(Object message) throws Exception {
        handleReceive(message);
    }

    /**
     * @param message Incoming message
     * @throws Exception
     */

    protected void handleReceive(Object message) throws Exception{

        if(!subChannelProxy.handleMessage(getSender(), message))
        {
            unhandled(message);
        }

    }

    @VisibleForTesting
    public AkkaBasedSubChannelProxy getSubChannelProxy() {
        return subChannelProxy;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static abstract class AbstractBuilder<S extends SubChannelProxyActor> extends
            AbstractSubChannelBuilder<AbstractBuilder<S>> {

        private final Class<S> proxyClass;

        protected AbstractBuilder(final Class<S> proxyClass) {
            super();
            this.proxyClass = proxyClass;
        }

        @Override
        protected void verify() {
            super.verify();
        }

        public Props props() {
            verify();
            return Props.create(proxyClass, this);
        }

    }

    public static class Builder extends AbstractBuilder<SubChannelProxyActor> {

        protected Builder() {
            super(SubChannelProxyActor.class);
        }
    }


}
