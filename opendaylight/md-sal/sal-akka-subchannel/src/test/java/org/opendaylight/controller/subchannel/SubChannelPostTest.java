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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.AddressFromURIString;
import akka.actor.PoisonPill;
import akka.cluster.Cluster;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import com.google.common.util.concurrent.Uninterruptibles;
import com.typesafe.config.ConfigFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.subchannel.actors.TestAbstractActor;
import org.opendaylight.controller.subchannel.actors.TestClientActor;
import org.opendaylight.controller.subchannel.generic.config.DefaultSubChannelConfigParamsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.duration.FiniteDuration;


public class SubChannelPostTest extends TestAbstractActor {
    private static final Logger TEST_LOG = LoggerFactory.getLogger(SubChannelPostTest.class);
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
    public void testCreateSubChannel() throws Exception{
        TEST_LOG.info("testCreateSubChannel starting");
        String senderId = "sender";

        final ActorSystem system1 = newActorSystem("Member1");


        new JavaTestKit(system1) {
            {
                TestActorRef<TestClientActor> sender =
                        TestActorRef.create(system1,TestClientActor.builder()
                                .setCreateTestAkkaBasedSliceMessager(false).props(), senderId);

                String address = "akka.tcp://cluster-test@127.0.0.1:2558/user/sender/subchannel-proxy-default";
                FiniteDuration duration = new FiniteDuration(1000, TimeUnit.MILLISECONDS);
                ActorRef proxy = Await.result(
                        system1.actorSelection(address).resolveOne(duration), duration);

                assertEquals("Create complete", true, proxy!=null);
            }
        };
        TEST_LOG.info("testCreateSubChannel ending");
    }



    @Test
    public void testSubChannelPost() throws Exception{
        TEST_LOG.info("testSubChannelPost starting");
        String senderId = "sender";
        String receiverId = "receiver";

        final ActorSystem system1 = newActorSystem("Member1");
        Cluster.get(system1).join(AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558"));
        TestActorRef<TestClientActor> sender =
                TestActorRef.create(system1,TestClientActor.builder().props(), senderId);

        final ActorSystem system2 = newActorSystem("Member2");
        Cluster.get(system2).join(AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558"));
        TestActorRef<TestClientActor> receiver =
                TestActorRef.create(system2,TestClientActor.builder().props(), receiverId);


        new JavaTestKit(system1) {
            {

                String address = "akka.tcp://cluster-test@127.0.0.1:2559/user/receiver";
                FiniteDuration duration = new FiniteDuration(1000, TimeUnit.MILLISECONDS);
                ActorRef receiver1 = Await.result(
                        system1.actorSelection(address).resolveOne(duration), duration);

                String address2 = "akka.tcp://cluster-test@127.0.0.1:2559/user/receiver/subchannel-proxy-default";
                ActorRef receiver2 = Await.result(
                        system1.actorSelection(address2).resolveOne(duration), duration);

                String address3 = "akka.tcp://cluster-test@127.0.0.1:2558/user/sender/subchannel-proxy-default";
                ActorRef receiver3 = Await.result(
                        system2.actorSelection(address3).resolveOne(duration), duration);

                sender.underlyingActor().setTestMessageReplyReceived(new CountDownLatch(10));
                for(int i=0;i<10;i++) {
                    sender.underlyingActor().testPost(receiver1);
                }
                sender.underlyingActor().waitForTestMessageReply();
                Uninterruptibles.sleepUninterruptibly(DefaultSubChannelConfigParamsImpl.DEFAULT_SERIALIZER_IDLE_TIMEOUT+2,
                        TimeUnit.SECONDS);

            }
        };
        TEST_LOG.info("testSubChannelPost ending");
    }

    @Test
    public void testSubChannelPostRemoteNotExist() throws Exception{
        TEST_LOG.info("testSubChannelPostRemoteNotExist starting");

        String senderId = "sender";
        String receiverId = "receiver";

        final ActorSystem system1 = newActorSystem("Member1");
        Cluster.get(system1).join(AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558"));
        TestActorRef<TestClientActor> sender =
                TestActorRef.create(system1,TestClientActor.builder().props(), senderId);

        final ActorSystem system2 = newActorSystem("Member2");
        Cluster.get(system2).join(AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558"));
        TestActorRef<TestClientActor> receiver =
                TestActorRef.create(system2,TestClientActor.builder().props(), receiverId);

        new JavaTestKit(system1) {
            {

                String address = "akka.tcp://cluster-test@127.0.0.1:2559/user/receiver";
                FiniteDuration duration = new FiniteDuration(1000, TimeUnit.MILLISECONDS);
                ActorRef receiver1 = Await.result(
                        system1.actorSelection(address).resolveOne(duration), duration);


                receiver.tell(PoisonPill.getInstance(), ActorRef.noSender());

                sender.underlyingActor().testPost(receiver1);

                sender.underlyingActor().waitForResolveProxyException();

            }
        };
        TEST_LOG.info("testSubChannelPostRemoteNotExist ending");
    }




    @Test
    public void testSubChannelPostTimeOut() throws Exception{
        TEST_LOG.info("testSubChannelPostTimeOut starting");
        String senderId = "sender";
        String receiverId = "receiver";

        final ActorSystem system1 = newActorSystem("Member1");
        Cluster.get(system1).join(AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558"));
        TestActorRef<TestClientActor> sender =
                TestActorRef.create(system1,TestClientActor.builder().props(), senderId);

        final ActorSystem system2 = newActorSystem("Member2");
        Cluster.get(system2).join(AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558"));
        TestActorRef<TestClientActor> receiver =
                TestActorRef.create(system2,TestClientActor.builder().props(), receiverId);

        new JavaTestKit(system1) {
            {
                String address = "akka.tcp://cluster-test@127.0.0.1:2559/user/receiver";
                FiniteDuration duration = new FiniteDuration(1000, TimeUnit.MILLISECONDS);
                ActorRef receiver1 = Await.result(
                        system1.actorSelection(address).resolveOne(duration), duration);

                receiver.underlyingActor().setTestTimeOut(true);
                sender.underlyingActor().setTestTimeOut(true);

                sender.underlyingActor().setPostTimeoutExceptionReceived(new CountDownLatch(2));
                for(int i=0;i<2;i++) {
                    sender.underlyingActor().testPost(receiver1);
                }
                sender.underlyingActor().waitForPostTimeoutException(200);
                Uninterruptibles.sleepUninterruptibly(DefaultSubChannelConfigParamsImpl.DEFAULT_SERIALIZER_IDLE_TIMEOUT+2,
                        TimeUnit.SECONDS);
            }
        };
        TEST_LOG.info("testSubChannelPostTimeOut ending");
    }


}
