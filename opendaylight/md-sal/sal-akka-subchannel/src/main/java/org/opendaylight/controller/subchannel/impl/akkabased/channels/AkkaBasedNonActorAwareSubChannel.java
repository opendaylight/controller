/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subchannel.impl.akkabased.channels;

import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
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
public class AkkaBasedNonActorAwareSubChannel extends AbstractAkkaBasedSubChannel {
    protected static final Logger LOG = LoggerFactory.getLogger(AkkaBasedNonActorAwareSubChannel.class);

    public AkkaBasedNonActorAwareSubChannel(ActorSystem actorSystem, String proxyNameSuffix, Optional<Config> config) {
        super(newSubChannelProxyFactory(actorSystem),newSubChannelProxyBuilder(actorSystem,proxyNameSuffix,config));
    }

    public AkkaBasedNonActorAwareSubChannel(ActorSystem actorSystem, String proxyNameSuffix) {
        super(newSubChannelProxyFactory(actorSystem),
                newSubChannelProxyBuilder(actorSystem,proxyNameSuffix,Optional.absent()));
    }

    /**
     * SubChannel proxy name of user module(non Actor) :"subchannel-proxy-{proxySuffix}",
     * which could be used for configuration match of subchannel in akka remote(codename Artery) in case,
     * like " .../user/subchannel-proxy-{proxySuffix}".
     */
    protected static String getProxyName(String proxyNameSuffix){
        return SubChannelProxyIdentifier.builder().type(proxyNameSuffix).build().toString();
    }

    private static SubChannelProxyActor.Builder newSubChannelProxyBuilder(ActorSystem actorSystem,
                                                                     String proxyNameSuffix,
                                                                     Optional<Config> config){
        Preconditions.checkArgument(proxyNameSuffix!=null,"proxyNameSuffix should not be null!");
        Preconditions.checkArgument(actorSystem!=null,"actorSystem should not be null!");
        Preconditions.checkArgument(!proxyNameSuffix.equals("default"),"'default' is reserved!");

        return (SubChannelProxyActor.Builder)SubChannelProxyActor.builder()
                .setProxyName(getProxyName(proxyNameSuffix))
                .setConfig(config);
    }

    private static SubChannelProxyFactory<ActorRef> newSubChannelProxyFactory(ActorSystem actorSystem){
        return new AkkaBasedForNonActorSubChannelProxyFactoryImpl(actorSystem);
    }


    public static class AkkaBasedForNonActorSubChannelProxyFactoryImpl implements SubChannelProxyFactory<ActorRef> {
        private ActorSystem actorSystem;

        public AkkaBasedForNonActorSubChannelProxyFactoryImpl(ActorSystem actorSystem) {
            this.actorSystem = actorSystem;
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
                    ActorRef proxy = actorSystem.actorOf(builder1.props(),builder1.getProxyName());
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
