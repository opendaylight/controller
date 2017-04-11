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
import akka.dispatch.OnComplete;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import akka.util.Timeout;
import com.google.common.util.concurrent.Uninterruptibles;
import com.typesafe.config.ConfigFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.subchannel.actors.TestAbstractActor;
import org.opendaylight.controller.subchannel.actors.TestClientActor;
import org.opendaylight.controller.subchannel.api.akkabased.AkkaBasedSubChannelFactory;
import org.opendaylight.controller.subchannel.api.SubChannel;
import org.opendaylight.controller.subchannel.channels.TestAkkaBasedForNonActorSubChannel;
import org.opendaylight.controller.subchannel.generic.api.exception.RequestTimeoutException;
import org.opendaylight.controller.subchannel.generic.api.exception.ResolveProxyException;
import org.opendaylight.controller.subchannel.generic.config.DefaultSubChannelConfigParamsImpl;
import org.opendaylight.controller.subchannel.messages.RequestTestMessage;
import org.opendaylight.controller.subchannel.messages.TestMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

/**
 * Created by HanJie on 2017/2/20.
 *
 * @author Han Jie
 */
public class SubChannelRequestTest extends TestAbstractActor {
    private static final Logger TEST_LOG = LoggerFactory.getLogger(SubChannelRequestTest.class);
    private final Collection<ActorSystem> actorSystems = new ArrayList<>();

    @Before
    public void setUp() {

    }

    @After
    public void tearDown() {

        for (ActorSystem system : actorSystems) {
            JavaTestKit.shutdownActorSystem(system, null, Boolean.TRUE);
        }
    }

    private ActorSystem newActorSystem(String config) {
        ActorSystem system = ActorSystem.create("cluster-test", ConfigFactory.load().getConfig(config));
        actorSystems.add(system);
        return system;
    }

    @Test
    public void testCreateSubChannel() throws Exception {
        TEST_LOG.info("testCreateSubChannel starting");

        String senderId = "sender";
        final ActorSystem system1 = newActorSystem("Member1");



        new JavaTestKit(system1) {
            {
                final SubChannel<ActorRef> subChannel = AkkaBasedSubChannelFactory.createInstance(system1, "test");

                String address = "akka.tcp://cluster-test@127.0.0.1:2558/user/subchannel-proxy-test";

                final FiniteDuration duration = duration("60 seconds");
                ActorRef receiver1 = Await.result(
                        system1.actorSelection(address).resolveOne(duration), duration);

                assertEquals("Create complete", true, receiver1!=null);
            }
        };
        TEST_LOG.info("testCreateSubChannel ending");
    }


