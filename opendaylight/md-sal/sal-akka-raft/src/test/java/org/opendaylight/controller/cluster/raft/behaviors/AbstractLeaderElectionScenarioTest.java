/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.behaviors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.Dispatchers;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;
import com.google.common.util.concurrent.Uninterruptibles;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.opendaylight.controller.cluster.raft.DefaultConfigParamsImpl;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext;
import org.opendaylight.controller.cluster.raft.RaftState;
import org.opendaylight.controller.cluster.raft.TestActorFactory;
import org.opendaylight.controller.cluster.raft.base.messages.SendHeartBeat;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.FiniteDuration;

/**
 * Abstract base for a leader election scenario test.
 *
 * @author Thomas Pantelis
 */
public class AbstractLeaderElectionScenarioTest {
    static final int HEARTBEAT_INTERVAL = 50;

    static class MemberActor extends MessageCollectorActor {

        volatile RaftActorBehavior behavior;
        Map<Class<?>, CountDownLatch> messagesReceivedLatches = new ConcurrentHashMap<>();
        Map<Class<?>, Boolean> dropMessagesToBehavior = new ConcurrentHashMap<>();
        CountDownLatch behaviorStateChangeLatch;

        public static Props props() {
            return Props.create(MemberActor.class).withDispatcher(Dispatchers.DefaultDispatcherId());
        }

        @Override
        public void onReceive(Object message) throws Exception {
            // Ignore scheduled SendHeartBeat messages.
            if(message instanceof SendHeartBeat) {
                return;
            }

            try {
                if(behavior != null && !dropMessagesToBehavior.containsKey(message.getClass())) {
                    RaftActorBehavior oldBehavior = behavior;
                    behavior = behavior.handleMessage(getSender(), message);
                    if(behavior != oldBehavior && behaviorStateChangeLatch != null) {
                        behaviorStateChangeLatch.countDown();
                    }
                }
            } finally {
                super.onReceive(message);

                CountDownLatch latch = messagesReceivedLatches.get(message.getClass());
                if(latch != null) {
                    latch.countDown();
                }
            }
        }

        void expectBehaviorStateChange() {
            behaviorStateChangeLatch = new CountDownLatch(1);
        }

        void waitForBehaviorStateChange() {
            assertTrue("Expected behavior state change",
                    Uninterruptibles.awaitUninterruptibly(behaviorStateChangeLatch, 5, TimeUnit.SECONDS));
        }

        void expectMessageClass(Class<?> expClass, int expCount) {
            messagesReceivedLatches.put(expClass, new CountDownLatch(expCount));
        }

        void waitForExpectedMessages(Class<?> expClass) {
            CountDownLatch latch = messagesReceivedLatches.get(expClass);
            assertNotNull("No messages received for " + expClass, latch);
            assertTrue("Missing messages of type " + expClass,
                    Uninterruptibles.awaitUninterruptibly(latch, 5, TimeUnit.SECONDS));
        }

        void dropMessagesToBehavior(Class<?> msgClass) {
            dropMessagesToBehavior(msgClass, 1);
        }

        void dropMessagesToBehavior(Class<?> msgClass, int expCount) {
            expectMessageClass(msgClass, expCount);
            dropMessagesToBehavior.put(msgClass, Boolean.TRUE);
        }

        void clearDropMessagesToBehavior() {
            dropMessagesToBehavior.clear();
        }

        @Override
        public void clear() {
            behaviorStateChangeLatch = null;
            clearDropMessagesToBehavior();
            messagesReceivedLatches.clear();
            super.clear();
        }

        void forwardCapturedMessageToBehavior(Class<?> msgClass, ActorRef sender) throws Exception {
            Object message = getFirstMatching(getSelf(), msgClass);
            assertNotNull("Message of type " + msgClass + " not received", message);
            getSelf().tell(message, sender);
        }

