/*
 * Copyright (c) 2017 ZTE.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.subchannel;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import com.google.common.base.Optional;
import com.typesafe.config.ConfigFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.subchannel.actors.TestAbstractActor;
import org.opendaylight.controller.subchannel.actors.TestClientActor;
import org.opendaylight.controller.subchannel.actors.TestSubChannelProxyActor;
import org.opendaylight.controller.subchannel.generic.config.DefaultSubChannelConfigParamsImpl;
import org.opendaylight.controller.subchannel.generic.config.SubChannelConfigParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.duration.FiniteDuration;


public class SubChannelConfigTest extends TestAbstractActor {
    private static final Logger TEST_LOG = LoggerFactory.getLogger(SubChannelConfigTest.class);
    private final Collection<ActorSystem> actorSystems = new ArrayList<>();

    @Before
    public void setUp() {

    }

    @After
    public void tearDown() {

        for(ActorSystem system: actorSystems) {
            JavaTestKit.shutdownActorSystem(system, null, Boolean.TRUE);
        }
    }

    private ActorSystem newActorSystem(String config) {
        ActorSystem system = ActorSystem.create("cluster-test", ConfigFactory.load().getConfig(config));
        actorSystems.add(system);
        return system;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSubChannelConfig() throws Exception{
        TEST_LOG.info("testSubChannelConfig starting");
        String senderId = "sender";

        final ActorSystem system1 = newActorSystem("Member1");


        new JavaTestKit(system1) {
            {
                TestActorRef<TestClientActor> sender =
                        TestActorRef.create(system1,TestClientActor.builder()
                                .setConfig(Optional.of(ConfigFactory.load().getConfig("sal-akka-subchannel"))).props(),
                                senderId);

                String address = "akka.tcp://cluster-test@127.0.0.1:2558/user/sender/subchannel-proxy-default";
                FiniteDuration duration = new FiniteDuration(1000, TimeUnit.MILLISECONDS);
                ActorRef proxy = Await.result(
                        system1.actorSelection(address).resolveOne(duration), duration);

                assertEquals("Create complete", true, proxy!=null);
                SubChannelConfigParams configParams =
                        ((TestActorRef<TestSubChannelProxyActor>)proxy)
                                .underlyingActor()
                                .getSubChannelProxy()
                                .getConfigParams();
                assertEquals("error param", true, configParams.getChunkSize() == 1024000);
                assertEquals("error param", true, configParams.getSerializerTimeoutInSeconds() == 30);
                assertEquals("error param", true, configParams.getMessageTimeoutInSeconds() == 30);

            }
        };
        TEST_LOG.info("testSubChannelConfig ending");
    }


    @Test
    @SuppressWarnings("unchecked")
    public void testSubChannelConfigMixDefault() throws Exception{
        TEST_LOG.info("testSubChannelConfigMixDefault starting");
        String senderId = "sender";

        final ActorSystem system1 = newActorSystem("Member1");


        new JavaTestKit(system1) {
            {
                TestActorRef<TestClientActor> sender =
                        TestActorRef.create(system1,TestClientActor.builder()
                             .setConfig(Optional.of(ConfigFactory.load()
                                     .getConfig("sal-akka-subchannel-test-mix")))
                                        .props(),
                                senderId);

                String address = "akka.tcp://cluster-test@127.0.0.1:2558/user/sender/subchannel-proxy-default";
                FiniteDuration duration = new FiniteDuration(1000, TimeUnit.MILLISECONDS);
                ActorRef proxy = Await.result(
                        system1.actorSelection(address).resolveOne(duration), duration);

                assertEquals("Create complete", true, proxy!=null);
                SubChannelConfigParams configParams =
                        ((TestActorRef<TestSubChannelProxyActor>)proxy)
                                .underlyingActor()
                                .getSubChannelProxy()
                                .getConfigParams();
                assertEquals("error DEFAULT_CHUNK_SIZE", true,
                        configParams.getChunkSize() == DefaultSubChannelConfigParamsImpl.DEFAULT_CHUNK_SIZE);
                assertEquals("error SerializerTimeout", true, configParams.getSerializerTimeoutInSeconds() == 30);
                assertEquals("error MessageTimeout", true, configParams.getMessageTimeoutInSeconds() == 30);

            }
        };
        TEST_LOG.info("testSubChannelConfigMixDefault ending");
    }
}