    @Test
    public void testSubChannelRequest() throws Exception {
        TEST_LOG.info("testSubChannelRequest starting");

        String senderId = "sender";
        String receiverId = "receiver";
        final ActorSystem system1 = newActorSystem("Member1");
        Cluster.get(system1).join(AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558"));
        final SubChannel<ActorRef> subChannel = new TestAkkaBasedForNonActorSubChannel(system1, "test");

        final ActorSystem system2 = newActorSystem("Member2");
        Cluster.get(system2).join(AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558"));
        TestActorRef<TestClientActor> receiver =
                TestActorRef.create(system2, TestClientActor.builder().props(), receiverId);
        new JavaTestKit(system1) {
            {
                String address = "akka.tcp://cluster-test@127.0.0.1:2559/user/receiver";

                final FiniteDuration duration = duration("60 seconds");
                final Timeout timeout = new Timeout(duration);

                ActorRef receiver1 = Await.result(
                        system1.actorSelection(address).resolveOne(duration), duration);
                final CountDownLatch latch = new CountDownLatch(10);

                for (int i = 0; i < 10; i++) {
                    int x = i;
                    RequestTestMessage message = new RequestTestMessage(i);
                    Future<Object> requestFuture =
                            subChannel.request(receiver1, message,timeout);


                    requestFuture.onComplete(new OnComplete<Object>() {
                        @Override
                        public void onComplete(final Throwable t, final Object resp) {
                            TEST_LOG.info("onComplete {} {}", t, resp);
                            if (resp instanceof TestMessage) {
                                assertEquals("wrong id", true, ((TestMessage)resp).getId() == x);
                                latch.countDown();
                            }
                        }
                    }, getSystem().dispatcher());

                }
                assertEquals("request complete", true, latch.await(30, TimeUnit.SECONDS));
                Uninterruptibles.sleepUninterruptibly(DefaultSubChannelConfigParamsImpl.DEFAULT_SERIALIZER_IDLE_TIMEOUT+2,
                        TimeUnit.SECONDS);
            }
        };
        TEST_LOG.info("testSubChannelRequest ending");
    }

    @Test
    public void testSubChannelRequestRemoteNotExist() throws Exception {
        TEST_LOG.info("testSubChannelRequestRemoteNotExist starting");

        String senderId = "sender1";
        String receiverId = "receiver1";

        final ActorSystem system1 = newActorSystem("Member1");
        Cluster.get(system1).join(AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558"));

        final ActorSystem system2 = newActorSystem("Member2");
        Cluster.get(system2).join(AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558"));
        TestActorRef<TestClientActor> receiver =
                TestActorRef.create(system2, TestClientActor.builder().props(), receiverId);

        new JavaTestKit(system1) {
            {

                String address = "akka.tcp://cluster-test@127.0.0.1:2559/user/receiver1";

                final FiniteDuration duration = duration("100 seconds");
                final Timeout timeout = new Timeout(duration);

                ActorRef receiver1 = Await.result(
                        system1.actorSelection(address).resolveOne(duration), duration);

                receiver.tell(PoisonPill.getInstance(), ActorRef.noSender());
                RequestTestMessage message = new RequestTestMessage();
                final SubChannel<ActorRef> subChannel = new TestAkkaBasedForNonActorSubChannel(system1, "test");
                Future<Object> requestFuture = subChannel.request(receiver1, message, timeout);
                final CountDownLatch latch = new CountDownLatch(1);

                try {
                    requestFuture.onComplete(new OnComplete<Object>() {
                        @Override
                        public void onComplete(final Throwable failure, final Object resp) {
                            TEST_LOG.info("onComplete {} {}", failure, resp);
                            if (failure instanceof ResolveProxyException) {
                                latch.countDown();
                            }
                        }
                    }, getSystem().dispatcher());
                } catch (Exception e) {
                    assertEquals("request complete", true, latch.await(10, TimeUnit.SECONDS));
                }
            }
        };
        TEST_LOG.info("testSubChannelRequestRemoteNotExist ending");
    }


    @Test
    public void testSubChannelRequestTimeOut() throws Exception {
        TEST_LOG.info("testSubChannelRequestTimeOut starting");
        String senderId = "sender2";
        String receiverId = "receiver2";

        final ActorSystem system1 = newActorSystem("Member1");
        Cluster.get(system1).join(AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558"));

        final ActorSystem system2 = newActorSystem("Member2");
        Cluster.get(system2).join(AddressFromURIString.parse("akka.tcp://cluster-test@127.0.0.1:2558"));
        TestActorRef<TestClientActor> receiver =
                TestActorRef.create(system2, TestClientActor.builder().props(), receiverId);

        new JavaTestKit(system1) {
            {
                String address = "akka.tcp://cluster-test@127.0.0.1:2559/user/receiver2";
                final FiniteDuration duration = duration("20 seconds");
                final Timeout timeout = new Timeout(duration);
                ActorRef receiver1 = Await.result(
                        system1.actorSelection(address).resolveOne(duration), duration);

                RequestTestMessage message = new RequestTestMessage();
                final TestAkkaBasedForNonActorSubChannel subChannel = new TestAkkaBasedForNonActorSubChannel(system1, "test");
                subChannel.setTestTimeOut(true);

                Future<Object> requestFuture = subChannel.request(receiver1, message, timeout);

                final CountDownLatch latch = new CountDownLatch(1);


                requestFuture.onComplete(new OnComplete<Object>() {
                    @Override
                    public void onComplete(final Throwable t, final Object resp) {
                        if (t instanceof RequestTimeoutException) {
                            latch.countDown();
                        }
                    }
                }, getSystem().dispatcher());


                assertEquals("request complete", true, latch.await(30, TimeUnit.SECONDS));
                Uninterruptibles.sleepUninterruptibly(DefaultSubChannelConfigParamsImpl.DEFAULT_SERIALIZER_IDLE_TIMEOUT+2,
                        TimeUnit.SECONDS);
            }
        };
        TEST_LOG.info("testSubChannelRequestTimeOut ending");
    }
}


