/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.subchannel.channels;

import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.TestActorRef;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Uninterruptibles;
import com.typesafe.config.Config;
import org.opendaylight.controller.subchannel.actors.TestSubChannelProxyActor;
import org.opendaylight.controller.subchannel.impl.akkabased.channels.AbstractAkkaBasedSubChannel;
import org.opendaylight.controller.subchannel.impl.akkabased.proxy.SubChannelProxyIdentifier;
import org.opendaylight.controller.subchannel.generic.spi.subchannel.AbstractSubChannelBuilder;
import org.opendaylight.controller.subchannel.generic.spi.subchannel.SubChannelProxyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by HanJie on 2017/2/13.
 *
 * @author Han Jie
 */
public class TestAkkaBasedForNonActorSubChannel extends AbstractAkkaBasedSubChannel {
    protected static final Logger LOG = LoggerFactory.getLogger(TestAkkaBasedForActorSubChannel.class);
    public TestAkkaBasedForNonActorSubChannel(ActorSystem actorSystem, String proxyNameSuffix){
        super(newSubChannelProxyFactory(actorSystem,proxyNameSuffix),
                newSubChannelBuilder(actorSystem,proxyNameSuffix,Optional.absent()));
    }

    private static String getProxyName(){
        return SubChannelProxyIdentifier.builder().type("default").build().toString();
    }
    @SuppressWarnings("unchecked")
    private static TestSubChannelProxyActor.Builder newSubChannelBuilder(ActorSystem actorSystem,
                                                                         String proxyNameSuffix,
                                                                         Optional<Config> config){
        Preconditions.checkArgument(actorSystem!=null,"actorContext should not be null!");
        Preconditions.checkArgument(proxyNameSuffix!=null,"proxyNameSuffix should not be null!");
        return (TestSubChannelProxyActor.Builder)TestSubChannelProxyActor.newBuilder()
                .setProxyName(getProxyName())
                .setParentName(Optional.absent())
                .setConfig(config);
    }


    private static SubChannelProxyFactory<ActorRef> newSubChannelProxyFactory(ActorSystem actorSystem, String proxyNameSuffix){
        return new TestAkkaBasedForNonActorSubChannelProxyFactoryImpl(actorSystem,proxyNameSuffix);
    }



    private static class TestAkkaBasedForNonActorSubChannelProxyFactoryImpl implements SubChannelProxyFactory<ActorRef> {
        private ActorSystem actorSystem;
        private String proxyNameSuffix;
        private TestAkkaBasedForNonActorSubChannelProxyFactoryImpl(ActorSystem actorSystem, String proxyNameSuffix) {
            this.actorSystem = actorSystem;
            this.proxyNameSuffix = proxyNameSuffix;
        }

        @Override
        public ActorRef createSubChannelProxy(AbstractSubChannelBuilder<?> builder) {
            Preconditions.checkArgument(builder!=null,"creator should not be null!");
            Preconditions.checkArgument(builder instanceof TestSubChannelProxyActor.Builder,
                    "creator should be instanceof Builder!");
            Exception lastException = null;

            TestSubChannelProxyActor.Builder builder1 =
                    (TestSubChannelProxyActor.Builder) builder;
            for(int i=0;i<100;i++) {
                try {
                    ActorRef proxy = TestActorRef.create(actorSystem,
                            builder1.props(),builder1.getProxyName());
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

    @SuppressWarnings("unchecked")
    public void setTestTimeOut(boolean testTimeOut){
        ((TestActorRef<TestSubChannelProxyActor>) getSubChannelProxy()).underlyingActor().setTimeOut(testTimeOut);
    }
}