        void forwardCapturedMessagesToBehavior(Class<?> msgClass, ActorRef sender) throws Exception {
            for(Object m: getAllMatching(getSelf(), msgClass)) {
                getSelf().tell(m, sender);
            }
        }

        <T> T getCapturedMessage(Class<T> msgClass) throws Exception {
            T message = getFirstMatching(getSelf(), msgClass);
            assertNotNull("Message of type " + msgClass + " not received", message);
            return message;
        }
    }

    protected final Logger testLog = LoggerFactory.getLogger(MockRaftActorContext.class);
    protected final ActorSystem system = ActorSystem.create("test");
    protected final TestActorFactory factory = new TestActorFactory(system);
    protected TestActorRef<MemberActor> member1ActorRef;
    protected TestActorRef<MemberActor> member2ActorRef;
    protected TestActorRef<MemberActor> member3ActorRef;
    protected MemberActor member1Actor;
    protected MemberActor member2Actor;
    protected MemberActor member3Actor;
    protected MockRaftActorContext member1Context;
    protected MockRaftActorContext member2Context;
    protected MockRaftActorContext member3Context;

    @Before
    public void setup() throws Exception {
        member1ActorRef = newMemberActor("member1");
        member2ActorRef = newMemberActor("member2");
        member3ActorRef = newMemberActor("member3");

        member1Actor = member1ActorRef.underlyingActor();
        member2Actor = member2ActorRef.underlyingActor();
        member3Actor = member3ActorRef.underlyingActor();
    }

    @After
    public void tearDown() throws Exception {

        if (member1Actor.behavior != null) {
            member1Actor.behavior.close();
        }
        if (member2Actor.behavior != null) {
            member2Actor.behavior.close();
        }
        if (member3Actor.behavior != null) {
            member3Actor.behavior.close();
        }

        JavaTestKit.shutdownActorSystem(system);
    }

    DefaultConfigParamsImpl newConfigParams() {
        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(new FiniteDuration(HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS));
        configParams.setElectionTimeoutFactor(100000);
        configParams.setIsolatedLeaderCheckInterval(new FiniteDuration(1, TimeUnit.DAYS));
        return configParams;
    }

    MockRaftActorContext newRaftActorContext(String id, ActorRef actor,
            Map<String, String> peerAddresses) {
        MockRaftActorContext context = new MockRaftActorContext(id, system, actor);
        context.setPeerAddresses(peerAddresses);
        context.getTermInformation().updateAndPersist(1, "");
        return context;
    }

    void verifyBehaviorState(String name, MemberActor actor, RaftState expState) {
        assertEquals(name + " behavior state", expState, actor.behavior.state());
    }

    void initializeLeaderBehavior(MemberActor actor, MockRaftActorContext context, int numActiveFollowers) throws Exception {
        // Leader sends immediate heartbeats - we don't care about it so ignore it.

        actor.expectMessageClass(AppendEntriesReply.class, numActiveFollowers);

        Leader leader = new Leader(context);
        context.setCurrentBehavior(leader);

        actor.waitForExpectedMessages(AppendEntriesReply.class);
        // Delay assignment here so the AppendEntriesReply isn't forwarded to the behavior.
        actor.behavior = leader;

        actor.forwardCapturedMessagesToBehavior(AppendEntriesReply.class, ActorRef.noSender());
        actor.clear();

    }

    TestActorRef<MemberActor> newMemberActor(String name) throws Exception {
        TestActorRef<MemberActor> actor = factory.createTestActor(MemberActor.props().
                withDispatcher(Dispatchers.DefaultDispatcherId()), name);
        MessageCollectorActor.waitUntilReady(actor);
        return actor;
    }

    void sendHeartbeat(TestActorRef<MemberActor> leaderActor) {
        Uninterruptibles.sleepUninterruptibly(HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);
        leaderActor.underlyingActor().behavior.handleMessage(leaderActor, SendHeartBeat.INSTANCE);
    }
}
