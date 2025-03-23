/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.cluster.raft.behaviors;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.common.util.concurrent.Uninterruptibles;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Props;
import org.apache.pekko.actor.Status;
import org.apache.pekko.dispatch.ControlMessage;
import org.apache.pekko.dispatch.Dispatchers;
import org.apache.pekko.dispatch.Mailboxes;
import org.apache.pekko.pattern.Patterns;
import org.apache.pekko.testkit.TestActorRef;
import org.apache.pekko.testkit.javadsl.TestKit;
import org.apache.pekko.util.Timeout;
import org.junit.After;
import org.junit.Before;
import org.opendaylight.controller.cluster.raft.DefaultConfigParamsImpl;
import org.opendaylight.controller.cluster.raft.MockRaftActorContext;
import org.opendaylight.controller.cluster.raft.TestActorFactory;
import org.opendaylight.controller.cluster.raft.behaviors.AbstractLeader.SendHeartBeat;
import org.opendaylight.controller.cluster.raft.messages.AppendEntriesReply;
import org.opendaylight.controller.cluster.raft.spi.TermInfo;
import org.opendaylight.controller.cluster.raft.utils.MessageCollectorActor;
import org.opendaylight.raft.api.RaftRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.Await;
import scala.concurrent.duration.FiniteDuration;

/**
 * Abstract base for a leader election scenario test.
 *
 * @author Thomas Pantelis
 */
public class AbstractLeaderElectionScenarioTest {
    static final int HEARTBEAT_INTERVAL = 50;

    static class MemberActor extends MessageCollectorActor {
        private volatile RaftActorBehavior behavior;
        Map<Class<?>, CountDownLatch> messagesReceivedLatches = new ConcurrentHashMap<>();
        Map<Class<?>, Boolean> dropMessagesToBehavior = new ConcurrentHashMap<>();
        CountDownLatch behaviorStateChangeLatch;

        public static Props props() {
            return Props.create(MemberActor.class)
                .withDispatcher(Dispatchers.DefaultDispatcherId())
                .withMailbox(Mailboxes.DefaultMailboxId());
        }

        @Override
        @Deprecated(since = "11.0.0", forRemoval = true)
        public final ActorRef getSender() {
            return super.getSender();
        }

        @Override
        public void onReceive(Object message) throws Exception {
            // Ignore scheduled SendHeartBeat messages.
            if (message instanceof SendHeartBeat) {
                return;
            }

            if (message instanceof SetBehavior(var newBehavior, var context)) {
                behavior = newBehavior;
                context.setCurrentBehavior(behavior);
                return;
            }

            if (message instanceof GetBehaviorState(var replyTo)) {
                replyTo.tell(behavior != null ? behavior.raftRole()
                    : new Status.Failure(new IllegalStateException("RaftActorBehavior is not set in MemberActor")),
                    self());
            }

            if (message instanceof SendImmediateHeartBeat) {
                message = SendHeartBeat.INSTANCE;
            }

            try {
                if (behavior != null && !dropMessagesToBehavior.containsKey(message.getClass())) {
                    final var nextBehavior = behavior.handleMessage(getSender(), message);
                    if (nextBehavior != null) {
                        final var oldBehavior = behavior;
                        behavior = nextBehavior;
                        if (behavior != oldBehavior && behaviorStateChangeLatch != null) {
                            behaviorStateChangeLatch.countDown();
                        }
                    }
                }
            } finally {
                super.onReceive(message);

                final var latch = messagesReceivedLatches.get(message.getClass());
                if (latch != null) {
                    latch.countDown();
                }
            }
        }

        @Override
        public void postStop() throws Exception {
            super.postStop();

            if (behavior != null) {
                behavior.close();
            }
        }

        void expectBehaviorStateChange() {
            behaviorStateChangeLatch = new CountDownLatch(1);
        }

        void waitForBehaviorStateChange() {
            assertTrue("Expected behavior state change",
                    Uninterruptibles.awaitUninterruptibly(behaviorStateChangeLatch, 5, TimeUnit.SECONDS));
        }

        void expectMessageClass(final Class<?> expClass, final int expCount) {
            messagesReceivedLatches.put(expClass, new CountDownLatch(expCount));
        }

        void waitForExpectedMessages(final Class<?> expClass) {
            final var latch = messagesReceivedLatches.get(expClass);
            assertNotNull("No messages received for " + expClass, latch);
            assertTrue("Missing messages of type " + expClass,
                    Uninterruptibles.awaitUninterruptibly(latch, 5, TimeUnit.SECONDS));
        }

        void dropMessagesToBehavior(final Class<?> msgClass) {
            dropMessagesToBehavior(msgClass, 1);
        }

