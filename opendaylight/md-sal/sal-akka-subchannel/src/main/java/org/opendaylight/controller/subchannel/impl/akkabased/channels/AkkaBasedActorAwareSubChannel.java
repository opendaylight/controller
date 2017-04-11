/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subchannel.impl.akkabased.channels;

import java.util.concurrent.TimeUnit;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Uninterruptibles;
import com.typesafe.config.Config;
import org.opendaylight.controller.subchannel.impl.akkabased.proxy.SubChannelProxyActor;
import org.opendaylight.controller.subchannel.impl.akkabased.proxy.SubChannelProxyIdentifier;
import org.opendaylight.controller.subchannel.generic.spi.subchannel.AbstractSubChannelBuilder;
import org.opendaylight.controller.subchannel.generic.spi.subchannel.SubChannelProxyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by HanJie on 2017/2/6.
 *
 * @author Han Jie
 */
public class AkkaBasedActorAwareSubChannel extends AbstractAkkaBasedSubChannel {
    protected static final Logger LOG = LoggerFactory.getLogger(AkkaBasedActorAwareSubChannel.class);

    public AkkaBasedActorAwareSubChannel(ActorContext actorContext, Optional<Config> config) {
        super(newSubChannelProxyFactory(actorContext),newSubChannelBuilder(actorContext,config));
    }

    public AkkaBasedActorAwareSubChannel(ActorContext actorContext) {
        super(newSubChannelProxyFactory(actorContext),newSubChannelBuilder(actorContext,Optional.absent()));
    }

    /**
     * Default subchannel proxy id of child of user actor:"subchannel-proxy-default",which could be
     * used for configuration match of subchannel in akka remote(codename Artery) in case,
     * like " .../user/user-actor-id/subchannel-proxy-default".
     */
    private static String getProxyName(){
        return SubChannelProxyIdentifier.builder().type("default").build().toString();
    }

    private static SubChannelProxyActor.Builder newSubChannelBuilder(ActorContext actorContext,
                                                                     Optional<Config> config){
        Preconditions.checkArgument(actorContext!=null,"actorContext should not be null!");
        return (SubChannelProxyActor.Builder)SubChannelProxyActor.builder()
                .setProxyName(getProxyName())
                .setParentName(Optional.of(actorContext.self().path().name()))
                .setConfig(config);
    }

    private static SubChannelProxyFactory<ActorRef> newSubChannelProxyFactory(ActorContext actorContext){
        return new AkkaBasedForActorSubChannelProxyFactoryImpl(actorContext);
    }


    private static class AkkaBasedForActorSubChannelProxyFactoryImpl implements SubChannelProxyFactory<ActorRef> {
        private ActorContext actorContext;

        public AkkaBasedForActorSubChannelProxyFactoryImpl(ActorContext actorContext) {
            this.actorContext = actorContext;
        }

        @Override
        public ActorRef createSubChannelProxy(AbstractSubChannelBuilder<?> builder) {
            Preconditions.checkArgument(builder!=null,"creator should not be null!");
            Preconditions.checkArgument(builder instanceof SubChannelProxyActor.Builder,
                    "creator should be instanceof Builder!");
            Exception lastException = null;

            SubChannelProxyActor.Builder builder1 =
                    (SubChannelProxyActor.Builder) builder;
            for(int i=0;i<100;i++) {
                try {
                    ActorRef proxy = actorContext
                            .actorOf(builder1.props(),
                                    builder1.getProxyName());
                    LOG.debug("Create SubChannel Proxy : {}", proxy.path().toString());
                    return proxy;
                }catch (Exception e){
                    lastException = e;
                    Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
                    LOG.debug("Could not create actor {} because of {} - waiting for sometime before retrying (retry count = {})",
                            builder1.getProxyName(), e.getMessage(), i);
                }
            }

            throw new IllegalStateException("Failed to create SubChannel Proxy", lastException);

        }
    }
}