        void dropMessagesToBehavior(final Class<?> msgClass, final int expCount) {
            expectMessageClass(msgClass, expCount);
            dropMessagesToBehavior.put(msgClass, Boolean.TRUE);
        }

        void clearDropMessagesToBehavior() {
            dropMessagesToBehavior.clear();
        }

        public void clear() {
            behaviorStateChangeLatch = null;
            clearDropMessagesToBehavior();
            messagesReceivedLatches.clear();
            clearMessages(self());
        }

        void forwardCapturedMessageToBehavior(final Class<?> msgClass, final ActorRef sender) {
            Object message = getFirstMatching(self(), msgClass);
            assertNotNull("Message of type " + msgClass + " not received", message);
            self().tell(message, sender);
        }

        void forwardCapturedMessagesToBehavior(final Class<?> msgClass, final ActorRef sender) {
            for (Object m: getAllMatching(self(), msgClass)) {
                self().tell(m, sender);
            }
        }

        <T> T getCapturedMessage(final Class<T> msgClass) {
            T message = getFirstMatching(self(), msgClass);
            assertNotNull("Message of type " + msgClass + " not received", message);
            return message;
        }
    }

    static final class SendImmediateHeartBeat implements ControlMessage {
        static final SendImmediateHeartBeat INSTANCE = new SendImmediateHeartBeat();

        private SendImmediateHeartBeat() {
        }
    }

    record GetBehaviorState(ActorRef replyTo) implements ControlMessage {
        GetBehaviorState {
            requireNonNull(replyTo);
        }
    }

    record SetBehavior(RaftActorBehavior behavior, MockRaftActorContext context) implements ControlMessage {
        SetBehavior {
            requireNonNull(context);
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
    public void tearDown() {
        TestKit.shutdownActorSystem(system);
    }

    DefaultConfigParamsImpl newConfigParams() {
        DefaultConfigParamsImpl configParams = new DefaultConfigParamsImpl();
        configParams.setHeartBeatInterval(Duration.ofMillis(HEARTBEAT_INTERVAL));
        configParams.setElectionTimeoutFactor(100000);
        configParams.setIsolatedLeaderCheckInterval(Duration.ofDays(1));
        return configParams;
    }

    MockRaftActorContext newRaftActorContext(final String id, final ActorRef actor,
            final Map<String, String> peerAddresses) {
        MockRaftActorContext context = new MockRaftActorContext(id, system, actor);
        context.setPeerAddresses(peerAddresses);
        try {
            context.persistTermInfo(new TermInfo(1, ""));
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        return context;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    void verifyBehaviorState(final String name, final MemberActor actor, final RaftRole expState) {
        RaftRole actualState;
        try {
            actualState = (RaftRole) Await.result(Patterns.askWithReplyTo(actor.self(), GetBehaviorState::new,
                Timeout.apply(5, TimeUnit.SECONDS)), FiniteDuration.create(5, TimeUnit.SECONDS));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertEquals(name + " behavior state", expState, actualState);
    }

    void initializeLeaderBehavior(final MemberActor actor, final MockRaftActorContext context,
            final int numActiveFollowers) {
        // Leader sends immediate heartbeats - we don't care about it so ignore it.
        // Sometimes the initial AppendEntries messages go to dead letters, probably b/c the follower actors
        // haven't been fully created/initialized by akka. So we try up to 3 times to create the Leader as
        // a workaround.

        Leader leader = null;
        AssertionError lastAssertError = null;
        for (int i = 1; i <= 3; i++) {
            actor.expectMessageClass(AppendEntriesReply.class, numActiveFollowers);

            leader = new Leader(context);
            try {
                actor.waitForExpectedMessages(AppendEntriesReply.class);
                lastAssertError = null;
                break;
            } catch (AssertionError e) {
                lastAssertError = e;
            }
        }

        if (lastAssertError != null) {
            throw lastAssertError;
        }

        context.setCurrentBehavior(leader);

        // Delay assignment of the leader behavior so the AppendEntriesReply isn't forwarded to the behavior.
        actor.self().tell(new SetBehavior(leader, context), ActorRef.noSender());

        actor.forwardCapturedMessagesToBehavior(AppendEntriesReply.class, ActorRef.noSender());
        actor.clear();

    }

    TestActorRef<MemberActor> newMemberActor(final String name) throws TimeoutException, InterruptedException {
        TestActorRef<MemberActor> actor = factory.createTestActor(MemberActor.props()
                .withDispatcher(Dispatchers.DefaultDispatcherId()), name);
        MessageCollectorActor.waitUntilReady(actor);
        return actor;
    }

    void sendHeartbeat(final TestActorRef<MemberActor> leaderActor) {
        Uninterruptibles.sleepUninterruptibly(HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);
        leaderActor.tell(SendImmediateHeartBeat.INSTANCE, ActorRef.noSender());
    }
}
